package cloudos.dns.server;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;

@Slf4j
public class DnsServer extends RestServerBase<DnsServerConfiguration> {

    private static final String[] API_CONFIG_YML = {"dns-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {
        final List<ConfigurationSource> configSources = getStreamConfigurationSources(DnsServer.class, API_CONFIG_YML);
        main(DnsServer.class, configSources);
    }

}
