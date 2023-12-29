package ru.bssg.lottabyte.core.ui.model.gojs;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GojsModelData {
    private List<GojsModelNodeData> nodes;
    private List<GojsModelLinkData> links;
}
