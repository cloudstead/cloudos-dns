package cloudos.dns.main;

import com.google.common.collect.ImmutableMap;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.string.StringUtil;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.lang.reflect.Method;
import java.util.Map;

public class DnsMain {

    public final static Map<String, Class<?>> handlers
            = ImmutableMap.<String, Class<?>>builder()
            .put("admin", DnsAdminMain.class)
            .put("record", DnsRecordMain.class)
            .put("direct", DnsDirectMain.class)
            .build();

    public static void main (String[] args) {

        // redirect JUL -> logback using slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (args.length == 0) die("No command given. Use one of these: " + StringUtil.toString(handlers.keySet(), " "));

        // find the command
        final Class<?> handler = handlers.get(args[0]);
        if (handler == null) die("Unrecognized command: "+args[0]);

        // find the main method
        final Method mainMethod;
        try {
            mainMethod = handler.getMethod("main", String[].class);
        } catch (Exception e) {
            die("Error loading main method: "+e);
            return;
        }
        if (mainMethod == null) die("No main method found for "+handler.getName());

        // strip first arg (command name) and call main
        final String[] newArgs = ArrayUtil.remove(args, 0);
        try {
            mainMethod.invoke(null, (Object) newArgs);
        } catch (Exception e) {
            die("Error running main method: "+e);
        }
    }

    protected static void die(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

}
