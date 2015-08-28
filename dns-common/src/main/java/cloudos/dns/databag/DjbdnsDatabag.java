package cloudos.dns.databag;

import cloudos.databag.InitDatabag;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

public class DjbdnsDatabag extends InitDatabag {

    public static final String APP = "djbdns";

    public static File getChefFile(File dir) { return getChefFile(DjbdnsDatabag.class, dir, APP); }
    public static DjbdnsDatabag fromChefRepoOrNew(File dir) { return fromChefRepoOrNew(DjbdnsDatabag.class, dir, APP); }
    public void toChefRepo(File dir) { toChefRepo(dir, APP); }

    @Getter @Setter private String allow_axfr;

}
