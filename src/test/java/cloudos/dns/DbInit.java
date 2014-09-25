package cloudos.dns;

import org.junit.Test;

public class DbInit extends DnsTestBase {

    @Test public void init () throws Exception {

        // No longer creating admin below
        // We now create a superuser during chef install, using info in the init data bag

        // create cloudstead admin user
//        final Map<String, String> exports = CommandShell.loadShellExports(".cloudstead.env");
//        final String user = exports.get("CLOUDOS_DNS_USER");
//        final String password = exports.get("CLOUDOS_DNS_PASSWORD");
//        final DnsAccountDAO accountDAO = getBean(DnsAccountDAO.class);
//        accountDAO.create(new DnsAccount().setName(user).setPassword(new HashedPassword(password)));
    }

}
