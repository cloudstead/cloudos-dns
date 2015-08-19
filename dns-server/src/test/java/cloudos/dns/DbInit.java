package cloudos.dns;

import cloudos.dns.server.DnsServerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class DbInit extends DnsTestBase {

    @Test public void init () throws Exception {
        // No longer creating admin account here
        // We now create a superuser during chef install, using info in the init data bag
        final DnsServerConfiguration config = (DnsServerConfiguration) serverHarness.getConfiguration();
        log.info("initialized database: "+ config.getDatabase().getUrl());
    }

}
