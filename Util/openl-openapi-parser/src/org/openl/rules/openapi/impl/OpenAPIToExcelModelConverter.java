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
import java.util.stream.Collectors;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.StringUtils;
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
@Deprecated
public class OpenAPIToExcelModelConverter implements OpenAPIModelConverter {

    private final Logger logger = LoggerFactory.getLogger(OpenAPIToExcelModelConverter.class);

    public static final String HTTP_SUCCESS_CODE = "200";

    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";

    private static final String INTEGER_TYPE = "integer";
    private static final String DOUBLE_TYPE = "double";
    private static final String FLOAT_TYPE = "float";
    private static final String STRING_TYPE = "string";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final Set<String> DEFAULT_TYPES = new HashSet<>(
        Arrays.asList(INTEGER_TYPE, DOUBLE_TYPE, FLOAT_TYPE, STRING_TYPE, BOOLEAN_TYPE));

    @Override
    public ProjectModel extractProjectModel(String pathTo) {
        try {
            ParseOptions options = getParseOptions();
            OpenAPI openAPI = new OpenAPIV3Parser().read(pathTo, null, options);

            String projectName = openAPI.getInfo().getTitle();

            Map<String, Set<String>> allTypesMap = new HashMap<>();
            Map<String, Set<String>> allTypesClearMap = new HashMap<>();
            Map<String, Set<String>> refMaps = new HashMap<>();
            Map<String, Integer> requestsTypes = new HashMap<>();
            Set<String> responsesTypes = new HashSet<>();

            List<DatatypeModel> dataTypes = extractDatatypes(openAPI, allTypesMap);
            extractRequestsAndResponses(openAPI, requestsTypes, responsesTypes, allTypesMap);
            // This types are used only in requests -> we need to remove them from data types and expand at methods
            // signature
            Set<String> typesToExpand = extractTypesToExpand(requestsTypes);
            // Extracting spreadsheetResults -> we're removing returning types which were used in requests
            Set<String> possibleSpreadSheetResults = removeRequestTypes(requestsTypes, responsesTypes);

            List<DatatypeModel> filteredDatatypes = dataTypes.stream()
                .filter(x -> !possibleSpreadSheetResults.contains(x.getName()) && !typesToExpand.contains(x.getName()))
                .collect(Collectors.toList());

            List<SpreadsheetResultModel> spreadsheetResultModels = extractSprModels(openAPI,
                dataTypes,
                typesToExpand,
                possibleSpreadSheetResults,
                allTypesMap);

            return new ProjectModel(projectName, filteredDatatypes, spreadsheetResultModels);
        } catch (Exception e) {
            logger.error("Something went wrong", e);
        }
        return null;
    }

    private ParseOptions getParseOptions() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        return options;
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
            Set<String> responsesTypes,
            Map<String, Set<String>> allTypesMap) {
        Paths paths = openAPI.getPaths();
        for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
            String pathName = pathEntry.getKey();
            PathItem value = pathEntry.getValue();

            List<Operation> operations = value.readOperations();
            if (operations.size() > 1) {
                throw new UnsupportedOperationException("There are more than 1 http method in the path:" + pathName);
            }
            if (operations.isEmpty()) {
                logger.info("There were no operations in OpenAPI schema for the path: '{}', skipping it", pathName);
                continue;
            }
            Operation existingOperation = Optional.of(operations.stream().findFirst())
                .get()
                .orElseThrow(IllegalStateException::new);

            fillRequests(pathName, existingOperation, requestsTypes, allTypesMap);
            fillResponses(pathName, existingOperation, responsesTypes);
        }

    }

    private void fillResponses(String pathName, Operation existingOperation, Set<String> responsesTypes) {
        ApiResponse response = extractResponse(existingOperation.getResponses());
        if (response == null) {
            logger.warn("There are no success/default responses for the path : '{}', skipping it.", pathName);
        } else {
            Content content = response.getContent();
            MediaType mediaType = extractMediaType(content);
            if (mediaType == null) {
                logger.warn("There are no media types in response for the path : '{}', skipping it.", pathName);
            } else {
                responsesTypes.add(extractType(mediaType.getSchema()));
            }
        }
    }

    private void fillRequests(String pathName,
            Operation existingOperation,
            Map<String, Integer> requestsTypes,
            Map<String, Set<String>> allTypesMap) {
        RequestBody requestBody = existingOperation.getRequestBody();
        if (requestBody == null) {
            logger.info("Request body of path: '{}' is empty", pathName);
        } else {
            Content bodyContent = requestBody.getContent();
            MediaType mediaType = extractMediaType(bodyContent);
            if (mediaType == null) {
                logger.warn("There are no media types in request body for the path : '{}', skipping it.", pathName);
            } else {
                String typeName = extractType(mediaType.getSchema());
                requestsTypes.merge(typeName, 1, Integer::sum);
                Set<String> innerTypes = collectInnerTypes(typeName, allTypesMap);
                for (String innerType : innerTypes) {
                    requestsTypes.merge(innerType, 1, Integer::sum);
                }
            }
        }
    }

    private ApiResponse extractResponse(ApiResponses apiResponses) {
        ApiResponse successResponse = apiResponses.get(HTTP_SUCCESS_CODE);
        ApiResponse defaultResponse = apiResponses.getDefault();
        return successResponse == null ? defaultResponse : successResponse;
    }

    /**
     * Extracting media type schema from body. Searching for application/json, if not found searching for text/plain.
     * Both aren't presented -> using first found.
     * 
     * @param bodyContent - body content
     * @return media type schema
     */
    private MediaType extractMediaType(final Content bodyContent) {
        boolean applicationJson = bodyContent.containsKey(APPLICATION_JSON);
        boolean textPlain = bodyContent.containsKey(TEXT_PLAIN);
        MediaType mediaType;
        if (applicationJson) {
            mediaType = bodyContent.get(APPLICATION_JSON);
        } else if (textPlain) {
            mediaType = bodyContent.get(TEXT_PLAIN);
        } else {
            Optional<Map.Entry<String, MediaType>> first = bodyContent.entrySet().stream().findFirst();
            mediaType = first.map(Map.Entry::getValue).get();
        }
        return mediaType;
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

    private List<SpreadsheetResultModel> extractSprModels(OpenAPI openAPI,
            List<DatatypeModel> dataTypes,
            Set<String> typesToExpand,
            Set<String> possibleSpreadSheetResults,
            Map<String, Set<String>> allTypesMap) {
        Paths paths = openAPI.getPaths();
        List<SpreadsheetResultModel> resultModels = new ArrayList<>();
        for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
            PathItem item = pathEntry.getValue();
            String requestName = pathEntry.getKey();
            Operation operation = Optional.of(item.readOperations().stream().findFirst())
                .get()
                .orElseThrow(IllegalStateException::new);
            ApiResponse response = extractResponse(operation.getResponses());
            Content content = response.getContent();
            MediaType mediaType = extractMediaType(content);
            if (mediaType == null) {
                continue;
            }
            String responseTypeModel = extractType(mediaType.getSchema());
            if (possibleSpreadSheetResults.contains(responseTypeModel)) {
                SpreadsheetResultModel spreadsheetResultModel = new SpreadsheetResultModel();
                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null) {
                    Content bodyContent = requestBody.getContent();
                    MediaType bodyType = extractMediaType(bodyContent);
                    String typeModel = extractType(bodyType.getSchema());
                    String exampleParameterName = typeModel;
                    // if (typeModel) {
                    // exampleParameterName += "s";
                    // }
                    // if (typesToExpand.contains(requestType)) {
                    // // expand type there
                    // }
//                    spreadsheetResultModel.setSignature(requestName.substring(1) + "(" + StringUtils
//                        .capitalize(typeModel) + " " + StringUtils.uncapitalize(exampleParameterName) + ")");

                    // if (responseTypeModel.isArray()) {
                    // responseTypeName += "[]";
                    // }
                    spreadsheetResultModel.setType(StringUtils.capitalize(typeModel));
                    resultModels.add(spreadsheetResultModel);
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

            // second case - inline type - it will be schema
            boolean outsideType = !(value instanceof ObjectSchema || value.getClass().equals(Schema.class));
            if (outsideType) {
                // testFolderWithJsonFiles(SomeId) - IntegerSchema
                FieldModel fieldModel = extractFieldModel(schemaEntry);
                datatype.setFields(Collections.singletonList(fieldModel));
                result.add(datatype);
                allTypesMap.put(requestName, Collections.singleton(fieldModel.getType()));
                continue;
            }
            List<FieldModel> fields = new ArrayList<>();
            Set<String> types = new HashSet<>();
            Map<String, Schema> properties = value.getProperties();
            for (Map.Entry<String, Schema> property : properties.entrySet()) {
                FieldModel f = extractFieldModel(property);
                fields.add(f);
                types.add(f.getType());
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

        String typeModel = extractType(valueSchema);
        Object defaultValue = valueSchema.getDefault();

        return new FieldModel.Builder().setName(propertyName)
            .setType(typeModel)
            .setDefaultValue(defaultValue)
            .build();
    }

    private Set<String> extractTypeNames(Schema valueSchema) {
        Set<String> result = new HashSet<>();
        if (valueSchema instanceof ComposedSchema) {
            ComposedSchema schema = (ComposedSchema) valueSchema;
            List<Schema> allOf = schema.getAllOf();
            for (Schema allOfSchema : allOf) {
                result.add(extractType(allOfSchema));
            }
            List<Schema> anyOf = schema.getAnyOf();
            for (Schema anyOfSchema : anyOf) {
                result.add(extractType(anyOfSchema));
            }
            List<Schema> oneOf = schema.getOneOf();
            for (Schema oneOfSchema : oneOf) {
                result.add(extractType(oneOfSchema));
            }
        } else {
            result.add(extractType(valueSchema));
        }
        return result;
    }

    private String extractType(Schema valueSchema) {
        String typeName;
        boolean isArray = false;
        String format = valueSchema.getFormat();
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
                typeName = type;
                isArray = true;
            } else {
                // primitives
                typeName = type;
            }
        } else {
            // custom objects
            typeName = RefUtils.computeDefinitionName(valueSchema.get$ref());
        }
        if (format != null) {
            switch (format) {
                case "int32":
                    typeName = "Integer";
                    break;
                case "int64":
                    typeName = "Long";
                    break;
                case FLOAT_TYPE:
                    typeName = FLOAT_TYPE;
                    break;
                case DOUBLE_TYPE:
                    typeName = DOUBLE_TYPE;
                    break;
                default:
                    break;
            }
        }
        if (isArray) {
            typeName += "[]";
        }
        return typeName;
    }

}
