package org.openl.rules.rest.resolver;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.openl.rules.repository.api.Pageable;
import org.openl.rules.rest.exception.BadRequestException;
import org.openl.util.StringUtils;

/**
 * An abstract REST API {@link Pageable} type resolver.
 *
 * @author Vladyslav Pikus
 * @see OffsetValueArgumentResolver
 * @see PageValueArgumentResolver
 */
public abstract class AbstractPaginationValueArgumentResolver implements HandlerMethodArgumentResolver {

    static final String PAGE_QUERY_PARAM = "page";
    static final String OFFSET_QUERY_PARAM = "offset";
    static final String PAGE_SIZE_QUERY_PARAM = "size";
    static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        validateQueryParameters(webRequest);
        return handleValue(parameter, webRequest);
    }

    protected abstract Pageable handleValue(MethodParameter parameter, NativeWebRequest webRequest);

    protected Integer parseParameter(NativeWebRequest webRequest, String parameterName, int min) {
        String value = webRequest.getParameter(parameterName);
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min) {
                throw new BadRequestException("pageable.min.query.message", new Object[]{parameterName, min});
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new BadRequestException("pageable.parse.query.message", new Object[]{parameterName, value});
        }
    }

    protected int getDefaultPageSize(MethodParameter parameter, PaginationDefault defaults) {
        int size = defaults.size();
        if (size < 1) {
            Method annotatedMethod = parameter.getMethod();
            throw new IllegalStateException(
                    String.format("Invalid default page size configured for method '%s'. Must not be less than one.",
                            annotatedMethod));
        }
        return size;
    }

    private void validateQueryParameters(NativeWebRequest webRequest) {
        if (hasQueryParam(webRequest, PAGE_QUERY_PARAM) && hasQueryParam(webRequest, OFFSET_QUERY_PARAM)) {
            throw new BadRequestException("invalid.pageable.query.message");
        }
    }

    protected static boolean hasQueryParam(NativeWebRequest webRequest, String name) {
        return StringUtils.isNotBlank(webRequest.getParameter(name));
    }
}
