package cloudos.dns.mock;

import cloudos.dns.DnsClient;
import lombok.Getter;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;

import java.util.*;

public class MockDnsClient extends DnsClient {

    @Getter private final List<DnsOperation> actions = new ArrayList<>();
    @Getter private final Set<DnsRecord> records = new HashSet<>();

    public void reset () { actions.clear(); records.clear(); }

    @Override public void publish() throws Exception {
        actions.add(new DnsOperation(DnsCommand.PUBLISH));
    }

    @Override public String createOrUpdateUser(String username) throws Exception {
        actions.add(new DnsOperation(DnsCommand.ADD_USER, username, username));
        return username;
    }

    @Override public List<DnsRecord> list(DnsRecordMatch match) throws Exception {
        final List<DnsRecord> found = new ArrayList<>();
        for (DnsRecord r : records) {
            if (r.match(match)) found.add(r);
        }
        actions.add(new DnsOperation(DnsCommand.LIST, match, found));
        return found;
    }

    @Override public boolean write(DnsRecord record) throws Exception {
        records.add(record);
        actions.add(new DnsOperation(DnsCommand.WRITE, record, true));
        return true;
    }

    @Override public int remove(DnsRecordMatch match) throws Exception {
        int size = records.size();
        for (Iterator<DnsRecord> iter = records.iterator(); iter.hasNext(); ) {
            if (iter.next().match(match)) iter.remove();
        }
        int removed = size - records.size();
        actions.add(new DnsOperation(DnsCommand.REMOVE, match, removed));
        return removed;
    }
}
