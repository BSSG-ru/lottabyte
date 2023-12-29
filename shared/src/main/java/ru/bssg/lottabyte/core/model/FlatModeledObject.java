package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@NoArgsConstructor
public class FlatModeledObject {

    protected String id;
    protected Integer num;
    protected String name;
    protected String description;
    protected Integer versionId;
    protected LocalDateTime modified;
    protected Boolean hasAccess;



    public FlatModeledObject(FlatModeledObject f) {
        this.id = f.getId();
        this.name = f.getName();
        this.description = f.getDescription();
        this.modified = f.getModified();
        this.versionId = f.getVersionId();
        this.num = f.num;
        this.hasAccess = f.hasAccess;
    }

}
