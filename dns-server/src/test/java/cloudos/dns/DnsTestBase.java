package cloudos.dns;

import cloudos.dns.server.DnsServer;
import cloudos.dns.server.DnsServerConfiguration;
import lombok.Getter;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;

import java.util.List;
import java.util.Map;

public class DnsTestBase extends ApiDocsResourceIT<DnsServerConfiguration, DnsServer> {

    public static final String TEST_ENV_FILE = ".cloudos-dns-test.env";
    @Getter private final Map<String, String> serverEnvironment = CommandShell.loadShellExportsOrDie(TEST_ENV_FILE);

    protected String getTestConfig() { return "dns-config-test.yml"; }

    @Override protected List<ConfigurationSource> getConfigurations() {
        return new SingletonList<ConfigurationSource>(new StreamConfigurationSource(getTestConfig()));
    }

}
