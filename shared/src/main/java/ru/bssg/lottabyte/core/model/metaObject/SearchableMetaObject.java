package ru.bssg.lottabyte.core.model.metaObject;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableRelatedArtifact;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableMetaObject extends SearchableArtifact {
    private String metaDatabaseId;
    private String parentId;
    private String metaObjectType;

    @Builder
    public SearchableMetaObject(String id, Integer versionId, String name, String description, String parentId, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, String metaDatabaseId, String metaObjectType, List<SearchableRelatedArtifact> relatedArtifacts) {
        super(id, versionId, name, description, "", tags, modifiedBy, modifiedAt, ArtifactType.meta_object.getText(), "PUBLISHED", effectiveStartDate, effectiveEndDate, new ArrayList<>(), new ArrayList<>(), relatedArtifacts);
        this.setMetaDatabaseId(metaDatabaseId);
        this.setParentId(parentId);
        this.setMetaObjectType(metaObjectType);
    }
}
