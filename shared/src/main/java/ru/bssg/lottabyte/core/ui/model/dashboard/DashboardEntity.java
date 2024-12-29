package ru.bssg.lottabyte.core.ui.model.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEntity {
    private String id;
    private String name;
    private String description;
    private Integer rating;
    private Integer weight;
    private String artifactType;
    private String createdBy;
    private Boolean isInFav;
}
