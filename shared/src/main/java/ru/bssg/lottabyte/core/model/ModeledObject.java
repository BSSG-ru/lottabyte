package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema
@Data
public class ModeledObject<T extends Entity> {

    private Metadata metadata;
    @Schema(
            description = "Entity"
    )
    protected T entity;

    public ModeledObject(T entity, Metadata md) {
        this.metadata = md;
        this.entity = entity;
    }

    public ModeledObject(T entity, Metadata md, ArtifactType artifactType) {
        this.metadata = md;
        this.entity = entity;
        this.metadata.setArtifactType(artifactType.getText());
    }

    public ModeledObject(T entity) {
        this.metadata = new Metadata();
        LocalDateTime presentTime = LocalDateTime.now();
        this.metadata.setCreatedAt(presentTime);
        this.metadata.setModifiedAt(presentTime);
        this.entity = entity;
    }

    public ModeledObject() {
        this.metadata = new Metadata();
    }

    @Schema(
            description = "Metadata of the entity.",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public Metadata getMetadata() {
        return this.metadata;
    }

    @Schema(hidden = true)
    @JsonIgnore
    public String getArtifactType() {
        return this.metadata.getArtifactType();
    }

    @Schema(hidden = true)
    @JsonIgnore
    public String getId() {
        return this.metadata.getId();
    }

    public void setId(String value) {
        this.metadata.setId(value);
    }

    @Schema(hidden = true)
    @JsonIgnore
    public LocalDateTime getCreatedAt() {
        return this.metadata.getCreatedAt();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.metadata.setCreatedAt(createdAt);
    }

    @Schema(hidden = true)
    @JsonIgnore
    public String getModifiedBy() {
        return this.metadata.getModifiedBy();
    }

    public void setModifiedBy(String modifiedBy) {
        this.metadata.setModifiedBy(modifiedBy);
    }

    @Schema(hidden = true)
    @JsonIgnore
    public LocalDateTime getModifiedAt() {
        return this.metadata.getModifiedAt();
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.metadata.setModifiedAt(modifiedAt);
    }

    @Schema(hidden = true)
    @JsonIgnore
    public String getCreatedBy() {
        return this.metadata.getCreatedBy();
    }

    public void setCreatedBy(String createdBy) {
        this.metadata.setCreatedBy(createdBy);
    }

    @Schema(hidden = true)
    @JsonIgnore
    public String getName() {
        return this.metadata.getName();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getName() + "[metadata=");
        builder.append(this.metadata);
        builder.append(", entity=");
        builder.append(this.entity);
        builder.append("]");
        return builder.toString();
    }

    @JsonIgnore
    public FlatModeledObject getFlatModeledObject() {
        FlatModeledObject fo = new FlatModeledObject();
        fo.setId(this.metadata.getId());
        fo.setName(this.metadata.getName());
        fo.setVersionId(this.metadata.getVersionId());
        fo.setDescription(this.entity.getDescription());
        fo.setModified(this.metadata.getModifiedAt());
        return fo;
    }

    protected <T2> List<T2> getEmptyListIfNull(List<T2> list) {
        return (List)(list == null ? new ArrayList() : list);
    }

}
