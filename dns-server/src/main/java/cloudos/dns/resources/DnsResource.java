package cloudos.dns.resources;

import cloudos.dns.DnsApiConstants;
import cloudos.dns.dao.DnsAccountDAO;
import cloudos.dns.dao.SessionDAO;
import cloudos.dns.model.DnsAccount;
import cloudos.dns.model.support.DnsAccountRequest;
import cloudos.dns.model.support.DnsSessionRequest;
import cloudos.dns.model.support.DnsUserResponse;
import cloudos.dns.server.DnsServerConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.DnsRecordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static cloudos.dns.DnsApiConstants.*;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(DNS_ENDPOINT)
@Service @Slf4j
public class DnsResource {

    @Autowired private DnsServerConfiguration configuration;
    @Autowired private DnsAccountDAO accountDAO;
    @Autowired private SessionDAO sessionDAO;

    /**
     * Simple health check
     * @return A message indicating that the server is running
     * @statuscode 500 if the server cannot connect to the database
     */
    @GET
    @ReturnType("java.lang.String")
    public Response hello () {
        try {
            accountDAO.findByName("");
        } catch (Exception e) {
            log.error("Error connecting to database: "+e, e);
            return serverError();
        }
        return ok(configuration.getServerName()+" is running\n");
    }

    /**
     * Start a new session (login)
     * @param request the session request (login credentials)
     * @return A new session token
     */
    @POST
    @ReturnType("java.lang.String")
    public Response startSession (DnsSessionRequest request) {

        final String zone = configuration.getZone();

        // strip zone suffix if present
        String user = request.getUser();
        if (!empty(zone) && user.endsWith(zone)) user = user.substring(0, user.length() - zone.length());

        final DnsAccount account = accountDAO.findByNameAndPassword(user, request.getPassword());
        if (account == null) return notFound();
        return ok(sessionDAO.create(account));
    }

    /**
     * List all DNS accounts. Must be admin.
     * @param apiKey the session token
     * @return a List of accounts
     */
    @GET
    @Path(EP_USER_LIST)
    @ReturnType("java.util.List<cloudos.dns.model.DnsAccount>")
    public Response list (@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null || !account.isAdmin()) return forbidden();

        return ok(accountDAO.findAll());
    }

    /**
     * Create or update a user. Must be an admin.
     * @param apiKey The session token
     * @param name Name of the user to create
     * @param request a DnsAccountRequest containing the password and zone
     * @return The newly created DnsUser
     * @statuscode 403 if the user is not an admin or if using Dyn and the account.zone does not match the Dyn zone
     */
    @POST
    @Path(EP_USER + "/{name}")
    @ReturnType("cloudos.dns.model.support.DnsUserResponse")
    public Response createOrUpdateUser(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                       @PathParam("name") String name,
                                       @Valid DnsAccountRequest request) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return forbidden();
        if (!account.isAdmin()) return forbidden();

        name = name.toLowerCase();
        String zone = configuration.getZone();
        if (!empty(zone)) {
            if (!request.isZone(zone)) return forbidden();
        } else {
            zone = request.getZone();
        }
        if (name.endsWith(zone)) name = name.substring(0, name.length()-zone.length()-1);

        if (name.isEmpty() || name.length() > 63) return invalid("err.name.length");
        if (!name.matches("[A-Za-z0-9\\-]+") || name.startsWith("-")) return invalid("err.name.invalid");

        final DnsAccount existing = accountDAO.findByName(name);
        if (existing != null) {
            log.warn("Overwriting existing account: "+existing);
            accountDAO.delete(existing.getUuid());
        }

        if (!request.hasPassword()) request.setPassword(randomAlphanumeric(10));
        accountDAO.create(new DnsAccount(request));

        return ok(new DnsUserResponse(name, request.getPassword()));
    }

    /**
     * List all DNS accounts. Must be admin.
     * @param apiKey the session token
     * @return a List of accounts
     */
    @DELETE
    @Path(EP_USER + "/{name}")
    @ReturnType("java.lang.Void")
    public Response deleteUser (@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                @PathParam("name") String name) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null || !account.isAdmin()) return forbidden();

        final DnsAccount found = accountDAO.findByName(name);
        if (found == null) return notFound(name);

        // cannot delete self
        if (found.getUuid().equals(account.getUuid())) return forbidden();

        accountDAO.delete(found.getUuid());
        return ok();
    }

    /**
     * List all records that match a DnsRecordMatch query
     * @param apiKey the session token
     * @param match the query
     * @return a List of DnsRecords that match. If none are found, an empty list is returned (not a 404)
     */
    @POST
    @Path(EP_LIST)
    @ReturnType("java.util.List<org.cobbzilla.util.dns.DnsRecord>")
    public Response list (@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                          DnsRecordMatch match) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return forbidden();

        if (!account.isAdmin()) match.setSubdomain(account.getName()+"."+account.getZone());

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
     * @statuscode 403 if the record is not within the zone associated with the caller's account
     */
    @POST
    @Path(EP_UPDATE)
    @ReturnType("java.lang.Boolean")
    public Response writeRecord(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                DnsRecord record) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return forbidden();

        final List<ConstraintViolationBean> errors = DnsRecordValidator.validate(record);
        if (!empty(errors)) return invalid(errors);

        // admins can modify anything. users can only modify records within their subdomain (their username)
        if (!account.isAdmin()) {
            if (!record.getFqdn().endsWith(account.getName() + "." + account.getZone())) {
                return forbidden();
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
            log.error("Error writing record ("+record+"): "+e, e);
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
    @Path(EP_DELETE)
    @ReturnType("java.lang.Integer")
    public Response removeRecords(@HeaderParam(DnsApiConstants.H_API_KEY) String apiKey,
                                  DnsRecordMatch match) {

        final DnsAccount account = sessionDAO.find(apiKey);
        if (account == null) return forbidden();

        if (!account.isAdmin()) match.setSubdomain(account.getName()+"."+account.getZone());

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
