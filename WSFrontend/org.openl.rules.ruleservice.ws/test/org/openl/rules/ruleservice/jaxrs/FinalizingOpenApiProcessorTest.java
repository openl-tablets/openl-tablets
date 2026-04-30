package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.openl.rules.openapi.OpenAPIConfiguration;

class FinalizingOpenApiProcessorTest {

    private static final class SampleApp {
    }

    @Test
    void delegatesToOpenApiConfigurationGenerateOpenAPI() {
        var input = new OpenAPI().info(new Info().title("svc"));
        var generated = new OpenAPI().info(new Info().title("generated"));
        var mapper = new ObjectMapper();

        try (MockedStatic<OpenAPIConfiguration> stub = mockStatic(OpenAPIConfiguration.class)) {
            stub.when(() -> OpenAPIConfiguration.generateOpenAPI(input, SampleApp.class, mapper))
                    .thenReturn(generated);

            OpenAPI result = new FinalizingOpenApiProcessor(SampleApp.class, mapper).apply(input);

            assertNotNull(result);
            assertSame(generated, result, "result must come from OpenAPIConfiguration.generateOpenAPI");
            stub.verify(() -> OpenAPIConfiguration.generateOpenAPI(input, SampleApp.class, mapper));
        }
    }
}
