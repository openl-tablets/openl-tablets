package org.openl.studio.openapi;

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
import org.openl.studio.common.projection.FieldProjectionSupport;
import org.openl.studio.common.projection.test.ProjectTestView;

class FieldProjectionOpenApiCustomizerTest {

    private static final FieldProjectionOpenApiCustomizer CUSTOMIZER = new FieldProjectionOpenApiCustomizer(
            new FieldProjectionSupport());

    private static Parameter customize(Type returnType) {
        var methodInfo = mock(MethodInfo.class);
        when(methodInfo.getReturnType()).thenReturn(returnType);
        var operation = new Operation();
        CUSTOMIZER.customize(methodInfo, operation);
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
        var param = customize(ProjectTestView.class);
        assertTrue(param != null && Boolean.FALSE.equals(param.getRequired()));
        assertEquals("string", param.getSchema().getType());
    }

    @Test
    void addsParameterForListOfProjectableDto() {
        assertTrue(customize(new TypeReference<List<ProjectTestView>>() {}.getType()) != null);
    }

    @Test
    void addsParameterForPageOfProjectableDto() {
        assertTrue(customize(new TypeReference<PageResponse<ProjectTestView>>() {}.getType()) != null);
    }

    @Test
    void addsParameterForResponseEntityOfProjectableDto() {
        assertTrue(customize(new TypeReference<ResponseEntity<ProjectTestView>>() {}.getType()) != null);
    }

    @Test
    void skipsNonProjectableResponse() {
        assertNull(customize(String.class));
    }

    /**
     * Controllers may declare an in-scope interface (e.g. {@code EditableTableView}) as the return
     * type while returning a concrete subtype at runtime. The customizer must still advertise the
     * {@code fields} parameter for such operations.
     */
    @Test
    void addsParameterForInterfaceReturnTypeInScope() {
        assertTrue(customize(InScopeInterface.class) != null);
    }

    /** In-scope interface used to verify the OpenAPI customizer treats interfaces as projectable. */
    public interface InScopeInterface {
    }
}
