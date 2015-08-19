package cloudos.dns.dao;

import cloudos.dns.model.DnsAccount;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository public class DnsAccountDAO extends UniquelyNamedEntityDAO<DnsAccount> {

    public DnsAccount findByNameAndPassword(String name, String password) {
        final DnsAccount found = findByName(name);
        return found == null ? null : found.getPassword().isCorrectPassword(password) ? found : null;
    }

    private static final Transformer NAME_XFORM = new Transformer() {
        @Override public Object transform(Object o) { return ((DnsAccount) o).getName(); }
    };

    public List<String> findAllNames() {
        return (List<String>) CollectionUtils.collect(findAll(), NAME_XFORM);
    }

}
