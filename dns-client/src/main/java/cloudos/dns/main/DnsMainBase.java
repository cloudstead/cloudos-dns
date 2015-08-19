package cloudos.dns.main;

import cloudos.dns.DnsApiConstants;
import cloudos.dns.model.support.DnsSessionRequest;
import org.cobbzilla.wizard.main.MainApiBase;
import org.cobbzilla.wizard.util.RestResponse;

import static cloudos.dns.DnsApiConstants.DNS_ENDPOINT;

public abstract class DnsMainBase<OPT extends DnsMainOptionsBase> extends MainApiBase<OPT> {

    @Override protected Object buildLoginRequest(OPT options) {
        return new DnsSessionRequest(options.getAccount(), options.getPassword());
    }

    @Override protected String getLoginUri(String account) { return DNS_ENDPOINT; }

    @Override protected String getApiHeaderTokenName() { return DnsApiConstants.H_API_KEY; }

    @Override protected String getSessionId(RestResponse response) throws Exception {
        if (response.status != 200) die("Error logging in: "+response);
        return response.json;
    }

    @Override protected void setSecondFactor(Object loginRequest, String token) {
        die("2-factor auth not yet supported");
    }

}
