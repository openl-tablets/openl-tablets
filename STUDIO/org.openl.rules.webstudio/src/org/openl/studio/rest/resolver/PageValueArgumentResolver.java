package org.openl.studio.rest.resolver;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;

/**
 * REST API {@link Page} parameter type resolver. Resolves {@link Page} type from {@code page} and {@code size} query
 * parameters. Default values can be set up using {@link PaginationDefault} annotation.
 *
 * @author Vladyslav Pikus
 * @see OffsetValueArgumentResolver
 */
@Component
public class PageValueArgumentResolver extends AbstractPaginationValueArgumentResolver {

    @Override
    protected Pageable handleValue(MethodParameter parameter, NativeWebRequest webRequest) {
        var def = Optional.ofNullable(getDefaultFromAnnotation(parameter));
        var page = Optional.ofNullable(parseParameter(webRequest, PAGE_QUERY_PARAM, 0));
        var size = Optional.ofNullable(parseParameter(webRequest, PAGE_SIZE_QUERY_PARAM, 1));
        if (def.isEmpty() && size.isEmpty() && page.isEmpty()) {
            return Page.unpaged();
        }

        var pageNumber = page.orElseGet(() -> def.map(Page::getPageNumber).orElse(0));
        var pageSize = size.orElseGet(() -> def.map(Page::getPageSize).orElse(DEFAULT_PAGE_SIZE));

        return Page.of(pageNumber, pageSize);
    }

    private Page getDefaultFromAnnotation(MethodParameter parameter) {
        var defaultAnno = parameter.getParameterAnnotation(PaginationDefault.class);
        if (defaultAnno == null) {
            return null;
        }
        var page = defaultAnno.page();
        if (page < 0) {
            var annotatedMethod = parameter.getMethod();
            throw new IllegalStateException(
                    "Invalid default page number configured for method '%s'. Must not be less than zero.".formatted(
                            annotatedMethod));
        }
        return Page.of(page, getDefaultPageSize(parameter, defaultAnno));
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Page.class.equals(parameter.getParameterType());
    }
}
