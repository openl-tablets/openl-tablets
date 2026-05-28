package org.openl.studio.common.projection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.service.OpenApiOperationCustomizer;
import org.openl.studio.common.model.PageResponse;

/**
 * Advertises the response field projection query parameter (default {@code fields}) in the generated
 * OpenAPI document for every operation whose response is a projectable DTO.
 *
 * <p>This bean is the presence signal for the {@link OpenApiOperationCustomizer} SPI: the OpenAPI reader
 * adds the parameter only because this bean exists. It is wired only when the field projection feature is
 * present (it depends on {@link FieldProjectionSupport}) and emits nothing while projection is disabled.
 *
 * @author Vladyslav Pikus
 */
@Component
public class FieldProjectionOpenApiCustomizer implements OpenApiOperationCustomizer {

    private static final String QUERY = "query";

    private final FieldProjectionSupport support;

    public FieldProjectionOpenApiCustomizer(FieldProjectionSupport support) {
        this.support = support;
    }

    @Override
    public void customize(MethodInfo methodInfo, Operation operation) {
        if (!support.isEnabled()) {
            return;
        }
        var targetType = resolveProjectableType(methodInfo.getReturnType());
        if (targetType == null || !support.isProjectable(targetType)) {
            return;
        }
        var parameterName = support.getParameterName();
        if (hasParameter(operation, parameterName)) {
            return;
        }
        operation.addParametersItem(new Parameter()
                .name(parameterName)
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
     * Resolves the element type the projection would target, unwrapping {@link HttpEntity}/{@code ResponseEntity},
     * {@link PageResponse}, collections and arrays. Mirrors {@link FieldProjectionSupport#resolveTargetType(Object)}
     * but works on the declared return {@link Type}.
     */
    private static Class<?> resolveProjectableType(Type type) {
        if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> rawType) {
            if (isWrapper(rawType)) {
                var arguments = parameterizedType.getActualTypeArguments();
                return arguments.length == 1 ? resolveProjectableType(arguments[0]) : null;
            }
            return rawType;
        }
        if (type instanceof Class<?> clazz) {
            return clazz.isArray() ? clazz.getComponentType() : clazz;
        }
        return null;
    }

    private static boolean isWrapper(Class<?> rawType) {
        return HttpEntity.class.isAssignableFrom(rawType)
                || PageResponse.class.isAssignableFrom(rawType)
                || Iterable.class.isAssignableFrom(rawType)
                || Collection.class.isAssignableFrom(rawType);
    }
}
