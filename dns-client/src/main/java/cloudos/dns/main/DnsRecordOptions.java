package cloudos.dns.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordBase;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.util.dns.DnsType;
import org.cobbzilla.util.string.ValidationRegexes;
import org.kohsuke.args4j.Option;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class DnsRecordOptions extends DnsMainOptionsBase {

    public static final String USAGE_OPERATION = "The operation to perform. Can be add, remove or list. Default is list.";
    public static final String OPT_OPERATION = "-o";
    public static final String LONGOPT_OPERATION = "--operation";
    @Option(name=OPT_OPERATION, aliases=LONGOPT_OPERATION, usage=USAGE_OPERATION)
    @Getter @Setter private DnsOperation operation = DnsOperation.list;

    public static final String USAGE_TYPE = "The type of DNS record. Required for 'add' and 'remove' operations.";
    public static final String OPT_TYPE = "-r";
    public static final String LONGOPT_TYPE = "--record";
    @Option(name=OPT_TYPE, aliases=LONGOPT_TYPE, usage=USAGE_TYPE)
    @Getter @Setter private DnsType type;

    public static final String USAGE_FQDN = "The FQDN of the DNS record. Required for 'add' and 'remove' operations.";
    public static final String OPT_FQDN = "-f";
    public static final String LONGOPT_FQDN = "--fqdn";
    @Option(name=OPT_FQDN, aliases=LONGOPT_FQDN, usage=USAGE_FQDN)
    @Getter private String fqdn;
    public void setFqdn(String fqdn) {
        if (!ValidationRegexes.HOST_PATTERN.matcher(fqdn).matches()) die("setFqdn: invalid hostname: "+fqdn);
        this.fqdn = fqdn;
    }

    public static final String USAGE_SUBDOMAIN = "The subdomain to limit records to.";
    public static final String OPT_SUBDOMAIN = "-S";
    public static final String LONGOPT_SUBDOMAIN = "--subdomain";
    @Option(name=OPT_SUBDOMAIN, aliases=LONGOPT_SUBDOMAIN, usage=USAGE_SUBDOMAIN)
    @Getter @Setter private String subdomain;
    public boolean hasSubdomain() { return !empty(subdomain); }

    public static final String USAGE_VALUE = "The value of the DNS record. Required for 'add' operations.";
    public static final String OPT_VALUE = "-v";
    public static final String LONGOPT_VALUE = "--value";
    @Option(name=OPT_VALUE, aliases=LONGOPT_VALUE, usage=USAGE_VALUE)
    @Getter @Setter private String value;

    public static final String USAGE_TTL = "The TTL of the DNS record, in seconds. Default is 1 day (86400).";
    public static final String OPT_TTL = "-t";
    public static final String LONGOPT_TTL = "--ttl";
    @Option(name=OPT_TTL, aliases=LONGOPT_TTL, usage=USAGE_TTL)
    @Getter @Setter private int ttl = (int) TimeUnit.DAYS.toSeconds(1);

    public static final String USAGE_OPTIONS = "Options for the DNS record. Optional. Must be comma-separated list of key=value. Enclose with quotes if using spaces.";
    public static final String OPT_OPTIONS = "-O";
    public static final String LONGOPT_OPTIONS = "--options";
    @Option(name=OPT_OPTIONS, aliases=LONGOPT_OPTIONS, usage=USAGE_OPTIONS)
    @Getter @Setter private String options;

    public DnsRecordBase getDnsRecord() {
        DnsRecordBase record;
        record = new DnsRecord()
                .setTtl(getTtl())
                .setOptionsString(getOptions())
                .setType(getType())
                .setFqdn(getFqdn())
                .setValue(getValue());
        return record;
    }

    public DnsRecordBase getListRecordsMatch() {
        DnsRecordBase record;
        record = new DnsRecordMatch()
                .setSubdomain(getSubdomain())
                .setType(getType())
                .setFqdn(getFqdn());
        return record;
    }


    public DnsRecordBase getDeleteRecordsMatch() {
        return new DnsRecordMatch()
                .setSubdomain(getSubdomain())
                .setType(getType())
                .setFqdn(getFqdn())
                .setValue(getValue());
    }
}
