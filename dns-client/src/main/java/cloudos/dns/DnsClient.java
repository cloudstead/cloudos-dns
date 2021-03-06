package cloudos.dns;

import cloudos.dns.model.support.DnsAccountRequest;
import cloudos.dns.model.support.DnsSessionRequest;
import cloudos.dns.model.support.DnsUserResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.util.http.ApiConnectionInfo;
import org.cobbzilla.wizard.client.ApiClientBase;

import java.util.Arrays;
import java.util.List;

import static cloudos.dns.DnsApiConstants.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@NoArgsConstructor
public class DnsClient extends ApiClientBase implements DnsManager {

    @Getter @Setter private String sessionId = null;

    public DnsClient(ApiConnectionInfo connectionInfo) {
        super(connectionInfo);
    }

    protected String getTokenHeader() { return DnsApiConstants.H_API_KEY; }

    @Override public void publish() throws Exception {
        // for now, noop -- all changes are published by server automatically
    }

    public String createOrUpdateUser(String username, String zone) throws Exception {
        return createOrUpdateUser(username, new DnsAccountRequest().setZone(zone));
    }

    public String createOrUpdateUser(String username, DnsAccountRequest request) throws Exception {
        initSession();
        final DnsUserResponse response = fromJson(post(DNS_ENDPOINT +EP_USER+"/"+username.toLowerCase(), toJson(request.setName(username))).json, DnsUserResponse.class);
        return response.getPassword();
    }

    @Override public List<DnsRecord> list(DnsRecordMatch match) throws Exception {
        initSession();
        return Arrays.asList(fromJson(post(DNS_ENDPOINT + EP_LIST, toJson(match)).json, DnsRecord[].class));
    }

    @Override public boolean write(DnsRecord record) throws Exception {
        initSession();
        return Boolean.valueOf(post(DNS_ENDPOINT + EP_UPDATE, toJson(record)).json);
    }

    @Override public int remove(DnsRecordMatch match) throws Exception {
        initSession();
        return Integer.parseInt(post(DNS_ENDPOINT + EP_DELETE, toJson(match)).json);
    }

    private void initSession() {
        if (sessionId != null) return;
        try {
            sessionId = post(DNS_ENDPOINT, toJson(new DnsSessionRequest(connectionInfo.getUser(), connectionInfo.getPassword()))).json;
            pushToken(sessionId);
        } catch (Exception e) {
            die("initSession: " + e, e);
        }
    }
}
