package ru.bssg.lottabyte.core.model.globalsearch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class GsiCategory {
    private long lastUpdatedAt;
    private String primaryCategoryId;
    private String primaryCategoryGlobalId;
    private String primaryCategoryName;
    private List<String> secondaryCategoryIds;
    private List<String> secondaryCategoryGlobalIds;
    private List<String> secondaryCategoryNames;
}
