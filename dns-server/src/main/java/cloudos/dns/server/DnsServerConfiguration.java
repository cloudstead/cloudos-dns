package cloudos.dns.server;

import cloudos.dns.config.DynDnsConfiguration;
import cloudos.dns.service.DynDnsManager;
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

@Configuration @Slf4j
public class DnsServerConfiguration extends RestServerConfiguration
        implements HasDatabaseConfiguration, HasRedisConfiguration {

    @Getter @Setter private String dataKey;

    @Getter @Setter private DnsRedisConfiguration redis = new DnsRedisConfiguration(this);

    @Setter private DatabaseConfiguration database;
    @Bean public DatabaseConfiguration getDatabase() { return database; }

    // only one of these should be defined
    @Getter @Setter private RootyConfiguration rooty;
    @Getter @Setter private DynDnsConfiguration dyndns;

    public DnsManager getDnsManager () {
        if (rooty == null && dyndns == null) throw new IllegalStateException("neither rooty nor dyndns defined");
        if (rooty != null && dyndns != null) throw new IllegalStateException("both rooty and dyndns defined");

        if (rooty != null) return rooty.getHandler(DnsHandler.class);
        return new DynDnsManager(dyndns);
    }

}
