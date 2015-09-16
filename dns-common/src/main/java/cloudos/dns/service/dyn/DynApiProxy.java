package cloudos.dns.service.dyn;

import cloudos.server.DnsConfiguration;
import com.dyn.client.v3.traffic.DynTrafficApi;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jclouds.ContextBuilder;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.string.StringUtil.exceptionContainsMessage;

@Slf4j
public abstract class DynApiProxy<T> implements InvocationHandler {

    protected final DnsConfiguration config;
    protected static final AtomicReference<DynTrafficApi> dyn = new AtomicReference<>();
    private static final List<DynApiProxy> proxies = new ArrayList<>();

    @Setter private T realApi = null;

    public T getRealApi() {
        if (realApi == null) {
            getApi();
            if (realApi == null) die("getRealApi: getRealApi didn't set realApi");
        }
        return realApi;
    }

    public DynApiProxy(DnsConfiguration config) {
        this.config = config;
        proxies.add(this);
    }

    private DynTrafficApi getApi() {
        if (dyn.get() == null) {
            synchronized (dyn) {
                if (dyn.get() == null) {
                    // Configure/Authenticate the Dyn Java client instance
                    final ProviderMetadata meta = Providers.withId("dyn-traffic");
                    final ContextBuilder ctx = ContextBuilder.newBuilder(meta);
                    ctx.credentials(config.getAccount() + ":" + config.getUser(), config.getPassword());
                    dyn.set(ctx.buildApi(DynTrafficApi.class));
                }
                for (DynApiProxy p : proxies) p.setRealApi(p.getRealApi(dyn.get()));
            }
        }
        return dyn.get();
    }

    protected abstract T getRealApi(DynTrafficApi api);

    protected DynTrafficApi resetApi() {
        dyn.set(null);
        return getApi();
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        synchronized (dyn) {
            try {
                return method.invoke(getRealApi(), args);

            } catch (Exception e) {
                if (exceptionContainsMessage(e, "inactivity logout")) {
                    log.warn(method.getName() + ": inactivity logout, will rebuild API and retry once");
                    resetApi();
                    return method.invoke(proxy, args);
                } else {
                    throw e;
                }
            }
        }
    }
}
