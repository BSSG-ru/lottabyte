package ru.bssg.lottabyte.core.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableArtifactsEntity {
    @JsonProperty("artifact_id")
    private String artifactId;
    @JsonProperty("version_id")
    private String versionId;
    @JsonProperty("global_id")
    private String globalId;
    @JsonProperty("synonyms")
    private List<String> synonyms;
    @JsonProperty("synonym_global_ids")
    private List<String> synonym_global_ids;
    @JsonProperty("effective_start_date")
    private ZonedDateTime effectiveStartDate;
    @JsonProperty("effective_end_date")
    private ZonedDateTime effectiveEndDate;
}
