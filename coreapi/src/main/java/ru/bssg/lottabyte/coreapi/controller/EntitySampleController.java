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
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.entitySample.*;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.*;
import ru.bssg.lottabyte.coreapi.service.connector.GenericJDBCConnectorServiceImpl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Entity Samples", description = "APIs for Entity Samples")
@RestController
@Slf4j
@RequestMapping("v1/samples")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@RequiredArgsConstructor
public class EntitySampleController {
        private final EntitySampleService sampleService;
        private final EntitySampleBodyService entitySampleBodyService;
        private final EntityQueryService entityQueryService;
        private final EntityService entityService;
        private final SystemService systemService;
        private final ConnectorService connectorService;
        private final SystemConnectionService systemConnectionService;
        private final JwtHelper jwtHelper;

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets EntitySample by given guid.", description = "This method can be used to get EntitySample by given guid.", operationId = "get_sample_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "EntitySample has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "EntitySample with sample_id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<EntitySample> getSampleById(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_id") String sampleId,
                        @Parameter(description = "Get body of the Sample", example = "true") @RequestParam(value = "include_body", defaultValue = "true") Boolean includeBody,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(sampleService.getEntitySampleById(sampleId, includeBody, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "get_sample_body_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/body/{sample_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "sample_r", "sample_body_download" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<?> getSampleBodyById(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_id") String sampleId,
                        @RequestParam(value = "tenant_id", defaultValue = "999") Integer tenantId,
                        @RequestParam(value = "as_file", defaultValue = "false") Boolean asFile,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                /*
                 * String token =
                 * Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).
                 * replace("Bearer ","");
                 * UserDetails userDetails = jwtHelper.getUserDetail(token);
                 */
                UserDetails userDetails = new UserDetails();
                userDetails.setTenant(tenantId.toString());

                EntitySample sample = sampleService.getEntitySampleById(sampleId, false, userDetails);
                String body = entitySampleBodyService.getEntitySampleBodyFromS3ById(sampleId, userDetails);
                HttpHeaders responseHeaders = new HttpHeaders();
                if (!asFile) {
                        if (sample.getEntity().getSampleType().equals(EntitySampleType.xml)
                                        || sample.getEntity().getSampleType().equals(EntitySampleType.text) ||
                                        (sample.getEntity().getSampleType().equals(EntitySampleType.csv))) {
                                responseHeaders.setContentType(MediaType.TEXT_XML);
                        } else {
                                responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                        }
                } else {
                        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                        String filename = "sample_" + sampleId + ".";
                        switch (sample.getEntity().getSampleType()) {
                                case json:
                                case table:
                                        filename += "json";
                                        break;
                                case xml:
                                        filename += "xml";
                                        break;
                                case csv:
                                        filename += "csv";
                                        break;
                                case text:
                                        filename += "txt";
                                        break;
                        }
                        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                                        .filename(filename).build();
                        responseHeaders.setContentDisposition(contentDisposition);
                }
                return new ResponseEntity<>(body, responseHeaders, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid. The result is truncated by the number of lines specified in lines", description = "This method can be used to get Sample body by given guid. The result is truncated by the number of lines specified in lines", operationId = "get_sample_body_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/body_pretty/{sample_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<?> getSampleBodyPrettyById(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa4c563b-c18f-459b-a962-7551fb030df9") @PathVariable("sample_id") String sampleId,
                        @Parameter(description = "The result is truncated by the number of lines specified in lines.") @RequestParam(value = "lines", defaultValue = "100") Integer lines,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                return new ResponseEntity<>(
                                entitySampleBodyService.getSamplePrettyBodyById(sampleId, lines, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets all Samples.", description = "This method can be used to get all Samples.", operationId = "get_samples_paginated")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Samples have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/entity", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<EntitySample>> getAllSamplesOfEntityPaginated(
                        @Parameter(description = "Get Samples with bodies", example = "true") @RequestParam(value = "include_body", defaultValue = "true") Boolean includeBody,
                        @Parameter(description = "The maximum number of Samples to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(
                                sampleService.getEntitySampleWithPaging(offset, limit, includeBody, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Creates Sample.", description = "This method can be used to create Sample.", operationId = "create_sample")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample has been created successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySample> createSample(
                        @RequestBody UpdatableEntitySampleEntity newEntitySampleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                return new ResponseEntity<>(sampleService.createSample(newEntitySampleEntity, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Updates Sample body by given guid.", description = "This method can be used to update Sample body by given guid.", operationId = "update_sample_body")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/body/{sample_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySample> updateSampleBody(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_id") String sampleId,
                        @RequestBody String sampleBody,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(sampleService.updateSampleBody(sampleId, sampleBody, userDetails),
                                HttpStatus.OK);
        }

        @RequestMapping(value = "/body/{sample_id}/upload", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySample> uploadSampleBody(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_id") String sampleId,
                        @RequestParam("file") MultipartFile file,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                try {
                        return new ResponseEntity<>(sampleService.updateSampleBody(sampleId,
                                        new String(file.getBytes()), userDetails), HttpStatus.OK);
                } catch (IOException e) {
                        throw new LottabyteException(e.getMessage(), e);
                }
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Updates Sample by given guid.", description = "This method can be used to update Sample by given guid.", operationId = "update_sample")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySample> patchSample(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_id") String sampleId,
                        @RequestBody UpdatableEntitySampleEntity entitySampleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                return new ResponseEntity<>(sampleService.patchSample(sampleId, entitySampleEntity, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Deletes Sample by given guid.", description = "This method can be used to delete Sample by given guid.", operationId = "delete_sample")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}/{force}", method = RequestMethod.DELETE, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteSample(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_id") String sampleId,
                        @Parameter(description = "Filter for cascade removal", example = "true") @PathVariable("force") Boolean force,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(sampleService.deleteSample(sampleId, force, userDetails), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Deletes Sample by given guid.", description = "This method can be used to delete Sample by given guid.", operationId = "delete_sample")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/body/{sample_body_id}", method = RequestMethod.DELETE, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteEntitySampleBodyFromS3ById(
                        @Parameter(description = "Artifact ID of the Sample", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("sample_body_id") String sampleBodyId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(
                                entitySampleBodyService.deleteEntitySampleBodyFromS3ById(sampleBodyId, userDetails),
                                HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatEntitySample>> searchEntitySamples(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                SearchResponse<FlatEntitySample> res = sampleService.searchEntitySamples(request, userDetails);
                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/search_by_domain/{domain_id}", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatEntitySample>> searchEntitySamplesByDomain(
                        @RequestBody SearchRequestWithJoin request,
                        @PathVariable("domain_id") String domainId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                SearchResponse<FlatEntitySample> res = sampleService.searchEntitySamplesByDomain(request, domainId,
                                userDetails);
                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        // Sample properties

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "get_sample_property_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/properties/{property_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<EntitySampleProperty> getSamplePropertyById(
                        @PathVariable("property_id") String propertyId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                EntitySampleProperty esp = sampleService.getSamplePropertyById(propertyId, userDetails);
                return new ResponseEntity<>(esp, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "get_sample_properties_paginated")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}/properties", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<EntitySampleProperty>> getSamplesPropertiesPaginated(
                        @PathVariable("sample_id") String sampleId,
                        @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(
                                sampleService.getSamplesPropertiesPaginated(sampleId, limit, offset, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "get_sample_properties_search")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/properties/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatEntitySampleProperty>> searchSampleProperties(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatEntitySampleProperty> res = sampleService.searchSampleProperties(request,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "create_sample_property")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}/properties", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySampleProperty> createSampleProperty(
                        @PathVariable("sample_id") String sampleId,
                        @RequestBody UpdatableEntitySampleProperty newEntitySamplePropertyEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(sampleService.createSampleProperty(sampleId, newEntitySamplePropertyEntity,
                                userDetails), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "update_sample_property")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/properties/{property_id}", method = RequestMethod.PATCH, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySampleProperty> patchSampleProperty(
                        @PathVariable("property_id") String propertyId,
                        @RequestBody UpdatableEntitySampleProperty entitySamplePropertyEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(
                                sampleService.updateSampleProperty(propertyId, entitySamplePropertyEntity, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Sample body by given guid.", description = "This method can be used to get Sample body by given guid.", operationId = "delete_sample_property")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/properties/{property_id}/{force}", method = RequestMethod.DELETE, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteSampleProperty(
                        @PathVariable("property_id") String propertyId,
                        @PathVariable("force") Boolean force,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                ArchiveResponse resp = new ArchiveResponse();
                sampleService.deleteSampleProperty(propertyId, force, userDetails);
                resp.setDeletedGuids(Collections.singletonList(propertyId));
                return new ResponseEntity<>(resp, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/testCS")
        public String testCS() throws LottabyteException, SQLException, ClassNotFoundException {
                GenericJDBCConnectorServiceImpl cs = new GenericJDBCConnectorServiceImpl();

                String res = "no";

                UserDetails userDetails = new UserDetails();
                userDetails.setTenant("999");
                userDetails.setLanguage(Language.ru);

                EntityQuery eq = entityQueryService.getEntityQueryById("6f9941d9-e6fb-4f3e-8b09-42476bbf77a1",
                                userDetails);
                DataEntity e = entityService.getDataEntityById(eq.getEntity().getEntityId(), userDetails);
                for (String sysid : e.getEntity().getSystemIds()) {
                        System sys = systemService.getSystemById(sysid, userDetails);
                        Connector connector = connectorService.getConnectorById(sys.getEntity().getConnectorId(),
                                        userDetails);
                        PaginatedArtifactList<ConnectorParam> pal = connectorService.getConnectorParamsPaginated(
                                        sys.getEntity().getConnectorId(), 0, 999999, userDetails);

                        List<SystemConnection> scList = systemConnectionService.getSystemConnectionsBySystemId(sysid,
                                        userDetails);
                        for (SystemConnection sc : scList) {

                                PaginatedArtifactList<SystemConnectionParam> pal2 = systemConnectionService
                                                .getSystemConnectionParamsPaginated(sc.getId(), 0, 99999, userDetails);

                                EntityQueryResult eqr = cs.querySystem(connector, pal.getResources(), sys, e, eq, sc,
                                                pal2.getResources(), userDetails);
                                res = eqr.getTextSampleBody();
                        }
                }

                return res;
        }

        @Hidden
        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get EntitySample versions.", description = "This method can be used to read EntitySample versions.", operationId = "get_entity_sample_versions")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "EntityQuery versions have been read successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "EntityQuery with query_id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        @RequestMapping(value = "/{sample_id}/versions", method = RequestMethod.GET, produces = { "application/json" })
        public ResponseEntity<PaginatedArtifactList<EntitySample>> getEntitySampleVersions(
                        @PathVariable("sample_id") String sampleId,
                        @Parameter(description = "The maximum number of Entity Sample versions to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                PaginatedArtifactList<EntitySample> list = sampleService.getEntitySampleVersions(sampleId, offset,
                                limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(list, HttpStatus.OK);
        }

        @RequestMapping(value = "/dq_rules/{dq_rules_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<EntitySampleDQRule> getSampleDQRuleById(
                        @PathVariable("dq_rules_id") String dqRuleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                EntitySampleDQRule esp = sampleService.getSampleDQRuleById(dqRuleId, userDetails);
                return new ResponseEntity<>(esp, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Search dq rules.", description = ".", operationId = "searchSampleDQRules")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/dq_rules/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatEntitySampleDQRule>> searchSampleDQRules(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatEntitySampleDQRule> res = sampleService.searchSampleDQRules(request,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Search dq rules.", description = ".", operationId = "get_sample_properties_search")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}/dq_rules", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "sample_r" }, level = ANY_ROLE)
        public ResponseEntity<List<EntitySampleDQRule>> getSampleDQRules(
                        @PathVariable("sample_id") String sampleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<EntitySampleDQRule> res = sampleService.getSampleDQRules(sampleId,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create dq rule by given guid.", description = "This method can be used to create dq rule by given guid.", operationId = "createSampleDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{sample_id}/dq_rules", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySampleDQRule> createSampleDQRule(
                        @PathVariable("sample_id") String sampleId,
                        @RequestBody UpdatableEntitySampleDQRule newEntitySampleDQRuleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(sampleService.createSampleDQRule(sampleId, newEntitySampleDQRuleEntity,
                                userDetails), HttpStatus.OK);
        }

    
        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Update dq rule by given guid.", description = "This method can be used to update dq rule by given guid.", operationId = "patchSampleDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/dq_rules/{dq_rule_id}", method = RequestMethod.PATCH, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySampleDQRule> patchSampleDQRule(
                        @PathVariable("dq_rule_id") String dqRuleId,
                        @RequestBody UpdatableEntitySampleDQRule entitySampleDQRuleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(
                                sampleService.updateSampleDQRule(dqRuleId, entitySampleDQRuleEntity, userDetails),
                                HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Delete dq rule by given guid.", description = "This method can be used to delete dq rule by given guid.", operationId = "deleteSampleDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/dq_rules/{dq_rule_id}", method = RequestMethod.DELETE, produces = {
                        "application/json" })
        @Secured(roles = { "sample_r", "sample_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteSampleDQRule(
                        @PathVariable("dq_rule_id") String dqRuleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                ArchiveResponse resp = new ArchiveResponse();
                sampleService.deleteSampleDQRuleBy(dqRuleId, true, userDetails);
                resp.setDeletedGuids(Collections.singletonList(dqRuleId));
                return new ResponseEntity<>(resp, HttpStatus.OK);
        }

}
