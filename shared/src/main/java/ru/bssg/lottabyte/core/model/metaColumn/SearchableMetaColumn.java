package ru.bssg.lottabyte.core.model.metaColumn;

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
public class SearchableMetaColumn extends SearchableArtifact {
    private String metaDatabaseId;
    private String metaObjectId;

    @Builder
    public SearchableMetaColumn(String id, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, String metaDatabaseId, String metaObjectId, List<SearchableRelatedArtifact> relatedArtifacts) {
        super(id, 0, name, description, "", tags, modifiedBy, modifiedAt, ArtifactType.meta_column.getText(), "PUBLISHED", effectiveStartDate, effectiveEndDate, new ArrayList<>(), new ArrayList<>(), relatedArtifacts);
        this.setMetaDatabaseId(metaDatabaseId);
        this.setMetaObjectId(metaObjectId);
    }
}
