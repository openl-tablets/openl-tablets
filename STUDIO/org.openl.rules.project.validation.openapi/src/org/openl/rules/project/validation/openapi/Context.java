package org.openl.rules.project.validation.openapi;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiPredicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.validation.ValidatedCompiledOpenClass;

class Context {
    @Getter
    @Setter
    private ValidatedCompiledOpenClass validatedCompiledOpenClass;
    @Getter
    @Setter
    private RulesDeploy rulesDeploy;
    @Getter
    @Setter
    private OpenAPI expectedOpenAPI;
    @Getter
    @Setter
    private OpenAPI actualOpenAPI;
    @Getter
    @Setter
    private IOpenClass openClass;
    @Getter
    @Setter
    private Class<?> serviceClass;
    @Getter
    @Setter
    private ClassLoader serviceClassLoader;
    @Getter
    @Setter
    private Map<Method, Method> methodMap;
    @Getter
    @Setter
    private boolean provideRuntimeContext;

    @Getter
    @Setter
    private String actualPath;
    @Getter
    @Setter
    private String expectedPath;
    @Getter
    @Setter
    private String operationType;
    @Getter
    @Setter
    private PathItem expectedPathItem;
    @Getter
    @Setter
    private PathItem actualPathItem;
    @Getter
    @Setter
    private Operation expectedOperation;
    @Getter
    @Setter
    private Operation actualOperation;
    @Getter
    @Setter
    private MediaType expectedMediaType;
    @Getter
    @Setter
    private MediaType actualMediaType;
    @Getter
    @Setter
    private String mediaType;

    @Getter
    @Setter
    private IOpenClass type;
    @Getter
    @Setter
    private Object targetService;
    @Getter
    @Setter
    private IOpenField field;
    @Getter
    @Setter
    private Method method;
    @Getter
    @Setter
    private IOpenMethod openMethod;
    @Getter
    @Setter
    private ObjectMapper objectMapper;
    @Getter
    @Setter
    private BiPredicate<Schema, IOpenField> isIncompatibleTypesPredicate;

    @Getter
    private final OpenClassPropertiesResolver openClassPropertiesResolver = new OpenClassPropertiesResolver(this);
    @Getter
    @Setter
    private OpenAPIResolver actualOpenAPIResolver;
    @Getter
    @Setter
    private OpenAPIResolver expectedOpenAPIResolver;
    @Getter
    private final SpreadsheetMethodResolver spreadsheetMethodResolver = new SpreadsheetMethodResolver(this);

    @Getter
    @Setter
    private boolean yaml;

    @Getter
    @Setter
    private boolean typeValidationInProgress;
}
