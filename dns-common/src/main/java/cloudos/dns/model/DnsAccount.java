package cloudos.dns.model;

import cloudos.dns.model.support.DnsAccountRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jmkgreen.morphia.annotations.Embedded;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.ValidationRegexes;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Entity @NoArgsConstructor
@Accessors(chain=true) @ToString(callSuper=true)
public class DnsAccount extends UniquelyNamedEntity implements Scrubbable {

    private static final ScrubbableField[] SCRUBBABLE = new ScrubbableField[]{
            new ScrubbableField(DnsAccount.class, "password", String.class)
    };

    public DnsAccount(DnsAccountRequest request) {
        setName(request.getName());
        setPassword(new HashedPassword(request.getPassword()));
        setZone(request.getZone());
    }

    @Override @JsonIgnore public ScrubbableField[] fieldsToScrub() { return SCRUBBABLE; }

    @Column(length=1024) @Size(max=1024, message="err.zone.length")
    @Pattern(regexp=ValidationRegexes.DOMAIN_REGEX, flags=Pattern.Flag.CASE_INSENSITIVE, message="err.zone.invalid")
    @Getter @Setter private String zone;

    @Getter @Setter private boolean admin = false;

    @Getter @Setter @Embedded @JsonIgnore private HashedPassword password;

}
