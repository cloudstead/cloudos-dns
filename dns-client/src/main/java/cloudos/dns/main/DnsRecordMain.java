package cloudos.dns.main;

import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.dns.DnsApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class DnsRecordMain extends DnsMainBase<DnsRecordOptions> {

    public static void main (String[] args) { main(DnsRecordMain.class, args); }

    @Override protected void run() throws Exception {
        final DnsRecordOptions options = getOptions();
        final ApiClientBase api = getApiClient();
        switch (options.getOperation()) {
            case list:
                out(api.doPost(DNS_ENDPOINT + EP_LIST, toJson(options.getListRecordsMatch())));
                break;

            case add:
                out(api.doPost(DNS_ENDPOINT + EP_UPDATE, toJson(options.getDnsRecord())));
                break;

            case remove:
                out(api.doPost(DNS_ENDPOINT + EP_DELETE, toJson(options.getDeleteRecordsMatch())));
                break;

            default:
                die("Invalid operation: "+options.getOperation());
                break;
        }
    }

}
