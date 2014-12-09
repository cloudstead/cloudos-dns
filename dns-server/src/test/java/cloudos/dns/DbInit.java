package cloudos.dns;

import cloudos.dns.server.DnsServerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class DbInit extends DnsTestBase {

    @Test public void init () throws Exception {

        // No longer creating admin below
        // We now create a superuser during chef install, using info in the init data bag
        final DnsServerConfiguration config = (DnsServerConfiguration) serverHarness.getConfiguration();
        log.info("initialized database: "+ config.getDatabase().getUrl());

        // create cloudstead admin user
//        final Map<String, String> exports = CommandShell.loadShellExports(".cloudstead.env");
//        final String user = exports.get("CLOUDOS_DNS_USER");
//        final String password = exports.get("CLOUDOS_DNS_PASSWORD");
//        final DnsAccountDAO accountDAO = getBean(DnsAccountDAO.class);
//        final DnsAccount admin = (DnsAccount) new DnsAccount()
//                .setPassword(new HashedPassword(password))
//                .setName(user);
//        accountDAO.create(admin);
    }

}
