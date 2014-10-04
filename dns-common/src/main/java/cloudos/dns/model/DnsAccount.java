package cloudos.dns.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jmkgreen.morphia.annotations.Embedded;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Entity;

@Entity @Accessors(chain=true) @ToString(callSuper=true)
public class DnsAccount extends UniquelyNamedEntity implements Scrubbable {

    private static final ScrubbableField[] SCRUBBABLE = new ScrubbableField[]{
            new ScrubbableField(DnsAccount.class, "password", String.class)
    };
    @Override @JsonIgnore public ScrubbableField[] getFieldsToScrub() { return SCRUBBABLE; }

    @Getter @Setter private boolean admin = false;
    @Getter @Setter @Embedded @JsonIgnore private HashedPassword password;

}
