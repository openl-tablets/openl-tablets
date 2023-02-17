package org.openl.rules.rest.resolver;

import java.lang.reflect.Method;
import java.util.Optional;

import org.openl.rules.repository.api.Page;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class PaginationValueArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PAGE_QUERY_PARAM = "page";
    private static final String PAGE_SIZE_QUERY_PARAM = "size";

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Page.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
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
        int size = defaultAnno.size();
        if (size < 1) {
            Method annotatedMethod = parameter.getMethod();
            throw new IllegalStateException(
                String.format("Invalid default page size configured for method '%s'. Must not be less than one.",
                    annotatedMethod));
        }
        return Page.of(page, size);
    }

    private Integer parseParameter(NativeWebRequest webRequest, String parameterName, int min) {
        String value = webRequest.getParameter(parameterName);
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min) {
                throw new IllegalStateException(String
                    .format("The value of '%s' query parameter must be greater or equal '%s'.", parameterName, min));
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                String.format("Failed to parse the value of '%s' query parameter: %s", parameterName, value),
                e);
        }
    }
}
