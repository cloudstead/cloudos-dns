package cloudos.dns.server;

import cloudos.databag.DnsMode;
import org.cobbzilla.util.dns.DnsServerType;
import cloudos.dns.service.DynDnsManager;
import cloudos.server.DnsConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.http.ApiConnectionInfo;
import org.cobbzilla.wizard.cache.redis.HasRedisConfiguration;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rooty.RootyConfiguration;
import rooty.toots.djbdns.DnsHandler;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Configuration @Slf4j
public class DnsServerConfiguration extends RestServerConfiguration
        implements HasDatabaseConfiguration, HasRedisConfiguration {

    @Getter @Setter private String dataKey;

    @Getter @Setter private DnsRedisConfiguration redis = new DnsRedisConfiguration(this);

    @Setter private DatabaseConfiguration database;
    @Bean public DatabaseConfiguration getDatabase() { return database; }

    @Getter @Setter private DnsServerType serverType;

    // only one of these will be defined, based on mode defined above
    @Setter private RootyConfiguration rooty;
    @Setter private DnsConfiguration dyn;
    @Setter private ApiConnectionInfo external;

    @Getter(lazy=true) private final DnsManager dnsManager = initDnsManager();

    public DnsManager initDnsManager () {
        switch (serverType) {
            case dyn:
                dyn.setMode(DnsMode.dyn);
                return new DynDnsManager(dyn);

            case djbdns:
                return rooty.getHandler(DnsHandler.class);

            default: return die("initDnsManager: Unsupported internal mode: "+ serverType);
        }
    }

    public String getZone() { return dyn != null ? dyn.getZone() : null; }

}
