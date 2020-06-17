package org.openl.rules.openapi.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.parser.util.RefUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

/**
 * Extracts data types from OpenAPI JSON files
 */
public class OpenAPIToExcelModelConverter implements OpenAPIModelConverter {

    private final Logger logger = LoggerFactory.getLogger(OpenAPIToExcelModelConverter.class);

    public static final String HTTP_SUCCESS_CODE = "200";

    private final static String INTEGER_TYPE = "integer";
    private final static String NUMBER_TYPE = "number";
    private final static String STRING_TYPE = "string";
    private final static String BOOLEAN_TYPE = "boolean";

    private final static Set<String> DEFAULT_TYPES = new HashSet<>(
        Arrays.asList(INTEGER_TYPE, NUMBER_TYPE, STRING_TYPE, BOOLEAN_TYPE));

    @Override
    public ProjectModel extractProjectModel(String pathTo) {
        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setFlatten(true);
            OpenAPI openAPI = new OpenAPIV3Parser().read(pathTo, null, options);
            String projectName = openAPI.getInfo().getTitle();

            Map<String, Set<String>> allTypesMap = new HashMap<>();
            Map<String, Integer> requestsTypes = new HashMap<>();
            Set<String> responsesTypes = new HashSet<>();
            // Extracting datatype models. Also we're filling the map with all types which were defined with their
            // dependencies
            // TODO: now it stores the "clear" name - possible problem - when the reference type and simple type are
            // named equally. Is it possible?
            List<DatatypeModel> dataTypes = extractDatatypes(openAPI, allTypesMap);
            extractRequestsAndResponses(openAPI, requestsTypes, responsesTypes, allTypesMap);
            // This types are used only in requests -> we need to remove them from data types and expand at methods
            // signature
            Set<String> typesToExpand = extractTypesToExpand(requestsTypes);
            // Extracting spreadsheetResults -> we're removing returning types which were used in requests
            Set<String> possibleSpreadSheetResults = removeRequestTypes(requestsTypes, responsesTypes);

            List<SpreadsheetResultModel> spreadsheetResultModels = extractSprModels(openAPI, dataTypes);

            return new ProjectModel(projectName, dataTypes, spreadsheetResultModels);
        } catch (Exception e) {
            logger.error("Something went wrong", e);
        }
        return null;
    }

    private Set<String> removeRequestTypes(Map<String, Integer> requestsTypes, Set<String> responsesTypes) {
        Set<String> result = new HashSet<>(responsesTypes);
        result.removeIf(x -> requestsTypes.containsKey(x) && !DEFAULT_TYPES.contains(x));
        return result;
    }

    private Set<String> extractTypesToExpand(Map<String, Integer> requestsTypes) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Integer> requestEntry : requestsTypes.entrySet()) {
            // request was met only once
            if (requestEntry.getValue().equals(1)) {
                result.add(requestEntry.getKey());
            }
        }
        return result;
    }

    private void extractRequestsAndResponses(OpenAPI openAPI,
            Map<String, Integer> requestsTypes,
            Set<String> responses,
            Map<String, Set<String>> allTypesMap) {
        Paths paths = openAPI.getPaths();
        for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
            PathItem value = pathEntry.getValue();
            List<Operation> operations = value.readOperations();
            if (operations.size() > 1) {
                throw new UnsupportedOperationException("There are more than 1 http method");
            }
            if (operations.isEmpty()) {
                logger.info("There were no operations in OpenAPI scheme");
                return;
            }
            Optional<Operation> operation = operations.stream().findFirst();
            Operation existingOperation = Optional.of(operation).get().orElse(null);
            if (existingOperation == null) {
                throw new IllegalStateException("");
            }
            RequestBody requestBody = existingOperation.getRequestBody();
            if (requestBody != null) {
                Content bodyContent = requestBody.getContent();

                boolean applicationJson = bodyContent.containsKey("application/json");
                boolean textPlain = bodyContent.containsKey("text/plain");
                if (applicationJson) {
                    MediaType mediaType = bodyContent.get("application/json");
                    String typeName = extractTypeName(mediaType.getSchema()).replaceAll("\\p{P}", "");
                    requestsTypes.merge(typeName, 1, Integer::sum);
                    Set<String> innerTypes = collectInnerTypes(typeName, allTypesMap);
                    for (String innerType : innerTypes) {
                        requestsTypes.merge(innerType, 1, Integer::sum);
                    }
                }
            }
            ApiResponses apiResponses = existingOperation.getResponses();
            ApiResponse defaultResponse = apiResponses.getDefault();
            ApiResponse successResponse = apiResponses.get(HTTP_SUCCESS_CODE);
            // TODO situation with not existing default and success responses
            Content content = successResponse == null ? defaultResponse.getContent() : successResponse.getContent();
            // TODO: mediatypes?
            for (Map.Entry<String, MediaType> stringMediaTypeEntry : content.entrySet()) {
                responses.add(extractTypeName(stringMediaTypeEntry.getValue().getSchema()).replaceAll("\\p{P}", ""));
            }
        }

    }

    private Set<String> collectInnerTypes(String typeName, Map<String, Set<String>> allTypesMap) {
        Set<String> innerTypes = new HashSet<>();
        collectInnerTypes(typeName, allTypesMap, innerTypes);
        return innerTypes;
    }

    private void collectInnerTypes(String typeName, Map<String, Set<String>> allTypesMap, Set<String> innerTypes) {
        if (allTypesMap.containsKey(typeName)) {
            Set<String> inner = allTypesMap.get(typeName);
            if (!inner.isEmpty()) {
                innerTypes.addAll(inner);
                for (String s : inner) {
                    collectInnerTypes(s, allTypesMap, innerTypes);
                }
            }
        }
    }

    private List<SpreadsheetResultModel> extractSprModels(OpenAPI openAPI, List<DatatypeModel> dataTypes) {
        Paths paths = openAPI.getPaths();
        List<SpreadsheetResultModel> resultModels = new ArrayList<>();

        Set<String> sprTypes = new HashSet<>();

        for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
            PathItem item = pathEntry.getValue();
            for (Operation operation : item.readOperations()) {
                ApiResponses responses = operation.getResponses();
                ApiResponse defaultResponse = responses.getDefault();
                ApiResponse successResponse = responses.get(HTTP_SUCCESS_CODE);
                Content content = defaultResponse == null ? successResponse.getContent() : defaultResponse.getContent();
                RequestBody requestBody = operation.getRequestBody();
                for (Map.Entry<String, MediaType> stringMediaTypeEntry : content.entrySet()) {
                    Schema schema = stringMediaTypeEntry.getValue().getSchema();
                    String type = extractTypeName(schema);
                    // todo
                    if (true) {
                        SpreadsheetResultModel spreadsheetResultModel = new SpreadsheetResultModel();
                        if (requestBody != null) {
                            Content bodyContent = requestBody.getContent();
                            for (Map.Entry<String, MediaType> requestEntry : bodyContent.entrySet()) {
                                spreadsheetResultModel.setSignature(
                                    type + "(" + extractTypeName(requestEntry.getValue().getSchema()) + ")");
                            }
                        }
                        Optional<DatatypeModel> datatypeResponseModel = dataTypes.stream()
                            .filter(x -> x.getName().equals(type))
                            .findFirst();
                        if (datatypeResponseModel.isPresent()) {
                            sprTypes.add(type);
                            spreadsheetResultModel.setModel(datatypeResponseModel.get());
                        }
                        resultModels.add(spreadsheetResultModel);
                    }
                }
            }
        }
        return resultModels;
    }

    private List<DatatypeModel> extractDatatypes(OpenAPI openAPI, Map<String, Set<String>> allTypesMap) {
        List<DatatypeModel> result = new ArrayList<>();
        Map<String, Schema> schemaMap = openAPI.getComponents().getSchemas();

        for (Map.Entry<String, Schema> schemaEntry : schemaMap.entrySet()) {
            String requestName = schemaEntry.getKey();
            DatatypeModel datatype = new DatatypeModel(requestName);
            Schema value = schemaEntry.getValue();

            Set<String> types = new HashSet<>();

            boolean outsideType = !(value instanceof ObjectSchema || value.getClass().equals(Schema.class));
            if (outsideType) {
                FieldModel fieldModel = extractFieldModel(schemaEntry);
                datatype.setFields(Collections.singletonList(fieldModel));
                result.add(datatype);
                types.add(fieldModel.getType());
                continue;
            }
            List<FieldModel> fields = new ArrayList<>();
            Map<String, Schema> properties = value.getProperties();
            for (Map.Entry<String, Schema> property : properties.entrySet()) {
                FieldModel f = extractFieldModel(property);
                fields.add(f);
                types.add(f.getType().replaceAll("\\p{P}", ""));
            }
            datatype.setFields(fields);
            result.add(datatype);
            allTypesMap.put(requestName, types);
        }
        return result;
    }

    private FieldModel extractFieldModel(final Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema valueSchema = property.getValue();

        String propertyType = extractTypeName(valueSchema);
        Object defaultValue = valueSchema.getDefault();
        String format = valueSchema.getFormat();

        return new FieldModel.Builder().setName(propertyName)
            .setType(propertyType)
            .setDefaultValue(defaultValue)
            .setFormat(format)
            .build();
    }

    private Set<String> extractTypeNames(Schema valueSchema) {
        Set<String> result = new HashSet<>();
        if (valueSchema instanceof ComposedSchema) {
            ComposedSchema schema = (ComposedSchema) valueSchema;
            List<Schema> allOf = schema.getAllOf();
            for (Schema allOfSchema : allOf) {
                result.add(extractTypeName(allOfSchema).replaceAll("\\p{P}", ""));
            }
            List<Schema> anyOf = schema.getAnyOf();
            for (Schema anyOfSchema : anyOf) {
                result.add(extractTypeName(anyOfSchema).replaceAll("\\p{P}", ""));
            }
            List<Schema> oneOf = schema.getOneOf();
            for (Schema oneOfSchema : oneOf) {
                result.add(extractTypeName(oneOfSchema).replaceAll("\\p{P}", ""));
            }
        } else {
            result.add(extractTypeName(valueSchema).replaceAll("\\p{P}", ""));
        }
        return result;
    }

    private String extractTypeName(Schema valueSchema) {
        String result;
        if (valueSchema.getType() != null) {
            String type = valueSchema.getType();
            // arrays
            if (valueSchema instanceof ArraySchema) {
                ArraySchema arraySchema = (ArraySchema) valueSchema;
                Schema<?> itemsSchema = arraySchema.getItems();
                if (itemsSchema.getType() != null) {
                    type = itemsSchema.getType();
                } else {
                    type = RefUtils.computeDefinitionName(itemsSchema.get$ref());
                }
                result = type + "[]";
            } else {
                // primitives
                result = type;
            }
        } else {
            // custom objects
            result = RefUtils.computeDefinitionName(valueSchema.get$ref());
        }
        // capitalize first letter was there
        // StringUtils.capitalize(result);
        return result;
    }

}
