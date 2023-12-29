package ru.bssg.lottabyte.core.model.businessEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessEntityEntity extends Entity {

    private String techName;
    private String definition;
    private String regulation;
    private List<String> altNames;
    private List<String> synonymIds;
    private List<String> beLinkIds;
    private String domainId;
    private String parentId;
    @JsonIgnore
    protected String description;
    private String formula;
    private String examples;
    private String link;
    private String datatypeId;
    private String limits;
    private String roles;

    public BusinessEntityEntity() {
        super(ArtifactType.business_entity);
    }

}
