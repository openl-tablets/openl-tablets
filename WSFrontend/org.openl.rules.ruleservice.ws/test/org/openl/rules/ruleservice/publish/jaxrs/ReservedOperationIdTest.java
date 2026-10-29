package org.openl.rules.ruleservice.publish.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;

import org.openl.classloader.OpenLClassLoader;
import org.openl.rules.openapi.OpenAPIConfiguration;

/**
 * A method named as a member of a JavaScript object gets an indexed operationId: Swagger UI and RapiDoc cannot load a
 * schema with such an operationId. Its path keeps the name of the method.
 */
class ReservedOperationIdTest {

    @SuppressWarnings("unused")
    public interface Service {
        String toString(String a);

        String valueOf(String a);

        String __proto__(String a);

        String hello(String a);
    }

    /** An interface annotated before the enhancement, as an annotation template is. */
    @SuppressWarnings("unused")
    public interface AnnotatedService {
        @POST
        @Path("/toString")
        @Operation(summary = "Named by the method")
        String toString(String a);

        @POST
        @Path("/declared")
        @Operation(operationId = "valueOf")
        String declared(String a);

        @POST
        @Path("/again")
        @Operation(operationId = "toString")
        String again(String a);

        @POST
        @Path("/hello")
        @Operation(operationId = "hello")
        String hello(String a);
    }

    @Test
    void aReservedNameGetsAnIndex() throws Exception {
        try (var classLoader = new OpenLClassLoader(getClass().getClassLoader())) {
            var enhanced = JAXRSOpenLServiceEnhancerHelper.enhanceInterface(Service.class,
                    new Object(),
                    classLoader,
                    null,
                    false);

            assertOperation(enhanced, "toString", "toString_1");
            assertOperation(enhanced, "valueOf", "valueOf_1");
            assertOperation(enhanced, "__proto__", "__proto___1");
            assertOperation(enhanced, "hello", "hello");
        }
    }

    @Test
    void anAnnotatedOperationGetsAnIndex() {
        var paths = OpenAPIConfiguration.generateOpenAPI(AnnotatedService.class, new ObjectMapper()).getPaths();

        assertEquals(Set.of("toString_1", "toString_2"),
                Set.of(paths.get("/toString").getPost().getOperationId(),
                        paths.get("/again").getPost().getOperationId()));
        assertEquals("valueOf_1", paths.get("/declared").getPost().getOperationId());
        assertEquals("hello", paths.get("/hello").getPost().getOperationId());
    }

    private static void assertOperation(Class<?> enhanced, String method, String operationId) throws Exception {
        var enhancedMethod = enhanced.getMethod(method, String.class);
        assertEquals(operationId, enhancedMethod.getAnnotation(Operation.class).operationId());
        assertEquals("/" + method, enhancedMethod.getAnnotation(Path.class).value());
    }
}
