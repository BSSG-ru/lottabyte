package ru.bssg.lottabyte.core.model.ldapProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class LdapProperty extends ModeledObject<LdapPropertyEntity> {

    public LdapProperty() {
    }

    public LdapProperty(LdapPropertyEntity entity) {
        super(entity);
    }

    public LdapProperty(LdapPropertyEntity entity, Metadata md) {
        super(entity, md, ArtifactType.ldap_properties);
    }

}
