package org.openl.studio.rest.resolver;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import org.openl.rules.repository.api.Offset;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;

/**
 * REST API {@link Offset} parameter type resolver. Resolves {@link Offset} type from {@code offset} and {@code size}
 * query parameters. Default values can be set up using {@link PaginationDefault} annotation.
 *
 * @author Vladyslav Pikus
 * @see PageValueArgumentResolver
 */
@Component
public class OffsetValueArgumentResolver extends AbstractPaginationValueArgumentResolver {

    @Override
    protected Pageable handleValue(MethodParameter parameter, NativeWebRequest webRequest) {
        var def = Optional.ofNullable(getDefaultFromAnnotation(parameter));
        var offset = Optional.ofNullable(parseParameter(webRequest, OFFSET_QUERY_PARAM, 0));
        var size = Optional.ofNullable(parseParameter(webRequest, PAGE_SIZE_QUERY_PARAM, 1));
        if (def.isEmpty() && size.isEmpty() && offset.isEmpty()) {
            return Page.unpaged();
        }

        var pageOffset = offset.orElseGet(() -> def.map(Offset::getOffset).orElse(0));
        var pageSize = size.orElseGet(() -> def.map(Offset::getPageSize).orElse(DEFAULT_PAGE_SIZE));

        return Offset.of(pageOffset, pageSize);
    }

    private Offset getDefaultFromAnnotation(MethodParameter parameter) {
        var defaultAnno = parameter.getParameterAnnotation(PaginationDefault.class);
        if (defaultAnno == null) {
            return null;
        }
        var offset = defaultAnno.offset();
        if (offset < 0) {
            var annotatedMethod = parameter.getMethod();
            throw new IllegalStateException(
                    "Invalid default page offset configured for method '%s'. Must not be less than zero.".formatted(
                            annotatedMethod));
        }
        return Offset.of(offset, getDefaultPageSize(parameter, defaultAnno));
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Offset.class.equals(parameter.getParameterType());
    }
}
