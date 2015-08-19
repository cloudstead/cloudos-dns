package cloudos.dns.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.ValidationRegexes;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true)
public class DnsAccountRequest {

    @HasValue(message="err.name.empty")
    @Size(min=2, max=UniquelyNamedEntity.NAME_MAXLEN, message="err.name.length")
    @Getter @Setter private String name;

    @Getter @Setter private String password;
    public boolean hasPassword() { return !empty(password); }

    @HasValue(message="err.zone.empty")
    @Size(max=1024, message="err.zone.length")
    @Pattern(regexp=ValidationRegexes.DOMAIN_REGEX, flags=Pattern.Flag.CASE_INSENSITIVE, message="err.zone.invalid")
    @Getter @Setter private String zone;

    public boolean isZone(String z) {
        if (empty(z)) return empty(zone);
        return !empty(zone) && z.equals(zone);
    }
}
