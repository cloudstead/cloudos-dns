package cloudos.dns.service;

import cloudos.dns.DnsApiConstants;
import cloudos.server.DnsConfiguration;
import com.dyn.client.v3.traffic.DynTrafficApi;
import com.dyn.client.v3.traffic.domain.CreateRecord;
import com.dyn.client.v3.traffic.domain.Job;
import com.dyn.client.v3.traffic.domain.Record;
import com.dyn.client.v3.traffic.domain.RecordId;
import com.dyn.client.v3.traffic.domain.rdata.*;
import com.dyn.client.v3.traffic.features.RecordApi;
import com.dyn.client.v3.traffic.features.ZoneApi;
import com.google.common.collect.FluentIterable;
import com.jayway.jsonpath.JsonPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.util.dns.DnsType;
import org.jclouds.ContextBuilder;
import org.jclouds.http.HttpResponseException;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Slf4j
public class DynDnsManager implements DnsManager {

    @Getter private DnsConfiguration config;
    private final DynTrafficApi dyn;

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final RecordApi recordApi = initRecordApi();
    private RecordApi initRecordApi() { return dyn.getRecordApiForZone(config.getZone()); }

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final ZoneApi zoneApi = initZoneApi();
    private ZoneApi initZoneApi() { return dyn.getZoneApi(); }

    public DynDnsManager(DnsConfiguration config) {
        this.config = config;
        // Configure/Authenticate the Dyn Java client instance
        final ProviderMetadata meta = Providers.withId("dyn-traffic");
        final ContextBuilder ctx = ContextBuilder.newBuilder(meta);
        ctx.credentials(config.getAccount()+":"+config.getUser(), config.getPassword());
        dyn = ctx.buildApi(DynTrafficApi.class);
    }

    @Override public List<DnsRecord> list(DnsRecordMatch match) throws Exception {

        if (match.hasFqdn() && match.hasSubdomain()) log.warn("both fqdn and subdomain defined -- ignoring fqdn");

        final RecordApi api = getRecordApi();
        final FluentIterable<RecordId> ids = match.hasFqdn() ? api.listByFQDN(match.getFqdn()) : api.list();

        final List<DnsRecord> results = new ArrayList<>();
        for (RecordId id : ids) {
            final Record rec = matches(match, id);
            if (rec != null) results.add(toDnsRecord(rec));
        }
        return results;
    }

    private DnsRecord toDnsRecord(Record record) {
        return (DnsRecord) new DnsRecord()
                .setOptions(getOptions(record))
                .setType(DnsType.valueOf(record.getType()))
                .setFqdn(record.getFQDN())
                .setValue(getValue(record));
    }

    private Map<String, String> getOptions(Record record) {
        Map<String, String> options = new HashMap<>();
        switch (DnsType.valueOf(record.getType())) {
            case MX:
                options.put("rank", String.valueOf(record.getRData().get("preference")));
                break;
        }
        return options;
    }

    @Override public boolean write(DnsRecord record) throws Exception {
        String fqdn = record.getFqdn();
        if (!fqdn.endsWith(".")) fqdn += ".";

        int ttl = record.getTtl();
        String value = record.getValue();

        final CreateRecord create;
        switch (record.getType()) {
            case A:
                create = CreateRecord.<AData> builder().type("A").fqdn(fqdn).ttl(ttl).rdata(AData.a(value)).build();
                break;
            case CNAME:
                if (!value.endsWith(".")) value += ".";
                create = CreateRecord.<CNAMEData> builder().type("CNAME").fqdn(fqdn).ttl(ttl).rdata(CNAMEData.cname(value)).build();
                break;
            case MX:
                if (!value.endsWith(".")) value += ".";
                int rank = record.getIntOption("rank", DnsApiConstants.DEFAULT_MX_RANK);
                create = CreateRecord.<MXData>builder().type("MX").fqdn(fqdn).ttl(ttl).rdata(MXData.mx(rank, value)).build();
                break;
            case NS:
                if (!value.endsWith(".")) value += ".";
                create = CreateRecord.<NSData>builder().type("NS").fqdn(fqdn).ttl(ttl).rdata(NSData.ns(value)).build();
                break;
            case TXT:
                create = CreateRecord.<TXTData>builder().type("TXT").fqdn(fqdn).ttl(ttl).rdata(TXTData.txt(value)).build();
                break;
            default:
                throw new IllegalArgumentException("Unsupported record type: "+record.getType());
        }

        final DnsRecordMatch match = new DnsRecordMatch(record);
        final List<DnsRecord> existing = list(match);

        boolean add = false;
        switch (existing.size()) {
            case 0:
                add = true;
                break;

            case 1:
                // if the value and options are the same, this is a no-op
                DnsRecord found = existing.get(0);
                if (found.equals(record)) {
                    log.warn("Identical record already exists, not re-adding. Requested to add: "+record+", found: "+found);
                } else {
                    log.warn("Record already exists, removing existing record ("+found+") and adding: "+record);
                    remove(match);
                    add = true;
                }
                break;

            default:
                die("Cannot add record (" + record + "), multiple existing records match: " + existing);
        }

        if (!add) return false;

        // ok -- create it
        final Job job;
        try {
            job = getRecordApi().scheduleCreate(create);
            if (job.getStatus() != Job.Status.SUCCESS) {
                die("Error scheduling job for record (" + record + "), status=" + job.getStatus() + ": " + create);
            }
            return true;

        } catch (HttpResponseException e) {
            if (isDuplicateRecordException(e)) {
                log.info("Duplicate record, silently not failing but leaving record as is. update="+record+", existing="+existing);
                return false;
            } else {
                throw e;
            }
        }
    }

    public static boolean isDuplicateRecordException(HttpResponseException e) {
        final String json = e.getContent();
        try {
            return e.getResponse().getStatusCode() == 400
                    // if one of the error messages has ERR_CD = 'TARGET_EXISTS', this is a duplicate record
                    && !((JSONArray) JsonPath.read(json, "$.msgs[?(@.ERR_CD == 'TARGET_EXISTS')]")).isEmpty();
        } catch (Exception e2) {
            log.error("error parsing json ("+json+"):" +e2, e2);
            return false; // it was an error, don't treat it as a duplicate.
        }
    }

    @Override public void publish() throws Exception {
        log.info("publish: " + dyn.getZoneApi().publish(config.getZone()));
    }

    @Override public int remove(DnsRecordMatch match) throws Exception {
        int removed = 0;
        for (RecordId id : getRecordApi().list()) {
            if (matches(match, id) != null) {
                log.info("remove: deleting record: "+id);
                getRecordApi().scheduleDelete(id);
                removed++;
            }
        }
        return removed;
    }

    protected Record matches(DnsRecordMatch match, RecordId id) {

        if (match.hasSubdomain()) {
            // fqdn of the record must be "subdomain" or "something.subdomain"
            if (!id.getFQDN().equalsIgnoreCase(match.getSubdomain()) && !id.getFQDN().toLowerCase().endsWith("."+match.getSubdomain().toLowerCase())) return null;

        }
        if (match.hasFqdn() && !match.getFqdn().equalsIgnoreCase(id.getFQDN())) return null;
        if (match.hasType() && !match.getType().name().equalsIgnoreCase(id.getType())) return null;

        Record record = null;
        if (match.hasValue()) {
            record = matches(match.getValue(), id);
            if (record == null) return null;
        }
        return record == null ? getRecordApi().get(id) : record;
    }

    protected Record matches(String value, RecordId id) {
        if (!isComparableType(DnsType.valueOf(id.getType()))) return null;
        final Record record = getRecordApi().get(id);
        return value.equalsIgnoreCase(getValue(record)) ? record : null;
    }

    protected boolean isComparableType(DnsType dnsType) {
        switch (dnsType) {
            case A: case CNAME: case MX: case NS: case SOA: case TXT: return true;
            default: return false;
        }
    }

    protected String getValue (Record record) {
        switch (DnsType.valueOf(record.getType())) {
            case A: return rdata(record, "address");
            case CNAME: return rdata(record, "cname");
            case MX: return rdata(record, "exchange");
            case NS: return rdata(record, "nsdname");
            case SOA: return rdata(record, "mname");
            case TXT: return rdata(record, "txtdata");
            default: return null;
        }
    }

    protected String rdata(Record record, String param) {
        return record.getRData().get(param).toString();
    }

}
