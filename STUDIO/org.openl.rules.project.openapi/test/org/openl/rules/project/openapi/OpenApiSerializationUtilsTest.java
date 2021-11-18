package org.openl.rules.project.openapi;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.junit.Before;
import org.junit.Test;
import org.openl.util.IOUtils;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

public class OpenApiSerializationUtilsTest {

    private Reader reader;

    @Before
    public void setUp() {
        reader = new Reader();
    }

    @Test
    public void testToJson() throws IOException {
        final String actualJsonApi = OpenApiSerializationUtils.toJson(reader.read(Service.class));
        final String expectedJson = readText("api_serialization/openapi.json");
        assertEquals(expectedJson, actualJsonApi);
    }

    @Test
    public void testToYaml() throws IOException {
        final String actualYamlApi = OpenApiSerializationUtils.toYaml(reader.read(Service.class));
        final String expectedYaml = readText("api_serialization/openapi.yaml");
        assertEquals(expectedYaml, actualYamlApi);
    }

    private static String readText(String file) throws IOException {
        return IOUtils.toStringAndClose(OpenApiSerializationUtilsTest.class.getResourceAsStream("/" + file));
    }

    @Path("books")
    @OpenAPIDefinition(tags = { @Tag(name = "book", description = "Operations about book"),
            @Tag(name = "foo", description = "Some other tag") }, info = @Info(title = "Book API", version = "1.0.0", description = "Some description", contact = @Contact(name = "John Doe", email = "jdoe@localhost"), license = @License(url = "http://www.apache.org/licenses/LICENSE-2.0.html", name = "Apache 2.0")))
    private interface Service {

        @Operation(summary = "Get a book by its id")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Found the book", content = {
                        @Content(mediaType = "application/json", schema = @Schema(implementation = Dto.class)) }),
                @ApiResponse(responseCode = "400", description = "Invalid id supplied", content = @Content),
                @ApiResponse(responseCode = "404", description = "Book not found", content = @Content) })
        @GET
        @Path("{id}")
        Dto findById(@Parameter(description = "id of book to be searched") @PathParam("id") long id);

    }

    private static class Dto {

        public String field1;
        public Double field2;
        public Date field3;

    }

}
