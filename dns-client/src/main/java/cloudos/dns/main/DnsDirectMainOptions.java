package cloudos.dns.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class DnsDirectMainOptions extends DnsRecordOptions {

    public static final String USAGE_CONFIG_FILE = "The JSON config file to source. Should be a single JSON object with fields for user, password, account, zone, and baseUri";
    public static final String OPT_CONFIG_FILE = "-c";
    public static final String LONGOPT_CONFIG_FILE = "--config";
    @Option(name=OPT_CONFIG_FILE, aliases=LONGOPT_CONFIG_FILE, usage=USAGE_CONFIG_FILE, required=true)
    @Getter @Setter private File configFile;

    public static final String USAGE_CONFIG_NODE = "The JSON-Path to the node containing the configuration.";
    public static final String OPT_CONFIG_NODE = "-n";
    public static final String LONGOPT_CONFIG_NODE = "--config-node";
    @Option(name=OPT_CONFIG_NODE, aliases=LONGOPT_CONFIG_NODE, usage=USAGE_CONFIG_NODE)
    @Getter @Setter private String configNode;
    public boolean hasConfigNode () { return !empty(configNode); }

}
