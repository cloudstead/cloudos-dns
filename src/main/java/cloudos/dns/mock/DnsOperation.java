package cloudos.dns.mock;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor
public class DnsOperation {

    private DnsCommand command;
    private Object param;
    private Object result;

    public DnsOperation(DnsCommand command) { this.command = command; }

}
