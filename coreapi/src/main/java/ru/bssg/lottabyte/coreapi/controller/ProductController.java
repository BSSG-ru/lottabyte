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
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.product.*;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.EntityService;
import ru.bssg.lottabyte.coreapi.service.ProductService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "product", description = "APIs for products.")
@RestController
@Slf4j
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@RequestMapping("v1/product")
@RequiredArgsConstructor
public class ProductController {
        private final ProductService productService;
        private final JwtHelper jwtHelper;

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Product by given guid.", description = "This method can be used to get Product by given guid.", operationId = "get_product_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Product with product_id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<Product> getProductById(
                        @Parameter(description = "Artifact ID of the Product", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("product_id") String productId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(productService.getProductById(productId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets all Product.", description = "This method can be used to get all Product.", operationId = "get_products_paginated")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Product not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<Product>> getAllProductsPaginated(
                        @Parameter(description = "The maximum number of Products to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @Parameter(description = "Artifact state.") @RequestParam(value = "state", defaultValue = "PUBLISHED") String artifactState,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(productService.getAllProductsPaginated(offset, limit, artifactState,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Product versions list by domain guid.", description = "This method can be used to get Product history versions by given guid.", operationId = "getProductVersionsById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product versions have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}/versions", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<Product>> getProductVersionsById(
                        @PathVariable("product_id") String productId,
                        @Parameter(description = "The maximum number of Products to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                PaginatedArtifactList<Product> list = productService.getProductVersions(
                                productId, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(list, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Product version by given guid and version id.", description = "This method can be used to get Product history version by given guid and version id.", operationId = "getProductVersionById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product version has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Product version not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}/versions/{version_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<Product> getProductVersionById(
                        @Parameter(description = "ID of the Product", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("product_id") String productId,
                        @Parameter(description = "Version ID of the Product", example = "1") @PathVariable("version_id") Integer versionId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return ResponseEntity.ok(productService.getProductVersionById(productId, versionId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Creates Product.", description = "This method can be used to creating Product.", operationId = "create_product")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product has been added successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "product_r", "product_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<Product> createProduct(@RequestBody UpdatableProductEntity newProductEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return new ResponseEntity<>(productService.createProduct(newProductEntity,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Update Product.", description = "This method can be used to updating Product.", operationId = "update_product")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "product_r", "product_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<Product> patchProduct(
                        @Parameter(description = "Artifact ID of the Entity", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("product_id") String productId,
                        @RequestBody UpdatableProductEntity productEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return new ResponseEntity<>(productService.updateProduct(productId, productEntity,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Delete Product.", description = "This method can be used to deleting Product.", operationId = "delete_product")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Product with product_id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}", method = RequestMethod.DELETE, produces = { "application/json" })
        @Secured(roles = { "product_r", "product_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<?> deleteProduct(
                        @Parameter(description = "Artifact ID of the Product", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("product_id") String productId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                Product result = productService.deleteProductById(productId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                if (result == null) {
                        ArchiveResponse resp = new ArchiveResponse();
                        resp.setDeletedGuids(Collections.singletonList(productId));
                        return ResponseEntity.ok(resp);
                } else {
                        return ResponseEntity.ok(result);
                }
        }

        @Hidden
        @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatProduct>> searchProducts(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatProduct> res = productService.searchProducts(request, userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/type/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatProductType>> searchProductTypes(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatProductType> res = productService.searchProductTypes(request, userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/supply_variant/search", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatProductSupplyVariant>> searchProductSupplyVariants(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatProductSupplyVariant> res = productService.searchProductSupplyVariants(request,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Product Type by given guid.", description = "This method can be used to get Product Type by given guid.", operationId = "get_product_type_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product type has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Product type with product_id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/type/{product_type_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<ProductType> getProductTypeById(
                        @Parameter(description = "Artifact ID of the Product Type", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("product_type_id") String productTypeId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(productService.getProductTypeById(productTypeId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Product Supply Variant by given guid.", description = "This method can be used to get Product Supply Variant by given guid.", operationId = "get_product_supply_variant_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product Supply Variant has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Product type with product_id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/supply_variant/{product_supply_variant_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<ProductSupplyVariant> getProductSupplyVariantById(
                        @Parameter(description = "Artifact ID of the Product Supply Variant", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("product_supply_variant_id") String productSupplyVariantId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(productService.getProductSupplyVariantById(productSupplyVariantId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Search dq rules.", description = ".", operationId = "getDQRules")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}/dq_rules", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "product_r" }, level = ANY_ROLE)
        public ResponseEntity<List<EntitySampleDQRule>> getDQRules(
                        @PathVariable("product_id") String productId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<EntitySampleDQRule> res = productService.getDQRules(productId,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create dq rule by given guid.", description = "This method can be used to create dq rule by given guid.", operationId = "createDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{product_id}/dq_rules", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "product_r", "product_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySampleDQRule> createDQRule(
                        @PathVariable("product_id") String productId,
                        @RequestBody UpdatableEntitySampleDQRule newEntitySampleDQRuleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(productService.createDQRule(productId, newEntitySampleDQRuleEntity,
                                userDetails), HttpStatus.OK);
        }
}
