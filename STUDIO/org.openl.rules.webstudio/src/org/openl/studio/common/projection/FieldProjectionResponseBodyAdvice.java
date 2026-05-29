package org.openl.studio.common.projection;

import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
@RequiredArgsConstructor
public class FieldProjectionResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    private final FieldProjectionSupport support;

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
                                           MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return;
        }
        var rawFields = servletRequest.getServletRequest().getParameter(FieldProjectionSupport.PARAMETER_NAME);
        if (StringUtils.isBlank(rawFields)) {
            return;
        }
        // Projection eligibility follows the declared return type, with the runtime body as a fallback
        // for wildcard types like ResponseEntity<?>. This decouples validation of the fields parameter
        // from whether the current response happens to be empty: an invalid value fails the same way
        // whether the page has zero or many elements.
        if (!isProjectableEndpoint(returnType, bodyContainer.getValue())) {
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

    private boolean isProjectableEndpoint(MethodParameter returnType, @Nullable Object body) {
        var declared = support.resolveTargetType(returnType.getGenericParameterType());
        if (declared != null) {
            return support.isProjectable(declared);
        }
        var runtime = support.resolveTargetType(body);
        return runtime != null && support.isProjectable(runtime);
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
