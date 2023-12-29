package ru.bssg.lottabyte.core.model.businessEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update business entity"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableBusinessEntityEntity extends BusinessEntityEntity {
    @JsonIgnore
    protected String description;

    public UpdatableBusinessEntityEntity(BusinessEntityEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setTechName(fromCopy.getTechName());
        this.setDefinition(fromCopy.getDefinition());
        this.setRegulation(fromCopy.getRegulation());
        if (fromCopy.getAltNames() != null) {
            this.setAltNames(new ArrayList<>(fromCopy.getAltNames()));
        }
        if (fromCopy.getSynonymIds() != null) {
            this.setSynonymIds(new ArrayList<>(fromCopy.getSynonymIds()));
        }
        if (fromCopy.getBeLinkIds() != null) {
            this.setBeLinkIds(new ArrayList<>(fromCopy.getBeLinkIds()));
        }
        this.setDomainId(fromCopy.getDomainId());
        this.setFormula(fromCopy.getFormula());
        this.setExamples(fromCopy.getExamples());
        this.setLink(fromCopy.getLink());
        this.setDatatypeId(fromCopy.getDatatypeId());
        this.setLimits(fromCopy.getLimits());
        this.setRoles(fromCopy.getRoles());
    }

}
