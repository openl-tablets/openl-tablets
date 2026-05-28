package org.openl.studio.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.service.OpenApiOperationCustomizer;
import org.openl.studio.common.projection.FieldProjectionSupport;

/**
 * Advertises the {@value FieldProjectionSupport#PARAMETER_NAME} query parameter in the generated
 * OpenAPI document for every operation whose response is a projectable DTO.
 *
 * <p>Activation is automatic: the OpenAPI reader picks up every {@link OpenApiOperationCustomizer}
 * bean, so declaring this one is the presence signal.
 *
 * <p>The bean depends on {@link FieldProjectionSupport}, which couples the customization to the
 * projection feature itself.
 *
 * @author Vladyslav Pikus
 */
@Component
@RequiredArgsConstructor
public class FieldProjectionOpenApiCustomizer implements OpenApiOperationCustomizer {

    private static final String QUERY = "query";

    private final FieldProjectionSupport support;

    @Override
    public void customize(MethodInfo methodInfo, Operation operation) {
        if (!advertisesFieldsFor(methodInfo)) {
            return;
        }
        if (hasParameter(operation, FieldProjectionSupport.PARAMETER_NAME)) {
            return;
        }
        operation.addParametersItem(new Parameter()
                .name(FieldProjectionSupport.PARAMETER_NAME)
                .in(QUERY)
                .required(false)
                .description("Comma-separated list of response fields to return, including nested selection, "
                        + "e.g. 'id,name' or 'id,name,modules(id,name)'. When omitted, the full response is returned.")
                .schema(new StringSchema()));
    }

    private static boolean hasParameter(Operation operation, String parameterName) {
        return operation.getParameters() != null && operation.getParameters().stream()
                .anyMatch(parameter -> parameterName.equals(parameter.getName()) && QUERY.equals(parameter.getIn()));
    }

    /**
     * Whether this operation should advertise the {@code fields} parameter.
     *
     * <p>When the declared return type pins a concrete class, projection eligibility follows that
     * class directly. When the declared type is uninformative (for example {@code ResponseEntity<?>}),
     * the operation is treated as eligible as long as it produces JSON -- the runtime body shape is
     * what counts there, and the projection advice will simply not fire if the actual body is not a
     * projectable DTO.
     */
    private boolean advertisesFieldsFor(MethodInfo methodInfo) {
        var targetType = support.resolveTargetType(methodInfo.getReturnType());
        if (targetType != null) {
            return support.isProjectable(targetType);
        }
        return producesJson(methodInfo);
    }

    private static boolean producesJson(MethodInfo methodInfo) {
        // Explicit produces= entry is required: an empty list means the controller did not declare
        // any media type, and we prefer to under-advertise rather than tag binary-only endpoints.
        for (var mediaType : methodInfo.getProduces()) {
            if (MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(mediaType)
                    || MediaType.ALL_VALUE.equals(mediaType)) {
                return true;
            }
        }
        return false;
    }
}
