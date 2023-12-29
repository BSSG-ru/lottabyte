package ru.bssg.lottabyte.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.bssg.lottabyte.core.model.util.Utils;

import java.io.Serializable;

@Schema(
        name = "ErrorModel"
)
@Data
public class ErrorModel implements Serializable {

    @Schema(
            required = true,
            name = "code",
            description = "The error code."
    )
    private String code;

    @Schema(
            name = "id",
            description = "The error id."
    )
    private String id;

    @Schema(
            required = true,
            description = "The error message."
    )
    private String message;

    @Schema(
            name = "more_info",
            description = "The additional information of the error."
    )
    private String moreInfo;

    public ErrorModel() {
    }

    public ErrorModel(ErrorCode code, String message) {
        this.code = code.name();
        this.message = message;
    }

    public void setCode(ErrorCode code) {
        this.code = code.name();
    }
    public String toString() {
        return Utils.toString(this);
    }

}
