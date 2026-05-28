package org.openl.studio.common.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.util.StringUtils;

/**
 * Applies GraphQL-like response field projection driven by the {@code ?fields=} query parameter.
 *
 * <p>The advice is fully transparent to controllers: it reduces the serialized JSON to the requested
 * root-level fields of the response DTO without any controller-side parameter or annotation. It runs
 * only for Jackson-rendered responses (see {@link AbstractMappingJacksonResponseBodyAdvice#supports}),
 * so binary, {@code String} and other non-JSON payloads are unaffected.
 *
 * <p>Selection is hierarchical: {@code ?fields=id,name,modules(id,name)} projects the root and recurses
 * into nested objects/arrays. A field selected without a {@code (...)} sub-selection is kept whole.
 * Projection is applied by registering a single {@link FieldProjectionPropertyFilter} (shared by all
 * projectable DTOs via {@link FieldProjectionAnnotationIntrospector}); pagination wrappers are not
 * projectable and keep all of their fields. The projection can only remove properties, never add hidden
 * ones, so {@code @JsonIgnore} and {@code @JsonProperty(access = WRITE_ONLY)} stay in effect.
 *
 * @author Vladyslav Pikus
 */
@ControllerAdvice
@Order(3)
public class FieldProjectionResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    static final String UNKNOWN_FIELD_ERROR_CODE = "unknown.field.message";

    private final FieldProjectionSupport support;
    private final ObjectMapper objectMapper;

    public FieldProjectionResponseBodyAdvice(FieldProjectionSupport support, ObjectMapper objectMapper) {
        this.support = support;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return support.isEnabled() && super.supports(returnType, converterType);
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
                                           MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return;
        }
        var rawFields = servletRequest.getServletRequest().getParameter(support.getParameterName());
        if (StringUtils.isBlank(rawFields)) {
            // No projection requested -> response stays exactly as it is today.
            return;
        }
        var targetType = support.resolveTargetType(bodyContainer.getValue());
        if (targetType == null || !support.isProjectable(targetType)) {
            // Error responses, binary payloads, actuator, empty collections, ... are left untouched.
            return;
        }
        var selection = support.parseSelection(rawFields);
        if (!selection.hasChildren()) {
            return;
        }
        if (support.isFailOnUnknownField()) {
            var unknown = support.findUnknownPaths(objectMapper, targetType, selection);
            if (!unknown.isEmpty()) {
                throw new BadRequestException(UNKNOWN_FIELD_ERROR_CODE, new Object[]{String.join(", ", unknown)});
            }
        }
        FilterProvider filters = new SimpleFilterProvider()
                .setFailOnUnknownId(false)
                .addFilter(FieldProjectionSupport.FILTER_ID, new FieldProjectionPropertyFilter(selection));
        bodyContainer.setFilters(filters);
    }
}
