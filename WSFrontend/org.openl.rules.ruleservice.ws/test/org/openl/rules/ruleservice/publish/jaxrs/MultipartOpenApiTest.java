package org.openl.rules.ruleservice.publish.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.junit.jupiter.api.Test;

import org.openl.rules.openapi.OpenAPIConfiguration;

/**
 * The parts of a multipart request are the properties of its body.
 *
 * @author Yury Molchan
 */
class MultipartOpenApiTest {

    /** An annotation template of a method reading a multipart request, as a service declares it. */
    @SuppressWarnings("unused")
    public interface ExperienceService {
        @POST
        @Path("/experience-data/import")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String importData(@Multipart("experienceData") String experienceData,
                          @Multipart(value = "file", type = "text/csv", required = false) InputStream source);
    }

    /** A method reading a part without a name next to a named one. */
    @SuppressWarnings("unused")
    public interface UnnamedPartService {
        @POST
        @Path("/import")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String importData(@Multipart String root, @Multipart("file") InputStream source);
    }

    @Test
    void listsThePartsOfAMultipartRequest() {
        var operation = OpenAPIConfiguration.generateOpenAPI(ExperienceService.class, new ObjectMapper())
                .getPaths()
                .get("/experience-data/import")
                .getPost();

        var schema = operation.getRequestBody().getContent().get(MediaType.MULTIPART_FORM_DATA).getSchema();
        assertEquals("object", schema.getType());
        assertEquals(Set.of("experienceData", "file"), schema.getProperties().keySet());
        var data = (Schema<?>) schema.getProperties().get("experienceData");
        assertEquals("string", data.getType());
        var file = (Schema<?>) schema.getProperties().get("file");
        assertEquals("string", file.getType());
        assertEquals("binary", file.getFormat());
        assertEquals(List.of("experienceData"), schema.getRequired());
    }

    /** A part without a name is left out, and the named parts still make up the body. */
    @Test
    void leavesOutAPartWithoutAName() {
        var operation = OpenAPIConfiguration.generateOpenAPI(UnnamedPartService.class, new ObjectMapper())
                .getPaths()
                .get("/import")
                .getPost();

        var schema = operation.getRequestBody().getContent().get(MediaType.MULTIPART_FORM_DATA).getSchema();
        assertEquals(Set.of("file"), schema.getProperties().keySet());
        assertEquals("binary", ((Schema<?>) schema.getProperties().get("file")).getFormat());
        assertEquals(List.of("file"), schema.getRequired());
    }
}
