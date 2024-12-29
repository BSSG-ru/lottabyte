package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.tag.Tag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "ObjectMetadata")
@NoArgsConstructor
@Data
public class Metadata {

    private String artifactType;
    private String id;
    private String name;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private Integer versionId;
    private Integer historyId;
    private LocalDateTime effectiveStartDate;
    private LocalDateTime effectiveEndDate;
    private List<Relation> tags;
    private String modifierDisplayName;
    private String modifierDescription;
    private String modifierEmail;

    public Metadata(ResultSet rs, ArtifactType artifactType) throws SQLException {
        this.setId(rs.getString("id"));
        this.setCreatedBy(rs.getString("creator"));
        this.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
        this.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
        this.setModifiedBy(rs.getString("modifier"));
        this.setName(rs.getString("name"));
        this.setArtifactType(artifactType.toString());
        try{
            this.setHistoryId(rs.getInt("history_id"));
        }catch(SQLException ignored){}
        try{
            this.setVersionId(rs.getInt("version_id"));
        }catch(SQLException ignored){}

        try {
            if (rs.getTimestamp("history_start") != null)
                this.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
        } catch (SQLException ignored) {}
        try {
            if (rs.getTimestamp("history_end") != null)
                this.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());
        } catch (SQLException ignored) {}
        try {
            this.setModifierDisplayName(rs.getString("modifier_display_name"));
            this.setModifierDescription(rs.getString("modifier_description"));
            this.setModifierEmail(rs.getString("modifier_email"));
        } catch (SQLException ignored) {}
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags.stream()
                .map(x -> new Relation(x.getId(), x.getName())).collect(Collectors.toList());
    }

}
