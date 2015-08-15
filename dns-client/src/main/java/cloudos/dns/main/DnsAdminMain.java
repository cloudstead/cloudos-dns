package cloudos.dns.main;

import cloudos.dns.DnsApiConstants;
import cloudos.dns.model.support.DnsSessionRequest;
import org.cobbzilla.wizard.main.MainApiBase;
import org.cobbzilla.wizard.util.RestResponse;

import static cloudos.dns.DnsApiConstants.ENDPOINT;
import static cloudos.dns.DnsApiConstants.EP_USER;

public class DnsAdminMain extends MainApiBase<DnsAdminMainOptions> {

    public static void main (String[] args) { main(DnsAdminMain.class, args); }

    @Override protected Object buildLoginRequest(DnsAdminMainOptions options) {
        return new DnsSessionRequest(options.getAccount(), options.getPassword());
    }

    @Override protected String getLoginUri(String account) { return ENDPOINT; }

    @Override protected String getApiHeaderTokenName() { return DnsApiConstants.H_API_KEY; }

    @Override protected String getSessionId(RestResponse response) throws Exception {
        if (response.status != 200) die("Error logging in: "+response);
        return response.json;
    }

    @Override protected void setSecondFactor(Object loginRequest, String token) {
        die("2-factor auth not yet supported");
    }

    @Override protected void run() throws Exception {
        final DnsAdminMainOptions options = getOptions();
        final String domain = options.getDomain().toLowerCase();
        System.out.println(getApiClient().post(ENDPOINT + EP_USER + "/" + domain, options.getDomainPassword()).json);
    }
}
