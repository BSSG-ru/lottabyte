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
    private Integer weight;
    private String artifactType;
}
