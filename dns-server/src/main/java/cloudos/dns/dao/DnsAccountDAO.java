package cloudos.dns.dao;

import cloudos.dns.model.DnsAccount;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;
import org.cobbzilla.wizard.validation.UniqueValidatorDaoHelper;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository public class DnsAccountDAO extends UniquelyNamedEntityDAO<DnsAccount> {

    public DnsAccount findByNameAndPassword(String name, String password) {
        final DnsAccount found = findByName(name);
        return found.getPassword().isCorrectPassword(password) ? found : null;
    }

    @Override
    protected Map<String, UniqueValidatorDaoHelper.Finder<DnsAccount>> getUniqueHelpers() {
        return MapBuilder.build(new Object[][]{
                {"name", new UniqueValidatorDaoHelper.Finder<DnsAccount>() {
                    @Override public DnsAccount find(Object query) { return findByName(query.toString()); }
                }}
        });
    }
}
