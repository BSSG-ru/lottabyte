package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElasticSearch {

    @JsonProperty("size")
    public int size;

    @JsonProperty("rows")
    public List<SearchableArtifact> rows;
}