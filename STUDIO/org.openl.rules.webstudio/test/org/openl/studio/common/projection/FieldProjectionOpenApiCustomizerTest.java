package org.openl.studio.common.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.projection.test.ProjectTestView;

class FieldProjectionOpenApiCustomizerTest {

    private static FieldProjectionOpenApiCustomizer customizer(boolean enabled) {
        var properties = new FieldProjectionProperties(enabled, false, "fields",
                List.of("org.openl.studio.common.projection.test"));
        return new FieldProjectionOpenApiCustomizer(new FieldProjectionSupport(properties));
    }

    private static Parameter customize(boolean enabled, Type returnType) {
        var methodInfo = mock(MethodInfo.class);
        when(methodInfo.getReturnType()).thenReturn(returnType);
        var operation = new Operation();
        customizer(enabled).customize(methodInfo, operation);
        if (operation.getParameters() == null) {
            return null;
        }
        return operation.getParameters().stream()
                .filter(p -> "fields".equals(p.getName()) && "query".equals(p.getIn()))
                .findFirst()
                .orElse(null);
    }

    @Test
    void addsParameterForSingleProjectableDto() {
        var param = customize(true, ProjectTestView.class);
        assertTrue(param != null && Boolean.FALSE.equals(param.getRequired()));
        assertEquals("string", param.getSchema().getType());
    }

    @Test
    void addsParameterForListOfProjectableDto() {
        assertTrue(customize(true, new TypeReference<List<ProjectTestView>>() {}.getType()) != null);
    }

    @Test
    void addsParameterForPageOfProjectableDto() {
        assertTrue(customize(true, new TypeReference<PageResponse<ProjectTestView>>() {}.getType()) != null);
    }

    @Test
    void addsParameterForResponseEntityOfProjectableDto() {
        assertTrue(customize(true, new TypeReference<ResponseEntity<ProjectTestView>>() {}.getType()) != null);
    }

    @Test
    void skipsNonProjectableResponse() {
        assertNull(customize(true, String.class));
    }

    @Test
    void skipsWhenProjectionDisabled() {
        assertNull(customize(false, ProjectTestView.class));
    }
}
