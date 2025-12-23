package org.openl.rules.spring.openapi.app090;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller to verify automatic parameter name resolution from method signatures.
 * <p>
 * This controller intentionally does NOT specify parameter names in annotations
 * to test that the OpenAPI generator can automatically discover them from
 * method parameter names (when compiled with -parameters flag).
 */
@RestController
@RequestMapping(value = "/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tokens", description = "Token management operations")
public class TokenController {

    @GetMapping("/{tokenId}")
    @Operation(summary = "Get token by ID",
            description = "Retrieves a token by its ID. Parameter name should be auto-discovered as 'tokenId'.")
    @ApiResponse(responseCode = "200", description = "Token found")
    @ApiResponse(responseCode = "404", description = "Token not found")
    public TokenResponse getToken(@PathVariable String tokenId) {
        return new TokenResponse(tokenId, "test-token");
    }

    @DeleteMapping("/{tokenId}")
    @Operation(summary = "Delete token",
            description = "Deletes a token by its ID. Parameter name should be auto-discovered as 'tokenId'.")
    @ApiResponse(responseCode = "204", description = "Token deleted")
    @ApiResponse(responseCode = "404", description = "Token not found")
    public void deleteToken(@PathVariable String tokenId) {
        // Delete logic
    }

    @PostMapping
    @Operation(summary = "Create token",
            description = "Creates a new token")
    @ApiResponse(responseCode = "201", description = "Token created")
    public TokenResponse createToken(@RequestBody TokenRequest request) {
        return new TokenResponse("new-id", request.name());
    }

    @GetMapping("/search")
    @Operation(summary = "Search tokens",
            description = "Searches tokens by name. Parameter name should be auto-discovered as 'query'.")
    @ApiResponse(responseCode = "200", description = "Search results")
    public TokenResponse[] searchTokens(@RequestParam String query) {
        return new TokenResponse[]{new TokenResponse("id1", query)};
    }

    @GetMapping("/user/{userId}/tokens/{tokenId}")
    @Operation(summary = "Get user token",
            description = "Gets a specific token for a user. Both parameter names should be auto-discovered.")
    @ApiResponse(responseCode = "200", description = "Token found")
    public TokenResponse getUserToken(@PathVariable String userId, @PathVariable String tokenId) {
        return new TokenResponse(tokenId, "user-" + userId);
    }
}
