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

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ParameterModel;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
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

    private static final Logger logger = LoggerFactory.getLogger(OpenLOpenAPIUtils.class);
    public static final String SCHEMAS_LINK = "#/components/schemas/";
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";

    public static final byte MAX_PARAMETERS_COUNT = 7;

    @SuppressWarnings("unchecked")
    public static <T> T resolve(JXPathContext jxPathContext, T obj, Function<T, String> getRefFuc) {
        if (obj != null && getRefFuc.apply(obj) != null) {
            return resolve(jxPathContext, (T) resolveByRef(jxPathContext, getRefFuc.apply(obj)), getRefFuc);
        }
        return obj;
    }

    public static Object resolveByRef(JXPathContext jxPathContext, String ref) {
        ref = ref.substring(1);
        CompiledExpression compiledExpression = JXPathContext.compile(ref);
        return compiledExpression.createPath(jxPathContext).getValue();
    }

    public static ParseOptions getParseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        return options;
    }

    public static Set<String> getUnusedSchemaRefs(OpenAPI openAPI, Set<String> usedRefs) {
        Map<String, Schema> schemas = getSchemas(openAPI);
        Set<String> unusedSchemas = new HashSet<>(schemas.keySet());
        for (String usedRef : usedRefs) {
            String simpleName = getSimpleName(usedRef);
            unusedSchemas.remove(simpleName);
        }
        return unusedSchemas;
    }

    public static String getUsedSchemaInResponse(JXPathContext jxPathContext, PathItem pathItem) {
        String type = null;
        Operation satisfyingOperation = OpenLOpenAPIUtils.getOperation(pathItem);
        if (satisfyingOperation != null) {
            ApiResponses responses = satisfyingOperation.getResponses();
            if (responses != null) {
                ApiResponse response = getResponse(jxPathContext, responses);
                if (response != null && CollectionUtils.isNotEmpty(response.getContent())) {
                    MediaType mediaType = OpenLOpenAPIUtils.getMediaType(response.getContent());
                    if (mediaType != null) {
                        Schema<?> mediaTypeSchema = mediaType.getSchema();
                        if (mediaTypeSchema != null) {
                            type = extractType(mediaTypeSchema);
                        }
                    }
                }

            }
        }
        return type;
    }

    public enum PathTarget {
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

    public static ApiResponse getResponse(JXPathContext jxPathContext, ApiResponses apiResponses) {
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
        return resolve(jxPathContext, result, ApiResponse::get$ref);
    }

    public static MediaType getMediaType(Content content) {
        Set<String> mediaTypes = content.keySet();
        if (mediaTypes.contains(APPLICATION_JSON)) {
            return content.get(APPLICATION_JSON);
        } else if (mediaTypes.contains(TEXT_PLAIN)) {
            return content.get(TEXT_PLAIN);
        } else {
            return content.values().iterator().next();
        }
    }

    public static String getSimpleName(String ref) {
        if (ref.startsWith("#/components/")) {
            ref = ref.substring(ref.lastIndexOf('/') + 1);
        } else {
            logger.warn("Failed to get the schema name: {}", ref);
            return null;
        }
        return ref;
    }

    public static Map<String, Integer> getAllUsedSchemaRefs(OpenAPI openAPI,
            JXPathContext jxPathContext,
            PathTarget target) {
        Map<String, Integer> types = new HashMap<>();
        visitOpenAPI(openAPI, jxPathContext, target, s -> {
            if (s.get$ref() != null) {
                String ref = s.get$ref();
                types.merge(ref, 1, Integer::sum);
            }
        });
        return types;
    }

    public static Map<String, Set<String>> getAllUsedRefResponses(OpenAPI openAPI, JXPathContext jxPathContext) {
        Map<String, PathItem> paths = openAPI.getPaths();
        Map<String, Set<String>> allSchemaRefResponses = new HashMap<>();
        if (paths != null) {
            for (Map.Entry<String, PathItem> p : paths.entrySet()) {
                PathItem path = p.getValue();
                Map<PathItem.HttpMethod, Operation> operationsMap = path.readOperationsMap();
                if (CollectionUtils.isNotEmpty(operationsMap)) {
                    Operation satisfyingOperation = OpenLOpenAPIUtils.getOperation(path);
                    if (satisfyingOperation != null) {
                        ApiResponses responses = satisfyingOperation.getResponses();
                        if (responses != null) {
                            ApiResponse response = OpenLOpenAPIUtils.getResponse(jxPathContext, responses);
                            if (response != null && CollectionUtils.isNotEmpty(response.getContent())) {
                                MediaType mediaType = OpenLOpenAPIUtils.getMediaType(response.getContent());
                                if (mediaType != null) {
                                    Schema<?> mediaTypeSchema = mediaType.getSchema();
                                    if (mediaTypeSchema != null) {
                                        Set<String> refs = OpenLOpenAPIUtils
                                            .visitSchema(jxPathContext, mediaTypeSchema, false, false);
                                        allSchemaRefResponses.put(p.getKey(), refs);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return allSchemaRefResponses;
    }

    public static Set<String> visitSchema(JXPathContext jxPathContext,
            Schema<?> schema,
            boolean visitInterfaces,
            boolean visitProperties) {
        Set<String> result = new HashSet<>();
        Set<String> visitedSchema = new HashSet<>();
        visitSchema(jxPathContext, schema, null, visitedSchema, x -> {
            if (x.get$ref() != null) {
                result.add(x.get$ref());
            }
        }, visitInterfaces, visitProperties);
        return result;
    }

    private static void visitOpenAPI(OpenAPI openAPI,
            JXPathContext jxPathContext,
            PathTarget target,
            OpenAPISchemaExplorer visitor) {
        Map<String, PathItem> paths = openAPI.getPaths();
        Set<String> visitedSchemas = new HashSet<>();

        if (paths != null) {
            for (PathItem path : paths.values()) {
                if (target.equals(PathTarget.ALL))
                    visitPathItem(path, jxPathContext, visitor, visitedSchemas);
                else if (target.equals(PathTarget.REQUESTS)) {
                    visitPathItemRequests(path, jxPathContext, visitor, visitedSchemas);
                }
            }
        }
    }

    private static void visitPathItem(PathItem pathItem,
            JXPathContext jxPathContext,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas) {
        List<Operation> allOperations = pathItem.readOperations();
        if (allOperations != null) {
            for (Operation operation : allOperations) {
                // Params:
                visitParameters(jxPathContext, operation.getParameters(), visitor, visitedSchemas, true, true);

                // RequestBody:
                RequestBody requestBody = resolve(jxPathContext, operation.getRequestBody(), RequestBody::get$ref);
                if (requestBody != null) {
                    visitContent(jxPathContext, requestBody.getContent(), visitor, visitedSchemas, true);
                }

                // Responses:
                if (operation.getResponses() != null) {
                    for (ApiResponse r : operation.getResponses().values()) {
                        ApiResponse apiResponse = resolve(jxPathContext, r, ApiResponse::get$ref);
                        if (apiResponse != null) {
                            visitContent(jxPathContext, apiResponse.getContent(), visitor, visitedSchemas, true);
                            if (apiResponse.getHeaders() != null) {
                                for (Map.Entry<String, Header> e : apiResponse.getHeaders().entrySet()) {
                                    Header header = resolve(jxPathContext, e.getValue(), Header::get$ref);
                                    if (header.getSchema() != null) {
                                        visitSchema(jxPathContext,
                                            header.getSchema(),
                                            e.getKey(),
                                            visitedSchemas,
                                            visitor,
                                            true,
                                            true);
                                    }
                                    visitContent(jxPathContext, header.getContent(), visitor, visitedSchemas, true);
                                }
                            }
                        }
                    }
                }

                // Callbacks: openl doesn't support it
                if (operation.getCallbacks() != null) {
                    for (Callback c : operation.getCallbacks().values()) {
                        Callback callback = resolve(jxPathContext, c, Callback::get$ref);
                        if (callback != null) {
                            for (PathItem p : callback.values()) {
                                visitPathItem(p, jxPathContext, visitor, visitedSchemas);
                            }
                        }
                    }
                }
            }
        }
        // Params:
        visitParameters(jxPathContext, pathItem.getParameters(), visitor, visitedSchemas, true, true);
    }

    private static void visitPathItemRequests(PathItem pathItem,
            JXPathContext jxPathContext,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas) {
        List<Operation> allOperations = pathItem.readOperations();
        if (allOperations != null) {
            for (Operation operation : allOperations) {
                // parameters there too
                visitParameters(jxPathContext, operation.getParameters(), visitor, visitedSchemas, false, true);
                // RequestBody:
                RequestBody requestBody = resolve(jxPathContext, operation.getRequestBody(), RequestBody::get$ref);
                if (requestBody != null) {
                    visitContent(jxPathContext, requestBody.getContent(), visitor, visitedSchemas, false);
                }
            }
        }
        visitParameters(jxPathContext, pathItem.getParameters(), visitor, visitedSchemas, false, true);
    }

    private static void visitParameters(JXPathContext jxPathContext,
            List<Parameter> parameters,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas,
            boolean visitInterfaces,
            boolean visitProperties) {
        if (parameters != null) {
            for (Parameter p : parameters) {
                Parameter parameter = resolve(jxPathContext, p, Parameter::get$ref);
                if (parameter != null) {
                    if (parameter.getSchema() != null) {
                        visitSchema(jxPathContext,
                            parameter.getSchema(),
                            null,
                            visitedSchemas,
                            visitor,
                            visitInterfaces,
                            visitProperties);
                    }
                    visitContent(jxPathContext, parameter.getContent(), visitor, visitedSchemas, visitInterfaces);
                } else {
                    logger.warn("An unreferenced parameter(s) found.");
                }
            }
        }
    }

    private static void visitContent(JXPathContext jxPathContext,
            Content content,
            OpenAPISchemaExplorer visitor,
            Set<String> visitedSchemas,
            boolean visitInterfaces) {
        if (content != null) {
            for (Map.Entry<String, MediaType> e : content.entrySet()) {
                Schema<?> mediaTypeSchema = e.getValue().getSchema();
                if (mediaTypeSchema != null) {
                    visitSchema(jxPathContext,
                        mediaTypeSchema,
                        e.getKey(),
                        visitedSchemas,
                        visitor,
                        visitInterfaces,
                        true);
                }
            }
        }
    }

    private static void visitSchema(JXPathContext jxPathContext,
            Schema<?> schema,
            String mimeType,
            Set<String> visitedSchemas,
            OpenAPISchemaExplorer visitor,
            boolean visitInterfaces,
            boolean visitProperties) {
        visitor.explore(schema);
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            if (!visitedSchemas.contains(ref)) {
                visitedSchemas.add(ref);
                Schema<?> referencedSchema = resolve(jxPathContext, schema, Schema::get$ref);
                if (referencedSchema != null) {
                    visitSchema(jxPathContext,
                        referencedSchema,
                        mimeType,
                        visitedSchemas,
                        visitor,
                        visitInterfaces,
                        visitProperties);
                }
            }
        }
        if (schema instanceof ComposedSchema && visitInterfaces) {
            List<Schema> oneOf = ((ComposedSchema) schema).getOneOf();
            if (oneOf != null) {
                for (Schema<?> s : oneOf) {
                    visitSchema(jxPathContext, s, mimeType, visitedSchemas, visitor, visitInterfaces, visitProperties);
                }
            }
            List<Schema> allOf = ((ComposedSchema) schema).getAllOf();
            if (allOf != null) {
                for (Schema<?> s : allOf) {
                    visitSchema(jxPathContext, s, mimeType, visitedSchemas, visitor, visitInterfaces, visitProperties);
                }
            }

            List<Schema> anyOf = ((ComposedSchema) schema).getAnyOf();
            if (anyOf != null) {
                for (Schema<?> s : anyOf) {
                    visitSchema(jxPathContext, s, mimeType, visitedSchemas, visitor, visitInterfaces, visitProperties);
                }
            }
        } else if (schema instanceof ArraySchema) {
            Schema<?> itemsSchema = ((ArraySchema) schema).getItems();
            if (itemsSchema != null) {
                visitSchema(jxPathContext,
                    itemsSchema,
                    mimeType,
                    visitedSchemas,
                    visitor,
                    visitInterfaces,
                    visitProperties);
            }
        } else if (isMapSchema(schema)) {
            Object additionalProperties = schema.getAdditionalProperties();
            if (additionalProperties instanceof Schema) {
                visitSchema(jxPathContext,
                    (Schema) additionalProperties,
                    mimeType,
                    visitedSchemas,
                    visitor,
                    visitInterfaces,
                    visitProperties);
            }
        }
        if (schema.getNot() != null) {
            visitSchema(jxPathContext,
                schema.getNot(),
                mimeType,
                visitedSchemas,
                visitor,
                visitInterfaces,
                visitProperties);
        }
        if (visitProperties) {
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null) {
                for (Schema<?> property : properties.values()) {
                    visitSchema(jxPathContext,
                        property,
                        mimeType,
                        visitedSchemas,
                        visitor,
                        visitInterfaces,
                        visitProperties);
                }
            }
        }
    }

    private static boolean isMapSchema(Schema<?> schema) {
        if (schema instanceof MapSchema) {
            return true;
        }

        if (schema == null) {
            return false;
        }

        if (schema.getAdditionalProperties() instanceof Schema) {
            return true;
        }

        return schema.getAdditionalProperties() instanceof Boolean && (Boolean) schema.getAdditionalProperties();
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
            .filter(entry -> getParentName((ComposedSchema) entry.getValue(), openAPI) != null)
            .collect(Collectors.groupingBy(entry -> getParentName((ComposedSchema) entry.getValue(), openAPI)));

        return groupedByParent.entrySet()
            .stream()
            .collect(Collectors.toMap(mapEntry -> SCHEMAS_LINK + mapEntry.getKey(),
                entry -> entry.getValue().stream().map(x -> SCHEMAS_LINK + x.getKey()).collect(Collectors.toList())));
    }

    private static boolean isComposedSchema(Schema<?> schema) {
        return schema instanceof ComposedSchema;
    }

    public static String getParentName(ComposedSchema composedSchema, OpenAPI openAPI) {
        Map<String, Schema> allSchemas = getSchemas(openAPI);
        List<Schema> interfaces = getInterfaces(composedSchema);
        int nullSchemaChildrenCount = 0;
        boolean hasAmbiguousParents = false;
        List<String> refedWithoutDiscriminator = new ArrayList<>();

        if (interfaces != null && !interfaces.isEmpty()) {
            for (Schema<?> schema : interfaces) {
                // get the actual schema
                if (StringUtils.isNotEmpty(schema.get$ref())) {
                    String parentName = getSimpleName(schema.get$ref());
                    Schema<?> s = allSchemas.get(parentName);
                    if (s == null) {
                        logger.error("Failed to obtain schema from {}", parentName);
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
                // One schema is a $ref, and the other is the 'null' type, so the parent is obvious.
                // In this particular case there is no need to specify a discriminator.
                hasAmbiguousParents = false;
            }
        }

        // parent name only makes sense when there is a single obvious parent
        if (refedWithoutDiscriminator.size() == 1) {
            if (hasAmbiguousParents) {
                logger.warn(
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

    private static boolean hasOrInheritsDiscriminator(Schema<?> schema, Map<String, Schema> allSchemas) {
        if (schema.getDiscriminator() != null && StringUtils.isNotEmpty(schema.getDiscriminator().getPropertyName())) {
            return true;
        } else if (StringUtils.isNotEmpty(schema.get$ref())) {
            String parentName = getSimpleName(schema.get$ref());
            Schema<?> s = allSchemas.get(parentName);
            if (s != null) {
                return hasOrInheritsDiscriminator(s, allSchemas);
            } else {
                logger.error("Failed to obtain schema from {}", parentName);
            }
        } else if (schema instanceof ComposedSchema) {
            final ComposedSchema composed = (ComposedSchema) schema;
            final List<Schema> interfaces = getInterfaces(composed);
            for (Schema<?> i : interfaces) {
                if (hasOrInheritsDiscriminator(i, allSchemas)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNullType(Schema<?> schema) {
        return "null".equals(schema.getType());
    }

    public static String extractType(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return getSimpleName(schema.get$ref());
        }
        String schemaType = schema.getType();
        if ("string".equals(schemaType)) {
            if ("date".equals(schema.getFormat())) {
                return "Date";
            } else if ("date-time".equals(schema.getFormat())) {
                return "OffsetDateTime";
            }
            return "String";
        } else if ("number".equals(schemaType)) {
            if ("float".equals(schema.getFormat())) {
                return "Float";
            } else if ("double".equals(schema.getFormat())) {
                return "Double";
            } else {
                return "Double";
            }
        } else if ("integer".equals(schemaType)) {
            if ("int32".equals(schema.getFormat())) {
                return "Integer";
            } else if ("int64".equals(schema.getFormat())) {
                return "Long";
            } else {
                return "Long";
            }
        } else if ("boolean".equals(schemaType)) {
            return "Boolean";
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            String type = extractType(arraySchema.getItems());
            return type != null ? type + "[]" : "";
        }
        return "";
    }

    public static List<InputParameter> extractParameters(JXPathContext jxPathContext,
            Set<String> refsToExpand,
            PathItem pathItem,
            List<DatatypeModel> dts,
            String path) {
        return visitPathItemForParametersOfRequest(jxPathContext, pathItem, refsToExpand, dts, path);
    }

    private static List<InputParameter> visitPathItemForParametersOfRequest(JXPathContext jxPathContext,
            PathItem pathItem,
            Set<String> refsToExpand,
            List<DatatypeModel> dts,
            String path) {
        List<InputParameter> parameterModels = new ArrayList<>();
        List<Parameter> pathParameters = pathItem.getParameters();
        if (CollectionUtils.isNotEmpty(pathParameters)) {
            for (Parameter pathParameter : pathParameters) {
                Parameter p = resolve(jxPathContext, pathParameter, Parameter::get$ref);
                if (p != null) {
                    parameterModels.add(new ParameterModel(OpenLOpenAPIUtils.extractType(p.getSchema()), p.getName()));
                }
            }
        }
        Operation satisfyingOperation = OpenLOpenAPIUtils.getOperation(pathItem);
        if (satisfyingOperation != null) {
            List<Parameter> parameters = satisfyingOperation.getParameters();
            if (CollectionUtils.isNotEmpty(parameters)) {
                for (Parameter parameter : parameters) {
                    Parameter p = resolve(jxPathContext, parameter, Parameter::get$ref);
                    if (p != null) {
                        parameterModels
                            .add(new ParameterModel(OpenLOpenAPIUtils.extractType(p.getSchema()), p.getName()));
                    }
                }
            } else {
                RequestBody requestBody = resolve(jxPathContext,
                    satisfyingOperation.getRequestBody(),
                    RequestBody::get$ref);
                if (requestBody != null && CollectionUtils.isNotEmpty(requestBody.getContent())) {
                    MediaType mediaType = OpenLOpenAPIUtils.getMediaType(requestBody.getContent());
                    if (mediaType != null) {
                        Schema<?> resSchema = resolve(jxPathContext, mediaType.getSchema(), Schema::get$ref);
                        parameterModels = collectInputParams(jxPathContext,
                            refsToExpand,
                            parameterModels,
                            mediaType,
                            resSchema);
                    }
                }
            }
        }
        if (parameterModels.size() > MAX_PARAMETERS_COUNT) {
            DatatypeModel dt = new DatatypeModel(getDataTypeName(path));
            List<FieldModel> fields = new ArrayList<>();
            for (InputParameter parameterModel : parameterModels) {
                FieldModel fm = new FieldModel.Builder().setName(parameterModel.getName())
                    .setType(parameterModel.getType())
                    .setDefaultValue(null)
                    .build();
                fields.add(fm);
            }
            dt.setFields(fields);
            dts.add(dt);
            parameterModels = Collections
                .singletonList(new ParameterModel(dt.getName(), StringUtils.uncapitalize(dt.getName())));
        }
        return parameterModels;
    }

    private static String getDataTypeName(String path) {
        return normalizeName(path) + "Request";
    }

    public static String normalizeName(String originalName) {
        StringBuilder resultName = new StringBuilder();
        char[] chars = originalName.toCharArray();
        if (Character.isJavaIdentifierStart(chars[0])) {
            resultName.append(chars[0]);
        }
        for (int i = 1; i < chars.length; i++) {
            if (Character.isJavaIdentifierPart(chars[i])) {
                resultName.append(chars[i]);
            }
        }
        return resultName.toString();
    }

    private static List<InputParameter> collectInputParams(JXPathContext jxPathContext,
            Set<String> refsToExpand,
            List<InputParameter> parameterModels,
            MediaType mediaType,
            Schema<?> resSchema) {
        if (resSchema != null) {
            // search for refsToExpandInside
            // go through the schema and all the parameters
            Set<String> allSchemasUsedInRequest = visitSchema(jxPathContext, resSchema, false, true);
            boolean requestBodyHasExpandableParam = allSchemasUsedInRequest.stream().anyMatch(refsToExpand::contains);
            if (requestBodyHasExpandableParam) {
                for (String internalModel : allSchemasUsedInRequest) {
                    refsToExpand.remove(internalModel);
                }
            }
            String ref = mediaType.getSchema().get$ref();
            // only root schema is expandable
            if (ref != null && refsToExpand.contains(ref)) {
                Map<String, Schema> properties = resSchema.getProperties();
                if (CollectionUtils.isNotEmpty(properties)) {
                    if (properties.size() > MAX_PARAMETERS_COUNT) {
                        String name = getSimpleName(ref);
                        parameterModels = Collections.singletonList(
                            new ParameterModel(StringUtils.capitalize(name), StringUtils.uncapitalize(name)));
                        refsToExpand.remove(ref);
                    } else {
                        parameterModels = properties.entrySet()
                            .stream()
                            .map(OpenLOpenAPIUtils::extractParameter)
                            .collect(Collectors.toList());
                    }
                }
            } else {
                // non expandable
                String name = extractType(mediaType.getSchema());
                if (StringUtils.isBlank(name)) {
                    parameterModels = Collections.emptyList();
                } else {
                    parameterModels = Collections.singletonList(
                        new ParameterModel(StringUtils.capitalize(name), StringUtils.uncapitalize(name)));
                }
            }
        }
        return parameterModels;
    }

    public static ParameterModel extractParameter(Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();
        String typeModel = OpenLOpenAPIUtils.extractType(valueSchema);
        return new ParameterModel(typeModel, propertyName);
    }

}
