package cloudos.dns.main;

import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.dns.DnsApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class DnsAdminMain extends DnsMainBase<DnsAdminMainOptions> {

    public static void main (String[] args) { main(DnsAdminMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final DnsAdminMainOptions options = getOptions();
        final String uri;

        switch (options.getOperation()) {
            case list:
                out(api.doGet(DNS_ENDPOINT + EP_USER_LIST));
                break;

            case add:
                if (!options.hasDomain()) die("Domain is required");
                if (!options.hasZone()) die("Zone is required");
                uri = DNS_ENDPOINT + EP_USER + "/" + options.getDomain();
                out(api.doPost(uri, toJson(options.getDnsAccountRequest())));
                break;

            case remove:
                if (!options.hasDomain()) die("Domain is required");
                uri = DNS_ENDPOINT + EP_USER + "/" + options.getDomain();
                out(api.doDelete(uri));
                break;

            default:
                die("Unrecognized operation: "+options.getOperation());
                break;
        }
    }

}
