package org.openl.rules.openapi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenLOpenAPIUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenLOpenAPIUtils.class);

    public static ParseOptions getParseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        return options;
    }

    public enum Path {
        REQUESTS,
        RESPONSES,
        ALL
    }

    public static Operation getOperation(PathItem path) {
        Map<PathItem.HttpMethod, Operation> operationsMap = path.readOperationsMap();
        if (operationsMap.isEmpty()) {
            return null;
        }
        if (operationsMap.containsKey(PathItem.HttpMethod.GET)) {
            return operationsMap.get(PathItem.HttpMethod.GET);
        } else if (operationsMap.containsKey(PathItem.HttpMethod.POST)) {
            return operationsMap.get(PathItem.HttpMethod.POST);
        } else {
            return operationsMap.values().iterator().next();
        }
    }

    public static ApiResponse getResponse(OpenAPI openAPI, ApiResponses apiResponses) {
        if (CollectionUtils.isEmpty(apiResponses)) {
            return null;
        }
        ApiResponse successResponse = apiResponses.get("200");
        ApiResponse defaultResponse = apiResponses.getDefault();
        ApiResponse result;
        if (successResponse != null) {
            result = successResponse;
        } else if (defaultResponse != null) {
            result = defaultResponse;
        } else {
            result = apiResponses.values().iterator().next();
        }
        return getReferencedApiResponse(openAPI, result);
    }

    public static MediaType getMediaType(Content content) {
        Set<String> mediaTypes = content.keySet();
        if (mediaTypes.contains("application/json")) {
            return content.get("application/json");
        } else if (mediaTypes.contains("text/plain")) {
            return content.get("text/plain");
        } else {
            return content.values().iterator().next();
        }
    }

    public static String getSimpleName(String ref) {
        if (ref.startsWith("#/components/")) {
            ref = ref.substring(ref.lastIndexOf("/") + 1);
        } else {
            LOGGER.warn("Failed to get the schema name: {}", ref);
            return null;
        }
        return ref;
    }

    public static <T> T resolveByRef(JXPathContext jxPathContext, T obj, Function<T, String> getRefFuc) {
        if (obj != null && getRefFuc.apply(obj) != null) {
            return (T) resolveByRef(jxPathContext, getRefFuc.apply(obj));
        }
        return obj;
    }

    public static Object resolveByRef(JXPathContext jxPathContext, String ref) {
        ref = ref.substring(1);
        CompiledExpression compiledExpression = JXPathContext.compile(ref);
        return compiledExpression.createPath(jxPathContext).getValue();
    }

    public static Map<String, Integer> getAllUsedRefs(OpenAPI openAPI, Path explorePath) {
        Map<String, Integer> types = new HashMap<>();
        visitOpenAPI(openAPI, explorePath, s -> {
            if (s.get$ref() != null) {
                String ref = s.get$ref();
                types.merge(ref, 1, Integer::sum);
            }
        });
        return types;
    }


    public static Set<String> visitResponseSchema(OpenAPI openAPI, Schema schema) {
        Set<String> result = new HashSet<>();
        Set<String> visitedSchema = new HashSet<>();
        visitSchema(openAPI, schema, null, visitedSchema, x -> {
            if (x.get$ref() != null) {
                result.add(x.get$ref());
            }
        }, false);
        return result;
    }

    private static void visitOpenAPI(OpenAPI openAPI,
            Path explorePath,
            OpenAPISchemaExplorer visitor) {
        Map<String, PathItem> paths = openAPI.getPaths();
        Set<String> visitedSchemas = new HashSet<>();

        if (paths != null) {
            for (PathItem path : paths.values()) {
                if (explorePath.equals(Path.ALL))
                    visitPathItem(path, openAPI, visitor, visitedSchemas);
                else if (explorePath.equals(Path.REQUESTS)) {
                    visitPathItemRequests(path, openAPI, visitor, visitedSchemas);
                }
            }
        }
    }

    private static void visitPathItem(PathItem pathItem,
            OpenAPI openAPI,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas) {
        List<Operation> allOperations = pathItem.readOperations();
        if (allOperations != null) {
            for (Operation operation : allOperations) {
                // Params:
                visitParameters(openAPI, operation.getParameters(), visitor, visitedSchemas);

                // RequestBody:
                RequestBody requestBody = getReferencedRequestBody(openAPI, operation.getRequestBody());
                if (requestBody != null) {
                    visitContent(openAPI, requestBody.getContent(), visitor, visitedSchemas);
                }

                // Responses:
                if (operation.getResponses() != null) {
                    for (ApiResponse r : operation.getResponses().values()) {
                        ApiResponse apiResponse = getReferencedApiResponse(openAPI, r);
                        if (apiResponse != null) {
                            visitContent(openAPI, apiResponse.getContent(), visitor, visitedSchemas);
                            if (apiResponse.getHeaders() != null) {
                                for (Map.Entry<String, Header> e : apiResponse.getHeaders().entrySet()) {
                                    Header header = getReferencedHeader(openAPI, e.getValue());
                                    if (header.getSchema() != null) {
                                        visitSchema(openAPI,
                                            header.getSchema(),
                                            e.getKey(),
                                            visitedSchemas,
                                            visitor,
                                            true);
                                    }
                                    visitContent(openAPI, header.getContent(), visitor, visitedSchemas);
                                }
                            }
                        }
                    }
                }

                // Callbacks: openl doesn't support it
                if (operation.getCallbacks() != null) {
                    for (Callback c : operation.getCallbacks().values()) {
                        Callback callback = getReferencedCallback(openAPI, c);
                        if (callback != null) {
                            for (PathItem p : callback.values()) {
                                visitPathItem(p, openAPI, visitor, visitedSchemas);
                            }
                        }
                    }
                }
            }
        }
        // Params:
        visitParameters(openAPI, pathItem.getParameters(), visitor, visitedSchemas);
    }

    private static void visitPathItemRequests(PathItem pathItem,
            OpenAPI openAPI,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas) {
        List<Operation> allOperations = pathItem.readOperations();
        if (allOperations != null) {
            for (Operation operation : allOperations) {
                // RequestBody:
                RequestBody requestBody = getReferencedRequestBody(openAPI, operation.getRequestBody());
                if (requestBody != null) {
                    visitContent(openAPI, requestBody.getContent(), visitor, visitedSchemas);
                }
            }
        }
    }

    private static void visitParameters(OpenAPI openAPI,
            List<Parameter> parameters,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas) {
        if (parameters != null) {
            for (Parameter p : parameters) {
                Parameter parameter = getReferencedParameter(openAPI, p);
                if (parameter != null) {
                    if (parameter.getSchema() != null) {
                        visitSchema(openAPI, parameter.getSchema(), null, visitedSchemas, visitor, true);
                    }
                    visitContent(openAPI, parameter.getContent(), visitor, visitedSchemas);
                } else {
                    LOGGER.warn("Unreferenced parameter(s) found.");
                }
            }
        }
    }

    private static Parameter getReferencedParameter(OpenAPI openAPI,
            Parameter parameter) {
        if (parameter != null && StringUtils.isNotEmpty(parameter.get$ref())) {
            String name = getSimpleName(parameter.get$ref());
            Parameter referencedParameter = getParameter(openAPI, name);
            if (referencedParameter != null) {
                return referencedParameter;
            }
        }
        return parameter;
    }

    private static Parameter getParameter(OpenAPI openAPI, String name) {
        if (name == null) {
            return null;
        }

        if (openAPI != null && openAPI.getComponents() != null && openAPI.getComponents().getParameters() != null) {
            return openAPI.getComponents().getParameters().get(name);
        }
        return null;
    }

    private static RequestBody getReferencedRequestBody(OpenAPI openAPI,
            RequestBody requestBody) {
        if (requestBody != null && StringUtils.isNotEmpty(requestBody.get$ref())) {
            String name = getSimpleName(requestBody.get$ref());
            RequestBody referencedRequestBody = getRequestBody(openAPI, name);
            if (referencedRequestBody != null) {
                return referencedRequestBody;
            }
        }
        return requestBody;
    }

    private static RequestBody getRequestBody(OpenAPI openAPI, String name) {
        if (name == null) {
            return null;
        }

        if (openAPI != null && openAPI.getComponents() != null && openAPI.getComponents().getRequestBodies() != null) {
            return openAPI.getComponents().getRequestBodies().get(name);
        }
        return null;
    }

    private static void visitContent(OpenAPI openAPI,
            Content content,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas) {
        if (content != null) {
            for (Map.Entry<String, MediaType> e : content.entrySet()) {
                Schema mediaTypeSchema = e.getValue().getSchema();
                if (mediaTypeSchema != null) {
                    visitSchema(openAPI, mediaTypeSchema, e.getKey(), visitedSchemas, visitor, true);
                }
            }
        }
    }

    public static ApiResponse getReferencedApiResponse(OpenAPI openAPI,
            ApiResponse apiResponse) {
        if (apiResponse != null && StringUtils.isNotBlank(apiResponse.get$ref())) {
            String name = getSimpleName(apiResponse.get$ref());
            ApiResponse referencedApiResponse = getApiResponse(openAPI, name);
            if (referencedApiResponse != null) {
                return referencedApiResponse;
            }
        }
        return apiResponse;
    }

    public static ApiResponse getApiResponse(OpenAPI openAPI, String name) {
        if (name == null) {
            return null;
        }

        if (openAPI != null && openAPI.getComponents() != null && openAPI.getComponents().getResponses() != null) {
            return openAPI.getComponents().getResponses().get(name);
        }
        return null;
    }

    public static Header getReferencedHeader(OpenAPI openAPI, Header header) {
        if (header != null && StringUtils.isNotEmpty(header.get$ref())) {
            String name = getSimpleName(header.get$ref());
            Header referencedheader = getHeader(openAPI, name);
            if (referencedheader != null) {
                return referencedheader;
            }
        }
        return header;
    }

    public static Header getHeader(OpenAPI openAPI, String name) {
        if (name == null) {
            return null;
        }

        if (openAPI != null && openAPI.getComponents() != null && openAPI.getComponents().getHeaders() != null) {
            return openAPI.getComponents().getHeaders().get(name);
        }
        return null;
    }

    public static Callback getReferencedCallback(OpenAPI openAPI, Callback callback) {
        if (callback != null && StringUtils.isNotEmpty(callback.get$ref())) {
            String name = getSimpleName(callback.get$ref());
            Callback referencedCallback = getCallback(openAPI, name);
            if (referencedCallback != null) {
                return referencedCallback;
            }
        }
        return callback;
    }

    public static Callback getCallback(OpenAPI openAPI, String name) {
        if (name == null) {
            return null;
        }

        if (openAPI != null && openAPI.getComponents() != null && openAPI.getComponents().getCallbacks() != null) {
            return openAPI.getComponents().getCallbacks().get(name);
        }
        return null;
    }

    private static void visitSchema(OpenAPI openAPI,
            Schema schema,
            String mimeType,
            Set<String> visitedSchemas,
            OpenAPISchemaExplorer visitor,
            boolean visitProperties) {
        visitor.explore(schema);
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            if (!visitedSchemas.contains(ref)) {
                visitedSchemas.add(ref);
                Schema referencedSchema = getSchemas(openAPI).get(getSimpleName(ref));
                if (referencedSchema != null) {
                    visitSchema(openAPI, referencedSchema, mimeType, visitedSchemas, visitor, visitProperties);
                }
            }
        }
        if (schema instanceof ComposedSchema) {
            List<Schema> oneOf = ((ComposedSchema) schema).getOneOf();
            if (oneOf != null) {
                for (Schema s : oneOf) {
                    visitSchema(openAPI, s, mimeType, visitedSchemas, visitor, visitProperties);
                }
            }
            List<Schema> allOf = ((ComposedSchema) schema).getAllOf();
            if (allOf != null) {
                for (Schema s : allOf) {
                    visitSchema(openAPI, s, mimeType, visitedSchemas, visitor, visitProperties);
                }
            }
            List<Schema> anyOf = ((ComposedSchema) schema).getAnyOf();
            if (anyOf != null) {
                for (Schema s : anyOf) {
                    visitSchema(openAPI, s, mimeType, visitedSchemas, visitor, visitProperties);
                }
            }
        } else if (schema instanceof ArraySchema) {
            Schema itemsSchema = ((ArraySchema) schema).getItems();
            if (itemsSchema != null) {
                visitSchema(openAPI, itemsSchema, mimeType, visitedSchemas, visitor, visitProperties);
            }
        } else if (isMapSchema(schema)) {
            Object additionalProperties = schema.getAdditionalProperties();
            if (additionalProperties instanceof Schema) {
                visitSchema(openAPI, (Schema) additionalProperties, mimeType, visitedSchemas, visitor, visitProperties);
            }
        }
        if (schema.getNot() != null) {
            visitSchema(openAPI, schema.getNot(), mimeType, visitedSchemas, visitor, visitProperties);
        }
        if (visitProperties) {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                for (Schema property : properties.values()) {
                    visitSchema(openAPI, property, mimeType, visitedSchemas, visitor, visitProperties);
                }
            }
        }
    }

    private static boolean isMapSchema(Schema schema) {
        if (schema instanceof MapSchema) {
            return true;
        }

        if (schema == null) {
            return false;
        }

        if (schema.getAdditionalProperties() instanceof Schema) {
            return true;
        }

        if (schema.getAdditionalProperties() instanceof Boolean && (Boolean) schema.getAdditionalProperties()) {
            return true;
        }

        return false;
    }

    public static Map<String, Schema> getSchemas(OpenAPI openAPI) {
        if (openAPI != null && openAPI.getComponents() != null && CollectionUtils
            .isNotEmpty(openAPI.getComponents().getSchemas())) {
            return openAPI.getComponents().getSchemas();
        }
        return Collections.emptyMap();
    }

    public static Map<String, List<String>> getChildrenMap(OpenAPI openAPI) {
        Map<String, Schema> allSchemas = getSchemas(openAPI);
        Map<String, List<Map.Entry<String, Schema>>> groupedByParent = allSchemas.entrySet()
            .stream()
            .filter(entry -> isComposedSchema(entry.getValue()))
            .filter(entry -> getParentName((ComposedSchema) entry.getValue(), allSchemas) != null)
            .collect(Collectors.groupingBy(entry -> getParentName((ComposedSchema) entry.getValue(), allSchemas)));

        return groupedByParent.entrySet()
            .stream()
            .collect(Collectors.toMap(entry -> entry.getKey(),
                entry -> entry.getValue().stream().map(e -> e.getKey()).collect(Collectors.toList())));
    }

    private static boolean isComposedSchema(Schema schema) {
        if (schema instanceof ComposedSchema) {
            return true;
        }
        return false;
    }

    public static String getParentName(ComposedSchema composedSchema, Map<String, Schema> allSchemas) {
        List<Schema> interfaces = getInterfaces(composedSchema);
        int nullSchemaChildrenCount = 0;
        boolean hasAmbiguousParents = false;
        List<String> refedWithoutDiscriminator = new ArrayList<>();

        if (interfaces != null && !interfaces.isEmpty()) {
            for (Schema schema : interfaces) {
                // get the actual schema
                if (StringUtils.isNotEmpty(schema.get$ref())) {
                    String parentName = getSimpleName(schema.get$ref());
                    Schema s = allSchemas.get(parentName);
                    if (s == null) {
                        LOGGER.error("Failed to obtain schema from {}", parentName);
                        return "UNKNOWN_PARENT_NAME";
                    } else if (hasOrInheritsDiscriminator(s, allSchemas)) {
                        // discriminator.propertyName is used
                        return parentName;
                    } else {
                        // not a parent since discriminator.propertyName is not set
                        hasAmbiguousParents = true;
                        refedWithoutDiscriminator.add(parentName);
                    }
                } else {
                    // not a ref, doing nothing, except counting the number of times the 'null' type
                    // is listed as composed element.
                    if (isNullType(schema)) {
                        // If there are two interfaces, and one of them is the 'null' type,
                        // then the parent is obvious and there is no need to warn about specifying
                        // a determinator.
                        nullSchemaChildrenCount++;
                    }
                }
            }
            if (refedWithoutDiscriminator.size() == 1 && nullSchemaChildrenCount == 1) {
                // One schema is a $ref and the other is the 'null' type, so the parent is obvious.
                // In this particular case there is no need to specify a discriminator.
                hasAmbiguousParents = false;
            }
        }

        // parent name only makes sense when there is a single obvious parent
        if (refedWithoutDiscriminator.size() == 1) {
            if (hasAmbiguousParents) {
                LOGGER.warn(
                    "[deprecated] inheritance without use of 'discriminator.propertyName' is deprecated " + "and will be removed in a future release. Generating model for composed schema name: {}. Title: {}",
                    composedSchema.getName(),
                    composedSchema.getTitle());
            }
            return refedWithoutDiscriminator.get(0);
        }

        return null;
    }

    private static List<Schema> getInterfaces(ComposedSchema composed) {
        if (composed.getAllOf() != null && !composed.getAllOf().isEmpty()) {
            return composed.getAllOf();
        } else if (composed.getAnyOf() != null && !composed.getAnyOf().isEmpty()) {
            return composed.getAnyOf();
        } else if (composed.getOneOf() != null && !composed.getOneOf().isEmpty()) {
            return composed.getOneOf();
        } else {
            return Collections.emptyList();
        }
    }

    private static boolean hasOrInheritsDiscriminator(Schema schema, Map<String, Schema> allSchemas) {
        if (schema.getDiscriminator() != null && StringUtils.isNotEmpty(schema.getDiscriminator().getPropertyName())) {
            return true;
        } else if (StringUtils.isNotEmpty(schema.get$ref())) {
            String parentName = getSimpleName(schema.get$ref());
            Schema s = allSchemas.get(parentName);
            if (s != null) {
                return hasOrInheritsDiscriminator(s, allSchemas);
            } else {
                LOGGER.error("Failed to obtain schema from {}", parentName);
            }
        } else if (schema instanceof ComposedSchema) {
            final ComposedSchema composed = (ComposedSchema) schema;
            final List<Schema> interfaces = getInterfaces(composed);
            for (Schema i : interfaces) {
                if (hasOrInheritsDiscriminator(i, allSchemas)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNullType(Schema schema) {
        if ("null".equals(schema.getType())) {
            return true;
        }
        return false;
    }
}
