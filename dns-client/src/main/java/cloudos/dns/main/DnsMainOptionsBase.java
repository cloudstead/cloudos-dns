package cloudos.dns.main;

import org.cobbzilla.wizard.main.MainApiOptionsBase;

public class DnsMainOptionsBase extends MainApiOptionsBase {

    public static final String PASSWORD_ENV_VAR = "CLOUDOS_DNS_PASS";

    @Override protected String getPasswordEnvVarName() { return PASSWORD_ENV_VAR; }

    @Override protected String getDefaultApiBaseUri() { return "http://127.0.0.1:4002/api"; }


}
