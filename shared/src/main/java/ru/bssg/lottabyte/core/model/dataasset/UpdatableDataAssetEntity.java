package ru.bssg.lottabyte.core.model.dataasset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update data asset object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableDataAssetEntity extends DataAssetEntity {

    public UpdatableDataAssetEntity(DataAssetEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setSystemId(fromCopy.getSystemId());
        this.setDomainId(fromCopy.getDomainId());
        this.setDataSize(fromCopy.getDataSize());
        this.setRowsCount(fromCopy.getRowsCount());
        this.setHasQuery(fromCopy.getHasQuery());
        this.setHasSample(fromCopy.getHasSample());
        this.setHasStatistics(fromCopy.getHasStatistics());
    }

    @Override
    @JsonIgnore
    public void setHasQuery(Boolean hasQuery) {
        super.setHasQuery(hasQuery);
    }

    @Override
    @JsonIgnore
    public void setHasSample(Boolean hasSample) {
        super.setHasSample(hasSample);
    }

    @Override
    @JsonIgnore
    public void setHasStatistics(Boolean hasStatistics) {
        super.setHasStatistics(hasStatistics);
    }
}
