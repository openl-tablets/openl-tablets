package org.openl.rules.spring.openapi.app020.controller;

import java.util.List;

import org.openl.rules.spring.openapi.app020.model.ApiResponse;
import org.openl.rules.spring.openapi.app020.model.Pet;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "pet", description = "Everything about your Pets")
@RequestMapping("/pet")
public class PetController {

    /**
     * POST /pet : Add a new pet to the store
     *
     * @param pet Pet object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     */
    @Operation(summary = "Add a new pet to the store", tags = { "pet" })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", description = "Invalid input") })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pet object that needs to be added to the store", required = true, content = @Content(schema = @Schema(implementation = Pet.class)))
    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<?> addPet(@RequestBody Pet pet) {
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /pet/{petId} : Deletes a pet
     *
     * @param petId Pet id to delete (required)
     * @param apiKey (optional)
     * @return Invalid ID supplied (status code 400) or Pet not found (status code 404)
     */
    @Operation(summary = "Deletes a pet", tags = { "pet" })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, description = "Pet id to delete", name = "petId", required = true, schema = @Schema(type = "integer", format = "int64")),
            @Parameter(in = ParameterIn.HEADER, name = "api_key", schema = @Schema(type = "string")) })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pet not found") })
    @DeleteMapping(value = "/{petId}")
    public ResponseEntity<?> deletePet(@PathVariable("petId") Long petId,
            @RequestHeader(value = "api_key", required = false) String apiKey) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /pet/findByStatus : Finds Pets by status Multiple status values can be provided with comma separated strings
     *
     * @param status Status values that need to be considered for filter (required)
     * @return successful operation (status code 200) or Invalid status value (status code 400)
     */
    @Operation(summary = "Finds Pets by status", description = "Multiple status values can be provided with comma separated strings", tags = {
            "pet" })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Pet.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value") })
    @GetMapping(value = "/findByStatus", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE })
    @Parameter(description = "Status values that need to be considered for filter", required = true, in = ParameterIn.QUERY, name = "status", array = @ArraySchema(schema = @Schema(type = "string", defaultValue = "available", allowableValues = {
            "available",
            "pending",
            "sold" })))
    public ResponseEntity<List<Pet>> findPetsByStatus(@RequestParam(value = "status") List<String> status) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /pet/findByTags : Finds Pets by tags Multiple tags can be provided with comma separated strings. Use tag1,
     * tag2, tag3 for testing.
     *
     * @param tags Tags to filter by (required)
     * @return successful operation (status code 200) or Invalid tag value (status code 400)
     * @deprecated
     */
    @Operation(summary = "Finds Pets by tags", description = "Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.", deprecated = true)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Pet.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid tag value") })
    @GetMapping(value = "/findByTags", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    @Parameter(description = "Tags to filter by", in = ParameterIn.QUERY, name = "tags", required = true, array = @ArraySchema(schema = @Schema(type = "string")))
    public ResponseEntity<List<Pet>> findPetsByTags(@RequestParam(value = "tags") List<String> tags) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /pet/{petId} : Find pet by ID Returns a single pet
     *
     * @param petId ID of pet to return (required)
     * @return successful operation (status code 200) or Invalid ID supplied (status code 400) or Pet not found (status
     *         code 404)
     */
    @Operation(summary = "Find pet by ID", description = "Returns a single pet", tags = { "pet" })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = Pet.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pet not found") })
    @Parameter(description = "ID of pet to return", in = ParameterIn.PATH, name = "petId", required = true, schema = @Schema(type = "integer", format = "int64"))
    @GetMapping(value = "/{petId}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<Pet> getPetById(@PathVariable("petId") Long petId) {
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /pet : Update an existing pet
     *
     * @param pet Pet object that needs to be added to the store (required)
     * @return Invalid ID supplied (status code 400) or Pet not found (status code 404) or Validation exception (status
     *         code 405)
     */
    @Operation(summary = "Update an existing pet", tags = { "pet" })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pet not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", description = "Validation exception") })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pet object that needs to be added to the store", required = true, content = @Content(schema = @Schema(implementation = Pet.class)))
    @PutMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<Void> updatePet(@RequestBody Pet pet) {
        return ResponseEntity.ok().build();
    }

    /**
     * POST /pet/{petId} : Updates a pet in the store with form data
     *
     * @param petId ID of pet that needs to be updated (required)
     * @param name Updated name of the pet (optional)
     * @param status Updated status of the pet (optional)
     * @return Invalid input (status code 405)
     */
    /*
     * @Operation(summary = "Updates a pet in the store with form data", tags={ "pet"})
     * 
     * @ApiResponses(value = {
     * 
     * @ApiResponse(responseCode = "405", description = "Invalid input") })
     * 
     * @Parameter(description = "ID of pet that needs to be updated", in = ParameterIn.PATH, name = "petId", required =
     * true, schema = @Schema(type = "integer", format = "int64"))
     * 
     * @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType =
     * "application/x-www-form-urlencoded", schema = @Schema()))
     * 
     * @PostMapping( value = "/{petId}", consumes = { "application/x-www-form-urlencoded" } ) public
     * ResponseEntity<Void> updatePetWithForm(@PathVariable("petId") Long petId,@ApiParam(value =
     * "Updated name of the pet") @RequestPart(value = "name", required = false) String name,@ApiParam(value =
     * "Updated status of the pet") @RequestPart(value = "status", required = false) String status) { return
     * ResponseEntity.ok().build(); }
     */

    /**
     * POST /pet/{petId}/uploadImage : uploads an image
     *
     * @param petId ID of pet to update (required)
     * @param body (optional)
     * @return successful operation (status code 200)
     */
    @Operation(summary = "uploads an image", tags = { "pet" })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = ApiResponse.class))) })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/octet-stream", schema = @Schema(format = "binary", type = "string")))
    @PostMapping(value = "/{petId}/uploadImage", produces = { "application/json" }, consumes = {
            "application/octet-stream" })
    @Parameter(description = "ID of pet to update", required = true, in = ParameterIn.PATH, name = "petId", schema = @Schema(type = "integer", format = "int64"))
    public ResponseEntity<ApiResponse> uploadFile(@PathVariable("petId") Long petId,
            @RequestBody(required = false) org.springframework.core.io.Resource body) {
        return ResponseEntity.ok().build();
    }

}
