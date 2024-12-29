package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import ru.bssg.lottabyte.core.model.ArtifactType;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class ArtifactsRelation {
    public enum ArtifactsRelationType { ForeignKey, CrossTable, CustomSQL };

    private ArtifactsRelationType relationType;
    private ArtifactType artifact1Type;
    private ArtifactType artifact2Type;
    private String artifact1Column;
    private String artifact2Column;
    private String crossTableName;
    private String joinSQL;
    private String whereSQL;

    public ArtifactsRelation(ArtifactsRelationType relationType, ArtifactType artifact1Type, ArtifactType artifact2Type, String artifact1Column, String artifact2Column, String crossTableName) {
        this.relationType = relationType;
        this.artifact1Type = artifact1Type;
        this.artifact2Type = artifact2Type;
        this.artifact1Column = artifact1Column;
        this.artifact2Column = artifact2Column;
        this.crossTableName = crossTableName;
    }

    public ArtifactsRelation(ArtifactsRelationType relationType, ArtifactType artifact1Type, ArtifactType artifact2Type, String joinSQL, String whereSQL) {
        this.relationType = relationType;
        this.artifact1Type = artifact1Type;
        this.artifact2Type = artifact2Type;
        this.joinSQL = joinSQL;
        this.whereSQL = whereSQL;
    }
}
