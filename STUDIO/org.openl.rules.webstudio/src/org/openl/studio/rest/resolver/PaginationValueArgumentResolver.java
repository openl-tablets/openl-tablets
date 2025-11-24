package org.openl.studio.rest.resolver;

import static org.openl.studio.rest.resolver.AbstractPaginationValueArgumentResolver.OFFSET_QUERY_PARAM;
import static org.openl.studio.rest.resolver.AbstractPaginationValueArgumentResolver.hasQueryParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.openl.rules.repository.api.Offset;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;

/**
 * REST API {@link Pageable} parameter type resolver. Resolves {@link Offset} or {@link Page} types from {@code offset},
 * {@code page} and {@code size} query parameters. Default values can be set up using {@link PaginationDefault}
 * annotation.
 *
 * @author Vladyslav Pikus
 * @see PageValueArgumentResolver
 * @see OffsetValueArgumentResolver
 */
@Component
public class PaginationValueArgumentResolver implements HandlerMethodArgumentResolver {

    private final Map<Class<? extends Pageable>, AbstractPaginationValueArgumentResolver> paginationResolvers;

    public PaginationValueArgumentResolver(OffsetValueArgumentResolver offsetValueArgResolver,
                                           PageValueArgumentResolver pageValueArgResolver) {
        Map<Class<? extends Pageable>, AbstractPaginationValueArgumentResolver> paginationResolvers = new HashMap<>();
        paginationResolvers.put(Offset.class, offsetValueArgResolver);
        paginationResolvers.put(Page.class, pageValueArgResolver);
        this.paginationResolvers = Collections.unmodifiableMap(paginationResolvers);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Pageable.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        var resolver = selectResolver(parameter, webRequest);
        return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

    private AbstractPaginationValueArgumentResolver selectResolver(MethodParameter parameter,
                                                                   NativeWebRequest webRequest) {
        if (hasQueryParam(webRequest, OFFSET_QUERY_PARAM)) {
            return paginationResolvers.get(Offset.class);
        } else {
            return paginationResolvers.get(Page.class);
        }
    }

}
