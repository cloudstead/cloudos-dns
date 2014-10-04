package cloudos.dns.dao;

import cloudos.dns.model.DnsAccount;
import cloudos.dns.server.DnsServerConfiguration;
import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SessionDAO extends AbstractSessionDAO<DnsAccount> {

    @Autowired private DnsServerConfiguration configuration;

    @Override protected Class<DnsAccount> getEntityClass() { return DnsAccount.class; }

    @Override protected String getPassphrase() { return configuration.getDataKey(); }

    protected String toJson(DnsAccount account) throws Exception { return super.toJson(account.setPassword(null)); }
    protected DnsAccount fromJson(String json) throws Exception { return super.fromJson(json).setPassword(null); }

}
