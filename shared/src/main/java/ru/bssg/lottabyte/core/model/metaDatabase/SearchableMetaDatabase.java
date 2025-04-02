package ru.bssg.lottabyte.core.model.metaDatabase;

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
public class SearchableMetaDatabase extends SearchableArtifact {

    @Builder
    public SearchableMetaDatabase(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableRelatedArtifact> relatedArtifacts) {
        super(id, versionId, name, description, shortDescription, tags, modifiedBy, modifiedAt, ArtifactType.meta_database.getText(), "PUBLISHED", effectiveStartDate, effectiveEndDate, new ArrayList<>(), new ArrayList<>(), relatedArtifacts);
    }
}
