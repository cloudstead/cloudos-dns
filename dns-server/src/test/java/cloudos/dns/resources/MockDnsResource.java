package cloudos.dns.resources;

import cloudos.dns.DnsApiConstants;
import cloudos.dns.dao.SessionDAO;
import cloudos.dns.model.DnsAccount;
import cloudos.dns.model.support.DnsSessionRequest;
import cloudos.dns.model.support.DnsUserResponse;
import cloudos.dns.server.DnsServerConfiguration;
import cloudos.dns.service.mock.MockDnsManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(DnsApiConstants.MOCK_ENDPOINT)
@Service @Slf4j
public class MockDnsResource {

    @Autowired private DnsServerConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;

    private Map<String, DnsAccount> accounts = new ConcurrentHashMap<>();
    @Getter private MockDnsManager dnsManager = new MockDnsManager();

    @GET
    public Response hello () throws Exception {
        return Response.ok("hello\n").build();
    }

    @POST
    public Response startSession (DnsSessionRequest request) {

        final String zone = configuration.getDyndns().getZone();

        // strip zone suffix if present
        String user = request.getUser();
        if (user.endsWith(zone)) user = user.substring(0, user.length() - zone.length());

        DnsAccount account = accounts.get(user);
        if (account == null) {
            account = (DnsAccount) new DnsAccount().setPassword(new HashedPassword(request.getPassword())).setName(user);
            account.initUuid();
            accounts.put(user, account);
        }
        if (account.getPassword() != null && !account.getPassword().isCorrectPassword(request.getPassword())) return ResourceUtil.notFound();
        return Response.ok(sessionDAO.create(account)).build();
    }

    @POST
    @Path(DnsApiConstants.EP_USER + "/{name}")
    public Response createUser (@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                @PathParam("name") String name) {

        DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();
        if (!account.isAdmin()) return ResourceUtil.forbidden();

        name = name.toLowerCase();
        final String zone = configuration.getDyndns().getZone();
        if (name.endsWith(zone)) {
            name = name.substring(0, name.length()-zone.length()-1);
        }

        if (name.isEmpty() || name.length() > 63) return ResourceUtil.invalid("err.name.length");
        if (!name.matches("[A-Za-z0-9\\-]+") || name.startsWith("-")) return ResourceUtil.invalid("err.name.invalid");

        final DnsAccount existing = accounts.get(name);
        if (existing != null) {
            log.warn("Overwriting existing account: " + existing);
        }

        final String password = randomAlphanumeric(10);
        account = (DnsAccount) new DnsAccount().setPassword(new HashedPassword(password)).setName(name);
        account.initUuid();
        accounts.put(name, account);

        return Response.ok(new DnsUserResponse(name, password)).build();
    }

    @POST
    @Path(DnsApiConstants.EP_LIST)
    public Response list (@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                          DnsRecordMatch match) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();

        if (!account.isAdmin()) match.setSubdomain(account.getName()+"."+configuration.getDyndns().getZone());

        try {
            return Response.ok(getDnsManager().list(match)).build();
        } catch (Exception e) {
            log.error("error listing records: "+e, e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path(DnsApiConstants.EP_UPDATE)
    public Response writeRecord(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                DnsRecord record) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();

        // admins can modify anything. users can only modify records within their subdomain (their username)
        if (!account.isAdmin()) {
            if (!record.getFqdn().endsWith(account.getName() + "." + configuration.getDyndns().getZone())) {
                return ResourceUtil.forbidden();
            }
        }

        try {
            final DnsManager dnsManager = getDnsManager();
            final boolean updated;
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (dnsManager) {
                updated = dnsManager.write(record);
                if (updated) dnsManager.publish();
            }
            return Response.ok(updated).build();

        } catch (Exception e) {
            log.error("Error writing record: "+e, e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path(DnsApiConstants.EP_DELETE)
    public Response removeRecords(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                  DnsRecordMatch match) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();

        if (!account.isAdmin()) match.setSubdomain(account.getName()+"."+configuration.getDyndns().getZone());

        try {
            final DnsManager dnsManager = getDnsManager();
            int removed;
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (dnsManager) {
                removed = dnsManager.remove(match);
                dnsManager.publish();
            }
            return Response.ok(removed).build();

        } catch (Exception e) {
            log.error("Error removing records: "+e, e);
            return Response.serverError().build();
        }
    }
}
