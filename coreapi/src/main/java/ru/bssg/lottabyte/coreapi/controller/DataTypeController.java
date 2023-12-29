package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.datatype.DataType;
import ru.bssg.lottabyte.core.model.datatype.FlatDataType;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.BusinessEntityService;
import ru.bssg.lottabyte.coreapi.service.DataTypeService;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Data Type", description = "APIs for Data Type.")
@RestController
@Slf4j
@RequestMapping("v1/datatype")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class DataTypeController {
    private final DataTypeService dataTypeService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets data type by given guid.",
            description = "This method can be used to get data type by given guid.",
            operationId = "getDataTypeById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "data type has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "data type with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{datatype_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<DataType> getDataTypeById(
            @Parameter(description = "Artifact ID of the data type",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("datatype_id") String businessEntityDataTypeId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(dataTypeService.getDataTypeById(businessEntityDataTypeId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataType>> searchDataType(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(dataTypeService.searchDataType(request, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }
}
