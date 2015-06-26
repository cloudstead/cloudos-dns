package cloudos.dns.resources;

import cloudos.dns.DnsApiConstants;
import cloudos.dns.dao.DnsAccountDAO;
import cloudos.dns.dao.SessionDAO;
import cloudos.dns.model.DnsAccount;
import cloudos.dns.model.support.DnsSessionRequest;
import cloudos.dns.model.support.DnsUserResponse;
import cloudos.dns.server.DnsServerConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.DnsRecordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(DnsApiConstants.ENDPOINT)
@Service @Slf4j
public class DnsResource {

    @Autowired private DnsServerConfiguration configuration;
    @Autowired private DnsAccountDAO accountDAO;
    @Autowired private SessionDAO sessionDAO;

    @GET
    public Response hello () throws Exception { return ok("hello\n"); }

    /**
     * Start a new session (login)
     * @param request the session request (login credentials)
     * @return A new session token
     */
    @POST
    @ReturnType("java.lang.String")
    public Response startSession (DnsSessionRequest request) {

        final String zone = configuration.getDyndns().getZone();

        // strip zone suffix if present
        String user = request.getUser();
        if (user.endsWith(zone)) user = user.substring(0, user.length() - zone.length());

        final DnsAccount account = accountDAO.findByNameAndPassword(user, request.getPassword());
        if (account == null) return notFound();
        return ok(sessionDAO.create(account));
    }

    /**
     * Create or update a user. Must be an admin.
     * @param apiKey The session token
     * @param name Name of the user to create
     * @param password Password for the new user
     * @return The newly created DnsUser
     */
    @POST
    @Path(DnsApiConstants.EP_USER + "/{name}")
    @ReturnType("cloudos.dns.model.support.DnsUserResponse")
    public Response createOrUpdateUser(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                       @PathParam("name") String name,
                                       String password) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();
        if (!account.isAdmin()) return ResourceUtil.forbidden();

        name = name.toLowerCase();
        final String zone = configuration.getDyndns().getZone();
        if (name.endsWith(zone)) {
            name = name.substring(0, name.length()-zone.length()-1);
        }

        if (name.isEmpty() || name.length() > 63) return ResourceUtil.invalid("err.name.length");
        if (!name.matches("[A-Za-z0-9\\-]+") || name.startsWith("-")) return ResourceUtil.invalid("err.name.invalid");

        final DnsAccount existing = accountDAO.findByName(name);
        if (existing != null) {
            log.warn("Overwriting existing account: "+existing);
            accountDAO.delete(existing.getUuid());
        }

        if (empty(password)) password = randomAlphanumeric(10);
        accountDAO.create((DnsAccount) new DnsAccount().setPassword(new HashedPassword(password)).setName(name));

        return ok(new DnsUserResponse(name, password));
    }

    /**
     * List all records that match a DnsRecordMatch query
     * @param apiKey the session token
     * @param match the query
     * @return a List of DnsRecords that match. If none are found, an empty list is returned (not a 404)
     */
    @POST
    @Path(DnsApiConstants.EP_LIST)
    @ReturnType("java.util.List<org.cobbzilla.util.dns.DnsRecord>")
    public Response list (@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                          DnsRecordMatch match) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();

        if (!account.isAdmin()) match.setSubdomain(account.getName()+"."+configuration.getDyndns().getZone());

        try {
            return ok(configuration.getDnsManager().list(match));
        } catch (Exception e) {
            log.error("error listing records: "+e, e);
            return serverError();
        }
    }

    /**
     * Create or update a DNS record
     * @param apiKey the session token
     * @param record the record to create or update
     * @return true if the record was created or updated. false if it was unchanged.
     */
    @POST
    @Path(DnsApiConstants.EP_UPDATE)
    @ReturnType("java.lang.Boolean")
    public Response writeRecord(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                DnsRecord record) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();

        final List<ConstraintViolationBean> errors = DnsRecordValidator.validate(record);
        if (!empty(errors)) return invalid(errors);

        // admins can modify anything. users can only modify records within their subdomain (their username)
        if (!account.isAdmin()) {
            if (!record.getFqdn().endsWith(account.getName() + "." + configuration.getDyndns().getZone())) {
                return ResourceUtil.forbidden();
            }
        }

        try {
            final DnsManager dnsManager = configuration.getDnsManager();
            final boolean updated;
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (dnsManager) {
                updated = dnsManager.write(record);
                if (updated) dnsManager.publish();
            }
            return ok(updated);

        } catch (Exception e) {
            log.error("Error writing record: "+e, e);
            return serverError();
        }
    }

    /**
     * Delete DNS records that match a DnsRecordMatch query
     * @param apiKey the session token
     * @param match the DNS query
     * @return an integer: the number of records removed
     */
    @POST
    @Path(DnsApiConstants.EP_DELETE)
    @ReturnType("java.lang.Integer")
    public Response removeRecords(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                  DnsRecordMatch match) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return ResourceUtil.forbidden();

        if (!account.isAdmin()) match.setSubdomain(account.getName()+"."+configuration.getDyndns().getZone());

        try {
            final DnsManager dnsManager = configuration.getDnsManager();
            int removed;
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (dnsManager) {
                removed = dnsManager.remove(match);
                dnsManager.publish();
            }
            return ok(removed);

        } catch (Exception e) {
            log.error("Error removing records: "+e, e);
            return serverError();
        }
    }

}
