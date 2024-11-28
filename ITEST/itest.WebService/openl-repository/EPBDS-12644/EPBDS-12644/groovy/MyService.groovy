import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

import javax.ws.rs.POST
import javax.ws.rs.Path

abstract class MyService {

    abstract Integer parse(String num);

    @POST
    @Path("parse1")
    @Operation(summary = "Should add @ApiResponses when @Operation present and responses not added", tags = "Resource")
    abstract String parse1(String num);

    @POST
    @Path("parse2")
    @Operation(summary = "Should not add @ApiResponses when responses added in @Operation", tags = "Resource",
            responses = [@ApiResponse(responseCode = "200", description = "Response 200 added in @Operation"),
                    @ApiResponse(responseCode = "204", description = "Response 204 added in @Operation")])
    abstract String parse2(String num);

    @POST
    @Path("parse3")
    @Operation(summary = "Should not add @ApiResponses when @Operation present and @ApiResponses added on method", tags = "Resource")
    @ApiResponse(responseCode = "200", description = "Response 200 added in @ApiResponse")
    @ApiResponse(responseCode = "204", description = "Response 204 added in @ApiResponse")
    abstract String parse3(String num);

    @POST
    @Path("parse4")
    @Operation(summary = "Should not add @ApiResponses when @Operation present and @ApiResponse added on method", tags = "Resource")
    @ApiResponse(responseCode = "200", description = "Response 200 added in @ApiResponse")
    abstract String parse4(String num);

    @POST
    @Path("parse5")
    @ApiResponse(responseCode = "200", description = "Response 200 added in @ApiResponse")
    @ApiResponse(responseCode = "204", description = "Response 204 added in @ApiResponse")
    abstract String parse5(String num);

    @POST
    @Path("parse6")
    @ApiResponse(responseCode = "200", description = "Response 200 added in @ApiResponse")
    abstract String parse6(String num);
}
