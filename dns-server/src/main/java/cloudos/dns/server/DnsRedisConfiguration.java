package cloudos.dns.server;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.cache.redis.RedisConfiguration;

@AllArgsConstructor
public class DnsRedisConfiguration extends RedisConfiguration {

    private DnsServerConfiguration configuration;

    @Override public String getKey() { return configuration.getDataKey(); }

}
