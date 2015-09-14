package cloudos.dns.service.dyn;

import cloudos.server.DnsConfiguration;
import com.dyn.client.v3.traffic.DynTrafficApi;
import com.dyn.client.v3.traffic.features.RecordApi;
import lombok.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

@Slf4j
public class DynRecordApi extends DynApiProxy<RecordApi> implements RecordApi {

    @Delegate(types=RecordApi.class)
    private RecordApi proxy = (RecordApi) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{RecordApi.class}, this);

    public DynRecordApi(DnsConfiguration config) { super(config); }

    @Override protected void initApi(DynTrafficApi api) {
        setRealApi(api.getRecordApiForZone(config.getZone()));
    }

}
