package org.openl.rules.spring.openapi.app002.controller;

import java.util.List;

import org.openl.rules.spring.openapi.app002.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "user", description = "Operations about user")
public class UserController {

    /**
     * POST /user : Create user This can only be done by the logged in user.
     *
     * @param user Created user object (required)
     * @return successful operation (status code 200)
     */
    @Operation(summary = "Create user", description = "This can only be done by the logged in user.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation") })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Created user object", required = true, content = @Content(schema = @Schema(implementation = User.class)))
    @PostMapping(value = "/user", consumes = { "application/json" })
    public ResponseEntity<Void> createUser(@RequestBody User user) {
        return ResponseEntity.ok().build();
    }

    /**
     * POST /user/createWithArray : Creates list of users with given input array
     *
     * @param user List of user object (required)
     * @return successful operation (status code 200)
     */
    @Operation(summary = "Creates list of users with given input array", tags = { "user", })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation") })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of user object", required = true, content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    @PostMapping(value = "/user/createWithArray", consumes = { "application/json" })
    public ResponseEntity<Void> createUsersWithArrayInput(@RequestBody List<User> user) {
        return ResponseEntity.ok().build();
    }

    /**
     * POST /user/createWithList : Creates list of users with given input array
     *
     * @param user List of user object (required)
     * @return successful operation (status code 200)
     */
    @Operation(summary = "Creates list of users with given input array", tags = { "user", })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation") })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of user object", required = true, content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    @PostMapping(value = "/user/createWithList", consumes = { "application/json" })
    public ResponseEntity<Void> createUsersWithListInput(@RequestBody List<User> user) {
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /user/{username} : Delete user This can only be done by the logged in user.
     *
     * @param username The name that needs to be deleted (required)
     * @return Invalid username supplied (status code 400) or User not found (status code 404)
     */
    @Operation(summary = "Delete user", description = "This can only be done by the logged in user.", tags = {
            "user", })
    @ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Invalid username supplied"),
            @ApiResponse(responseCode = "404", description = "User not found") })
    @DeleteMapping(value = "/user/{username}")
    @Parameter(description = "The name that needs to be deleted", required = true, in = ParameterIn.PATH, name = "username", schema = @Schema(type = "string"))
    public ResponseEntity<Void> deleteUser(@PathVariable("username") String username) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /user/{username} : Get user by user name
     *
     * @param username The name that needs to be fetched. Use user1 for testing. (required)
     * @return successful operation (status code 200) or Invalid username supplied (status code 400) or User not found
     *         (status code 404)
     */
    @Operation(summary = "Get user by user name", tags = { "user", })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Invalid username supplied"),
            @ApiResponse(responseCode = "404", description = "User not found") })
    @Parameter(description = "The name that needs to be fetched. Use user1 for testing. ", in = ParameterIn.PATH, required = true, name = "username", schema = @Schema(type = "string"))
    @GetMapping(value = "/user/{username}", produces = { "application/json", "application/xml" })
    public ResponseEntity<User> getUserByName(@PathVariable("username") String username) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /user/login : Logs user into the system
     *
     * @param username The user name for login (required)
     * @param password The password for login in clear text (required)
     * @return successful operation (status code 200) or Invalid username/password supplied (status code 400)
     */
    @Operation(summary = "Logs user into the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(type = "string")), headers = {
                    @Header(description = "date in UTC when token expires", name = "X-Expires-After", schema = @Schema(format = "date-time", type = "string")),
                    @Header(description = "calls per hour allowed by the user", name = "X-Rate-Limit", schema = @Schema(format = "int32", type = "integer")) }),
            @ApiResponse(responseCode = "400", description = "Invalid username/password supplied") })
    @Parameters({
            @Parameter(description = "The user name for login", required = true, in = ParameterIn.QUERY, name = "username", schema = @Schema(type = "string")),
            @Parameter(description = "The password for login in clear text", required = true, in = ParameterIn.QUERY, name = "password", schema = @Schema(type = "string")) })
    @GetMapping(value = "/user/login", produces = { "application/json", "application/xml" })
    public ResponseEntity<String> loginUser(@RequestParam(value = "username") String username,
            @RequestParam(value = "password") String password) {
        return ResponseEntity.ok().build();
    }

    /**
     * GET /user/logout : Logs out current logged in user session
     *
     * @return successful operation (status code 200)
     */
    @Operation(summary = "Logs out current logged in user session")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation") })
    @GetMapping(value = "/user/logout")
    public ResponseEntity<Void> logoutUser() {
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /user/{username} : Updated user This can only be done by the logged in user.
     *
     * @param username name that need to be updated (required)
     * @param user Updated user object (required)
     * @return Invalid user supplied (status code 400) or User not found (status code 404)
     */
    @Operation(summary = "Updated user", description = "This can only be done by the logged in user.")
    @ApiResponses(value = { @ApiResponse(responseCode = "400", description = "Invalid user supplied"),
            @ApiResponse(responseCode = "404", description = "User not found") })
    @Parameter(description = "name that need to be updated", required = true, in = ParameterIn.PATH, name = "username", schema = @Schema(type = "string"))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user object", required = true, content = @Content(schema = @Schema(implementation = User.class)))
    @PutMapping(value = "/user/{username}", consumes = { "application/json" })
    public ResponseEntity<Void> updateUser(@PathVariable("username") String username, @RequestBody User user) {
        return ResponseEntity.ok().build();
    }

}
