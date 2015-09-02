package cloudos.dns.databag;

import cloudos.databag.InitDatabag;
import cloudos.databag.NameAndPassword;
import cloudos.server.DnsConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.dns.DnsServerType;

import java.io.File;

@Accessors(chain=true)
public class CloudOsDnsDatabag extends InitDatabag {

    public static final String APP = "cloudos-dns";

    // chef file management
    public static CloudOsDnsDatabag fromChefRepo(File dir) { return fromChefRepo(CloudOsDnsDatabag.class, dir, APP); }
    public static CloudOsDnsDatabag fromChefRepoOrNew(File dir) { return fromChefRepoOrNew(CloudOsDnsDatabag.class, dir, APP); }
    public static File getChefFile(File dir) { return getChefFile(CloudOsDnsDatabag.class, dir, APP); }
    public void toChefRepo(File dir) { toChefRepo(dir, APP); }

    @Getter @Setter private String server_tarball;
    @Getter @Setter private String server_shasum;

    @Getter @Setter private NameAndPassword admin;
    @Getter @Setter private DnsServerType server_type;
    @Getter @Setter private DnsConfiguration dyn;

}
