package cloudos.dns.main;

import cloudos.dns.DnsClient;
import cloudos.dns.config.DynDnsConfiguration;
import cloudos.dns.service.DynDnsManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordBase;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJson;

/**
 * Edits a DNS record by using the underlying
 */
@NoArgsConstructor
public class DnsDirectMain {

    @Getter @Setter private DnsDirectMainOptions options = new DnsDirectMainOptions();

    public static void main (String[] args) throws Exception {
        final DnsDirectMain dnsMain = new DnsDirectMain();
        if (dnsMain.init(args)) dnsMain.run();
    }

    public boolean init(String[] args) {
        final CmdLineParser parser = new CmdLineParser(getOptions());
        try {
            parser.parseArgument(args);
            return true;

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return false;
        }
    }

    public void run () throws Exception {

        final File configFile = options.getConfigFile();
        if (!configFile.exists()) die("env file does not exist: "+abs(configFile));

        final DynDnsConfiguration config;
        if (options.hasConfigNode()) {
            config = fromJson(configFile, options.getConfigNode(), DynDnsConfiguration.class);
        } else {
            config = fromJson(configFile, DynDnsConfiguration.class);
        }
        if (!config.isValid()) die("config (" + abs(configFile) + ") is not valid. If connecting to Dyn, specify user, password, account and zone. If connecting to cloudos-dns, specify user, password, and base_uri");

        if (!config.isEnabled()) {
            System.err.println("config indicates DNS is disabled, exiting");
            return;
        }

        final DnsManager dnsManager = config.isDynDns() ? new DynDnsManager(config) : new DnsClient(config);

        final DnsRecordBase record;
        switch (options.getOperation()) {
            case add:
                if (options.hasSubdomain()) die("subdomain option is invalid for 'add' operations");
                record = options.getDnsRecord();
                // do not add if there is already an identical record
                if (dnsManager.list(new DnsRecordMatch(record)).isEmpty()) {
                    dnsManager.write((DnsRecord) record);
                    dnsManager.publish();
                }
                break;

            case remove:
                record = options.getDeleteRecordsMatch();
                dnsManager.remove((DnsRecordMatch) record);
                dnsManager.publish();
                break;

            case list:
                record = options.getListRecordsMatch();
                final List<DnsRecord> found = dnsManager.list((DnsRecordMatch) record);
                System.out.println("------ found "+found.size()+" records");
                for (DnsRecord rec : found) {
                    System.out.println(rec);
                }
                break;

        }
    }
}
