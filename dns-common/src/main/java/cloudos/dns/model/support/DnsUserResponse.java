package cloudos.dns.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@AllArgsConstructor @NoArgsConstructor @Accessors(chain=true)
public class DnsUserResponse {

    @Getter @Setter private String name;
    @Getter @Setter private String password;

}
