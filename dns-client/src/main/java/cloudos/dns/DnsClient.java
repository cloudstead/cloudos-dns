package cloudos.dns;

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

    public String createUser (String username) throws Exception {
        initSession();
        final DnsUserResponse response = fromJson(post(ENDPOINT+EP_USER+"/"+username.toLowerCase(), null).json, DnsUserResponse.class);
        return response.getPassword();
    }

    @Override public List<DnsRecord> list(DnsRecordMatch match) throws Exception {
        initSession();
        return Arrays.asList(fromJson(post(ENDPOINT + EP_LIST, toJson(match)).json, DnsRecord[].class));
    }

    @Override public boolean write(DnsRecord record) throws Exception {
        initSession();
        return Boolean.valueOf(post(ENDPOINT + EP_UPDATE, toJson(record)).json);
    }

    @Override public int remove(DnsRecordMatch match) throws Exception {
        initSession();
        return Integer.parseInt(post(ENDPOINT + EP_DELETE, toJson(match)).json);
    }

    private void initSession() {
        if (sessionId != null) return;
        try {
            sessionId = post(ENDPOINT, toJson(new DnsSessionRequest(connectionInfo.getUser(), connectionInfo.getPassword()))).json;
            pushToken(sessionId);
        } catch (Exception e) {
            throw new IllegalStateException("initSession: "+e, e);
        }
    }
}
