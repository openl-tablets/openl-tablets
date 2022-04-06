package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * OpenAPI Parameter service helps to parse and build OpenAPI parameters from API annotation and Spring declaration
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiParameterService {

    /**
     * Parses {@link Parameter} from all places in the method e.g. {@link io.swagger.v3.oas.models.Operation},
     * {@link io.swagger.v3.oas.annotations.Parameter}, and method parameters
     *
     * @param apiContext current OpenAPI context
     * @param methodInfo method info
     * @param paramInfos the list of query, cookie, path and header method parameters
     * @return collection of parsed parameters
     */
    Collection<Parameter> generateParameters(OpenApiContext apiContext,
            MethodInfo methodInfo,
            List<ParameterInfo> paramInfos,
            Set<io.swagger.v3.oas.annotations.Parameter> ignore);

    /**
     * Applies javax validation annotations from method parameter to provided schema
     *
     * @param paramInfo method parameter
     * @param schema schema to apply validation annotations
     */
    void applyValidationAnnotations(ParameterInfo paramInfo, Schema<?> schema);

    /**
     * Resolves OpenAPI schema from requested type
     * 
     * @param type type to resolve
     * @param components schema container
     * @param jsonView json view annotation for requested type
     * @return resolved schema or null
     */
    @SuppressWarnings("rawtypes")
    Schema resolveSchema(Type type, Components components, JsonView jsonView);

    /**
     * Find suitable media types from defined Spring Message Converters
     *
     * @param cl class to resolve media types
     * @return found media types
     */
    String[] getMediaTypesForType(Class<?> cl);
}
