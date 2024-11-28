package org.openl.rules.rest.resolver;

import java.lang.reflect.Method;
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
        Optional<Page> def = Optional.ofNullable(getDefaultFromAnnotation(parameter));
        Optional<Integer> page = Optional.ofNullable(parseParameter(webRequest, PAGE_QUERY_PARAM, 0));
        Optional<Integer> size = Optional.ofNullable(parseParameter(webRequest, PAGE_SIZE_QUERY_PARAM, 1));
        if (def.isEmpty() && size.isEmpty() && page.isEmpty()) {
            return Page.unpaged();
        }

        int pageNumber = page.orElseGet(() -> def.map(Page::getPageNumber).orElse(0));
        int pageSize = size.orElseGet(() -> def.map(Page::getPageSize).orElse(DEFAULT_PAGE_SIZE));

        return Page.of(pageNumber, pageSize);
    }

    private Page getDefaultFromAnnotation(MethodParameter parameter) {
        PaginationDefault defaultAnno = parameter.getParameterAnnotation(PaginationDefault.class);
        if (defaultAnno == null) {
            return null;
        }
        int page = defaultAnno.page();
        if (page < 0) {
            Method annotatedMethod = parameter.getMethod();
            throw new IllegalStateException(
                    String.format("Invalid default page number configured for method '%s'. Must not be less than zero.",
                            annotatedMethod));
        }
        return Page.of(page, getDefaultPageSize(parameter, defaultAnno));
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Page.class.equals(parameter.getParameterType());
    }
}
