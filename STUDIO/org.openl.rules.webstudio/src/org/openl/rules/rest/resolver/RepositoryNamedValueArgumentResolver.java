package org.openl.rules.rest.resolver;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.servlet.HandlerMapping;

/**
 * <p>An @{@link DesignRepository} is a named value that gets resolved from a URI template variable.
 * It is always required and does not have a default value to fall back on. See the base class
 * {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}
 * for more information on how named values are processed.
 *
 * @author Vladyslav Pikus
 *
 * @see DesignRepository
 * @see DesignTimeRepository
 */
@Component
public class RepositoryNamedValueArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

    private final DesignTimeRepository dtRepository;

    public RepositoryNamedValueArgumentResolver(DesignTimeRepository dtRepository) {
        this.dtRepository = dtRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(DesignRepository.class)) {
            return false;
        }
        if (!Repository.class.isAssignableFrom(parameter.getParameterType())) {
            return false;
        }
        DesignRepository anno = parameter.getParameterAnnotation(DesignRepository.class);
        return anno != null && StringUtils.isNotBlank(anno.value());
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        DesignRepository anno = Objects.requireNonNull(parameter.getParameterAnnotation(DesignRepository.class),
            "No 'DesignRepository' annotation");
        return new DesignRepositoryNamedValueInfo(anno);
    }

    @Override
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) {
        Map<String, String> uriTemplateVars = resolverUriTemplateVars(request);
        return Optional.ofNullable(uriTemplateVars.get(name)).map(dtRepository::getRepository).orElse(null);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request) {
        Map<String, String> uriTemplateVars = resolverUriTemplateVars(request);
        throw new NotFoundException("design.repo.message", uriTemplateVars.get(name));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> resolverUriTemplateVars(NativeWebRequest request) {
        return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
            RequestAttributes.SCOPE_REQUEST);
    }

    private static class DesignRepositoryNamedValueInfo extends NamedValueInfo {

        public DesignRepositoryNamedValueInfo(DesignRepository anno) {
            super(anno.value(), true, ValueConstants.DEFAULT_NONE);
        }

    }
}
