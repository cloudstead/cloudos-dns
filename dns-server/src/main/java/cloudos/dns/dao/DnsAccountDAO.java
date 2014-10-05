package cloudos.dns.dao;

import cloudos.dns.model.DnsAccount;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;
import org.springframework.stereotype.Repository;

@Repository public class DnsAccountDAO extends UniquelyNamedEntityDAO<DnsAccount> {

    public DnsAccount findByNameAndPassword(String name, String password) {
        final DnsAccount found = findByName(name);
        return found.getPassword().isCorrectPassword(password) ? found : null;
    }

}
