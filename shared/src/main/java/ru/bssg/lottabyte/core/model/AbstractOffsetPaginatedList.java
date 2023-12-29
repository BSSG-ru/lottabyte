package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.URI;

@Schema(
        description = "Base class for paginated results"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public abstract class AbstractOffsetPaginatedList<T> extends AbstractPaginatedList<T> {

    @Schema(
            example = "80",
            description = "Offset used in the list / search method.",
            required = true
    )
    private int offset;

    private Link next;
    private Link prev;
    @Schema(
            hidden = true
    )
    @JsonIgnore
    private boolean setUri = false;

    protected void setSetUri(boolean setUri) {
        this.setUri = setUri;
    }
}
