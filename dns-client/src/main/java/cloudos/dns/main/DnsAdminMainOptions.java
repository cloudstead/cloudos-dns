package cloudos.dns.main;

import cloudos.dns.model.support.DnsAccountRequest;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class DnsAdminMainOptions extends DnsMainOptionsBase {

    public static final String USAGE_OPERATION = "The operation to perform";
    public static final String OPT_OPERATION = "-o";
    public static final String LONGOPT_OPERATION = "--operation";
    @Option(name=OPT_OPERATION, aliases=LONGOPT_OPERATION, usage=USAGE_OPERATION)
    @Getter @Setter private DnsOperation operation = DnsOperation.list;

    public static final String USAGE_DOMAIN = "The sub-username (domain name). Required for add operation";
    public static final String OPT_DOMAIN = "-d";
    public static final String LONGOPT_DOMAIN = "--domain";
    @Option(name=OPT_DOMAIN, aliases=LONGOPT_DOMAIN, usage=USAGE_DOMAIN)
    @Setter private String domain;
    public String getDomain() { return empty(domain) ? domain : domain.toLowerCase(); }
    public boolean hasDomain() { return !empty(domain); }

    public static final String USAGE_ZONE = "The parent DNS zone. Required for add operation";
    public static final String OPT_ZONE = "-z";
    public static final String LONGOPT_ZONE = "--zone";
    @Option(name=OPT_ZONE, aliases=LONGOPT_ZONE, usage=USAGE_ZONE)
    @Getter @Setter private String zone;
    public boolean hasZone() { return !empty(zone); }

    public static final String USAGE_DOMAIN_PASS = "The password for the sub-username (domain name)";
    public static final String OPT_DOMAIN_PASS = "-P";
    public static final String LONGOPT_DOMAIN_PASS = "--domain-password";
    @Option(name=OPT_DOMAIN_PASS, aliases=LONGOPT_DOMAIN_PASS, usage=USAGE_DOMAIN_PASS)
    @Getter @Setter private String domainPassword;

    public DnsAccountRequest getDnsAccountRequest() {
        return new DnsAccountRequest()
                .setName(getDomain().toLowerCase())
                .setZone(getZone())
                .setPassword(getDomainPassword());
    }

}
