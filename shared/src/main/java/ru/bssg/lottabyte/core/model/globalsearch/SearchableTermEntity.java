package ru.bssg.lottabyte.core.model.globalsearch;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableTermEntity extends SearchableArtifactsEntity {

    private List<String> abbreviation;
    private List<String> synonyms;
    private List<String> synonymGlobalIds;

}

