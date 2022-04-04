package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.ParameterProcessor;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * OpenAPI Parameter service helps to parse and build OpenAPI parameters from API annotation and Spring declaration
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiParameterService {

    private final OpenApiPropertyResolver apiPropertyResolver;
    private final RequestMappingHandlerAdapter mappingHandlerAdapter;

    @Autowired
    public OpenApiParameterService(Optional<List<ModelConverter>> modelConverters,
            OpenApiPropertyResolver apiPropertyResolver,
            RequestMappingHandlerAdapter mappingHandlerAdapter) {
        this.apiPropertyResolver = apiPropertyResolver;
        this.mappingHandlerAdapter = mappingHandlerAdapter;
        modelConverters.ifPresent(converters -> converters.forEach(ModelConverters.getInstance()::addConverter));
    }

    /**
     * Parses {@link Parameter} from all places in the method e.g. {@link io.swagger.v3.oas.models.Operation},
     * {@link io.swagger.v3.oas.annotations.Parameter}, and method parameters
     * 
     * @param apiContext current OpenAPI context
     * @param methodInfo method info
     * @param paramInfos the list of query, cookie, path and header method parameters
     * @return collection of parsed parameters
     */
    public Collection<Parameter> parse(OpenApiContext apiContext,
            MethodInfo methodInfo,
            List<ParameterInfo> paramInfos,
            Set<io.swagger.v3.oas.annotations.Parameter> ignore) {
        Map<PKey, Parameter> parameters = new LinkedHashMap<>();

        // process Parameters from Open API Operation annotation
        if (methodInfo.getOperationAnnotation() != null) {
            for (var apiParameter : methodInfo.getOperationAnnotation().parameters()) {
                if (ignore.contains(apiParameter)) {
                    continue;
                }
                var parameter = parseParameter(methodInfo, apiParameter, apiContext.getComponents());
                parameters.putIfAbsent(new PKey(parameter), parameter);
            }
        }

        // process API Parameters from method
        var apiParameters = ReflectionUtils.getRepeatableAnnotations(methodInfo.getMethod(),
            io.swagger.v3.oas.annotations.Parameter.class);
        if (apiParameters != null) {
            for (var apiParameter : apiParameters) {
                if (ignore.contains(apiParameter)) {
                    continue;
                }
                var parameter = parseParameter(methodInfo, apiParameter, apiContext.getComponents());
                parameters.putIfAbsent(new PKey(parameter), parameter);
            }
        }

        // process method parameters
        for (var paramInfo : paramInfos) {
            var wrappedParam = parseParameter(paramInfo, methodInfo, apiContext.getComponents());
            if (wrappedParam.isPresent()) {
                var param = wrappedParam.get();
                var key = new PKey(param);
                var duplicatedParam = parameters.get(key);
                if (duplicatedParam == null) {
                    parameters.put(key, param);
                } else {
                    mergeParameters(duplicatedParam, param);
                }
            }
        }
        return parameters.values();
    }

    private void mergeParameters(Parameter firstParam, Parameter secondParam) {
        if (firstParam.getRequired() == null && secondParam.getRequired() == Boolean.TRUE) {
            firstParam.setRequired(Boolean.TRUE);
        }
        if (firstParam.get$ref() != null) {
            return;
        }
        if (firstParam.get$ref() == null && secondParam.get$ref() != null) {
            firstParam.set$ref(secondParam.get$ref());
            return;
        }
        if (firstParam.getContent() == null && secondParam.getContent() != null) {
            firstParam.setContent(secondParam.getContent());
        }
        if (firstParam.getSchema() == null && secondParam.getSchema() != null) {
            firstParam.setSchema(secondParam.getSchema());
        }
    }

    private Parameter parseParameter(MethodInfo methodInfo,
            io.swagger.v3.oas.annotations.Parameter apiParameter,
            Components components) {
        var parameter = new Parameter();

        if (StringUtils.isNotBlank(apiParameter.ref())) {
            parameter.set$ref(apiParameter.ref());
            return parameter;
        }

        var type = ParameterProcessor.getParameterType(apiParameter);
        ParameterProcessor.applyAnnotations(parameter,
            type,
            Collections.singletonList(apiParameter),
            components,
            new String[0],
            methodInfo.getConsumes(),
            methodInfo.getJsonView());

        if (StringUtils.isNotBlank(parameter.getDescription())) {
            parameter.description(apiPropertyResolver.resolve(parameter.getDescription()));
        }

        return parameter;
    }

    private Optional<Parameter> parseParameter(ParameterInfo paramInfo, MethodInfo methodInfo, Components components) {
        var requestParam = paramInfo.getParameterAnnotation(RequestParam.class);
        var pathVar = paramInfo.getParameterAnnotation(PathVariable.class);
        var requestHeader = paramInfo.getParameterAnnotation(RequestHeader.class);
        var cookieValue = paramInfo.getParameterAnnotation(CookieValue.class);

        var parameter = new Parameter();
        boolean empty = true;
        String defaultValue = null;
        if (pathVar != null) {
            if (StringUtils.isNotBlank(pathVar.value())) {
                parameter.setName(pathVar.value());
            }
            parameter.in(ParameterIn.PATH.toString()).required(pathVar.required());
            empty = false;
        } else if (requestHeader != null) {
            if (StringUtils.isNotBlank(requestHeader.value())) {
                parameter.setName(requestHeader.value());
            }
            defaultValue = requestHeader.defaultValue();
            parameter.in(ParameterIn.HEADER.toString()).required(requestHeader.required());
            empty = false;
        } else if (cookieValue != null) {
            if (StringUtils.isNotBlank(cookieValue.value())) {
                parameter.setName(cookieValue.value());
            }
            defaultValue = cookieValue.defaultValue();
            parameter.in(ParameterIn.COOKIE.toString()).required(cookieValue.required());
            empty = false;
        } else if (requestParam != null) {
            if (StringUtils.isNotBlank(requestParam.value())) {
                parameter.setName(requestParam.value());
            }
            defaultValue = requestParam.defaultValue();
            parameter.in(ParameterIn.QUERY.toString()).required(requestParam.required());
            empty = false;
        }
        if (empty && paramInfo.getParameter() == null) {
            return Optional.empty();
        }
        if (StringUtils.isBlank(parameter.getName())) {
            parameter.setName("arg" + paramInfo.getIndex());
        }

        if (parameter.getRequired() == Boolean.FALSE) {
            // false to null. Because null means is not required
            parameter.setRequired(null);
        }
        var parameterType = ParameterProcessor.getParameterType(paramInfo.getParameter(), true);
        if (parameterType == null) {
            parameterType = paramInfo.getType();
        }
        ParameterProcessor.applyAnnotations(parameter,
            parameterType,
            paramInfo.getParameter() != null ? List.of(paramInfo.getParameter()) : Collections.emptyList(),
            components,
            new String[0],
            methodInfo.getConsumes(),
            paramInfo.getJsonView());

        if (StringUtils.isNotBlank(parameter.getDescription())) {
            parameter.description(apiPropertyResolver.resolve(parameter.getDescription()));
        }

        if (parameter.getSchema() != null) {
            if (StringUtils.isNotBlank(defaultValue) && !ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
                parameter.getSchema().setDefault(defaultValue);
                // Supplying a default value implicitly sets required to false.
                parameter.setRequired(null);
            }
            applyValidationAnnotations(paramInfo, parameter.getSchema());
        }

        return Optional.of(parameter);
    }

    @SuppressWarnings("rawtypes")
    public void applyValidationAnnotations(ParameterInfo paramInfo, Schema schema) {
        if (schema == null) {
            return;
        }
        var min = paramInfo.getParameterAnnotation(Min.class);
        if (min != null) {
            schema.setMinimum(BigDecimal.valueOf(min.value()));
        }
        var max = paramInfo.getParameterAnnotation(Max.class);
        if (max != null) {
            schema.setMaximum(BigDecimal.valueOf(max.value()));
        }
        var decimalMin = paramInfo.getParameterAnnotation(DecimalMin.class);
        if (decimalMin != null) {
            schema.setMinimum(new BigDecimal(decimalMin.value()));
        }
        var decimalMax = paramInfo.getParameterAnnotation(DecimalMax.class);
        if (decimalMax != null) {
            schema.setMaximum(new BigDecimal(decimalMax.value()));
        }
        var pattern = paramInfo.getParameterAnnotation(Pattern.class);
        if (pattern != null) {
            schema.setPattern(pattern.regexp());
        }
    }

    @SuppressWarnings("rawtypes")
    public Schema resolveSchema(Type type, Components components, JsonView jsonView) {
        var resolvedSchema = ModelConverters.getInstance()
            .resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true).jsonViewAnnotation(jsonView));
        if (resolvedSchema.schema == null) {
            return null;
        }
        if (resolvedSchema.referencedSchemas != null) {
            resolvedSchema.referencedSchemas.forEach(components::addSchemas);
        }
        return resolvedSchema.schema;
    }

    public String[] getMediaTypesForType(Class<?> cl) {
        Set<MediaType> possibleMediaTypes = new TreeSet<>(
            MediaType.SPECIFICITY_COMPARATOR.thenComparing(MediaType.QUALITY_VALUE_COMPARATOR));
        for (var converter : mappingHandlerAdapter.getMessageConverters()) {
            possibleMediaTypes.addAll(converter.getSupportedMediaTypes(cl));
        }
        if (possibleMediaTypes.contains(MediaType.ALL)) {
            return MethodInfo.ALL_MEDIA_TYPES;
        }
        return possibleMediaTypes.stream().map(Object::toString).toArray(String[]::new);
    }

    private static final class PKey {
        public final String name;
        public final String in;

        public PKey(Parameter p) {
            this.name = p.getName();
            this.in = p.getIn();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PKey pKey = (PKey) o;
            return Objects.equals(name, pKey.name) && Objects.equals(in, pKey.in);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, in);
        }
    }

}
