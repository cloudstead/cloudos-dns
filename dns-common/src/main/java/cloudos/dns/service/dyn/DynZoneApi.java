package cloudos.dns.service.dyn;

import cloudos.server.DnsConfiguration;
import com.dyn.client.v3.traffic.DynTrafficApi;
import com.dyn.client.v3.traffic.features.ZoneApi;
import lombok.Delegate;

import java.lang.reflect.Proxy;

public class DynZoneApi extends DynApiProxy<ZoneApi> implements ZoneApi {

    @Delegate(types=ZoneApi.class)
    private ZoneApi proxy = (ZoneApi) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ZoneApi.class}, this);

    public DynZoneApi(DnsConfiguration config) { super(config); }

    @Override protected void initApi(DynTrafficApi api) { setRealApi(dyn.get().getZoneApi()); }

}
