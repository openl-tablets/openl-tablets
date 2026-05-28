package org.openl.studio.common.projection;

import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import org.openl.util.StringUtils;

/**
 * Reduces JSON responses to the fields requested by the {@code ?fields=} query parameter.
 *
 * <p>Transparent to controllers -- no extra parameter or annotation is required on any endpoint. Only
 * Jackson-rendered responses are touched; binary, {@code String} and other non-JSON payloads pass
 * through unchanged. Pagination wrappers keep all of their fields -- only the content elements are
 * projected.
 *
 * <p>Selection is hierarchical. {@code ?fields=id,name,modules(id,name)} projects the root and
 * recurses into nested objects and arrays. A field selected without a {@code (...)} sub-selection is
 * kept whole.
 *
 * <p>Projection only removes properties -- it never exposes hidden ones. {@code @JsonIgnore} and
 * {@code @JsonProperty(access = WRITE_ONLY)} stay in effect.
 *
 * @author Vladyslav Pikus
 */
@ControllerAdvice
@Order(3)
public class FieldProjectionResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    private final FieldProjectionSupport support;

    public FieldProjectionResponseBodyAdvice(FieldProjectionSupport support) {
        this.support = support;
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
                                           MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return;
        }
        var rawFields = servletRequest.getServletRequest().getParameter(FieldProjectionSupport.PARAMETER_NAME);
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
        if (selection.isEmpty()) {
            return;
        }
        var provider = getOrCreateFilterProvider(bodyContainer);
        if (provider != null) {
            provider.addFilter(FieldProjectionSupport.FILTER_ID, new FieldProjectionPropertyFilter(selection));
        }
    }

    /**
     * Merge into any FilterProvider already configured upstream (e.g. by another advice) instead of
     * replacing it -- defensive against future advices that contribute their own filters. Non-Simple
     * providers we don't recognize are left untouched and we skip projection rather than risk dropping
     * existing filters.
     */
    private SimpleFilterProvider getOrCreateFilterProvider(MappingJacksonValue bodyContainer) {
        var existing = bodyContainer.getFilters();
        switch (existing) {
            case null -> {
                var provider = new SimpleFilterProvider().setFailOnUnknownId(false);
                bodyContainer.setFilters(provider);
                return provider;
            }
            case SimpleFilterProvider simple -> {
                return simple;
            }
            default -> {
                return null;
            }
        }
    }
}
