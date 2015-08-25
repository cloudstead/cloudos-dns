package cloudos.dns.server;

import cloudos.dns.service.DynDnsManager;
import cloudos.server.DnsConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsManager;
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

    // only one of these should be defined
    @Getter @Setter private RootyConfiguration rooty;
    @Getter @Setter private DnsConfiguration dyndns;

    public DnsManager getDnsManager () {
        if (rooty == null && dyndns == null) die("neither rooty nor dyndns defined");
        if (rooty != null && dyndns != null) die("both rooty and dyndns defined");

        if (rooty != null) return rooty.getHandler(DnsHandler.class);
        return new DynDnsManager(dyndns);
    }

    public String getZone() { return dyndns != null ? dyndns.getZone() : null; }

}
