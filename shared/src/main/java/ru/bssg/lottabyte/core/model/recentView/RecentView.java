package ru.bssg.lottabyte.core.model.recentView;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.sql.Timestamp;

@Schema(
        description = "Update data asset object"
)
@Data
public class RecentView {

    private String id;
    private String userId;
    private String name;
    private String artifactId;
    private String artifactType;
    private Timestamp viewedTime;

}
