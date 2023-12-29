package ru.bssg.lottabyte.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.bssg.lottabyte.core.model.util.Utils;

import java.io.Serializable;
import java.util.List;

@Schema(
        name = "ErrorContainer",
        description = "error Container"
)
@Data
public class ErrorContainer implements Serializable {

    @Schema(
            required = true,
            description = "The trace id."
    )
    private String trace;

    @Schema(
            required = true,
            description = "List of errors."
    )
    private List<ErrorModel> errors;

    public ErrorContainer() {
    }

    public ErrorContainer(String trace, List<ErrorModel> errors) {
        this.trace = trace;
        this.errors = errors;
    }

    public String toString() {
        return Utils.toString(this);
    }

}
