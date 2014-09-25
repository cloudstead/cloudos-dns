package cloudos.dns.server;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.http.ApiConnectionInfo;

import static org.cobbzilla.util.string.StringUtil.empty;

@NoArgsConstructor
public class DynDnsConfiguration extends ApiConnectionInfo {

    @Getter @Setter private String account;
    @Getter @Setter private String zone;

    public DynDnsConfiguration (String account, String zone, String user, String password) {
        super(null, user, password);
        this.account = account;
        this.zone = zone;
    }

    public boolean isDynDns () { return empty(getBaseUri()); }

    public boolean isValid () {
        return isDynDns()
                ? !empty(getUser()) && !empty(getPassword()) && !empty(getAccount()) && !empty(getZone())
                : !empty(getUser()) && !empty(getPassword()) && !empty(getBaseUri());
    }
}
