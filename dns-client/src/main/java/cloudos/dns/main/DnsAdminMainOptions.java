package cloudos.dns.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.main.MainApiOptionsBase;
import org.kohsuke.args4j.Option;

public class DnsAdminMainOptions extends MainApiOptionsBase {

    public static final String PASSWORD_ENV_VAR = "CLOUDOS_DNS_PASS";

    @Override protected String getPasswordEnvVarName() { return PASSWORD_ENV_VAR; }

    @Override protected String getDefaultApiBaseUri() { return "http://127.0.0.1:4002/api"; }

    public static final String USAGE_DOMAIN = "The sub-username (domain name)";
    public static final String OPT_DOMAIN = "-d";
    public static final String LONGOPT_DOMAIN = "--domain";
    @Option(name=OPT_DOMAIN, aliases=LONGOPT_DOMAIN, usage=USAGE_DOMAIN, required=true)
    @Getter @Setter private String domain;

    public static final String USAGE_DOMAIN_PASS = "The password for the sub-username (domain name)";
    public static final String OPT_DOMAIN_PASS = "-P";
    public static final String LONGOPT_DOMAIN_PASS = "--domain-password";
    @Option(name=OPT_DOMAIN_PASS, aliases=LONGOPT_DOMAIN_PASS, usage=USAGE_DOMAIN_PASS, required=false)
    @Getter @Setter private String domainPassword;

}
