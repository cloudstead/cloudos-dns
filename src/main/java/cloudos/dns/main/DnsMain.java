package cloudos.dns.main;

import cloudos.dns.DnsClient;
import cloudos.dns.server.DynDnsConfiguration;
import cloudos.dns.service.DynDnsManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordBase;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.util.List;

@NoArgsConstructor
public class DnsMain {

    @Getter @Setter private DnsMainOptions options = new DnsMainOptions();

    public static void main (String[] args) throws Exception {
        final DnsMain dnsMain = new DnsMain();
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
        if (!configFile.exists()) throw new IllegalArgumentException("env file does not exist: "+configFile.getAbsolutePath());

        final DynDnsConfiguration config;
        if (options.hasConfigNode()) {
            config = JsonUtil.fromJson(FileUtil.toString(configFile), options.getConfigNode(), DynDnsConfiguration.class);
        } else {
            config = JsonUtil.fromJson(FileUtil.toString(configFile), DynDnsConfiguration.class);
        }
        if (!config.isValid()) {
            throw new IllegalArgumentException("config ("+configFile.getAbsolutePath()+ ") is not valid. If connecting to Dyn, specify user, password, account and zone. If connecting to cloudos-DNS, use user, password, and base_uri");
        }
        final DnsManager dnsManager = config.isDynDns() ? new DynDnsManager(config) : new DnsClient(config);

        final DnsRecordBase record;
        switch (options.getOperation()) {
            case add:
                if (options.hasSubdomain()) throw new IllegalArgumentException("subdomain option is invalid for 'add' operations");
                record = new DnsRecord()
                        .setTtl(options.getTtl())
                        .setOptions(options.getOptionsMap())
                        .setType(options.getType())
                        .setFqdn(options.getFqdn())
                        .setValue(options.getValue());
                // do not add if there is already an identical record
                if (dnsManager.list(new DnsRecordMatch(record)).isEmpty()) {
                    dnsManager.write((DnsRecord) record);
                    dnsManager.publish();
                }
                break;

            case remove:
                record = new DnsRecordMatch()
                        .setSubdomain(options.getSubdomain())
                        .setType(options.getType())
                        .setFqdn(options.getFqdn())
                        .setValue(options.getValue());
                dnsManager.remove((DnsRecordMatch) record);
                dnsManager.publish();
                break;

            case list:
                record = new DnsRecordMatch()
                        .setSubdomain(options.getSubdomain())
                        .setType(options.getType())
                        .setFqdn(options.getFqdn());
                final List<DnsRecord> found = dnsManager.list((DnsRecordMatch) record);
                System.out.println("------ found "+found.size()+" records");
                for (DnsRecord rec : found) {
                    System.out.println(rec);
                }
                break;

        }
    }

}