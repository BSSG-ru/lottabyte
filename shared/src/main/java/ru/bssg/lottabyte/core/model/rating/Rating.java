package ru.bssg.lottabyte.core.model.rating;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(
        description = "Update data asset object"
)
@Data
public class Rating {

    private String artifactId;
    private String artifactName;
    private String artifactType;
    private Double rating;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer totalRates;

}
