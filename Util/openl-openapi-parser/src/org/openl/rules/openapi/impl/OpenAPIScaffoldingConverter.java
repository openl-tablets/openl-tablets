package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSimpleName;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.normalizeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.jxpath.JXPathContext;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenAPIScaffoldingConverter implements OpenAPIModelConverter {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";
    public static final String RESULT = "Result";

    private boolean generateUnusedModels = true;

    public OpenAPIScaffoldingConverter() {
    }

    public OpenAPIScaffoldingConverter(boolean generateUnusedModels) {
        this.generateUnusedModels = generateUnusedModels;
    }

    @Override
    public ProjectModel extractProjectModel(String pathTo) {
        ParseOptions options = OpenLOpenAPIUtils.getParseOptions();
        OpenAPI openAPI = new OpenAPIV3Parser().read(pathTo, null, options);
        JXPathContext jxPathContext = JXPathContext.newContext(openAPI);

        String projectName = openAPI.getInfo().getTitle();
        Map<String, Integer> allUsedSchemaRefs = OpenLOpenAPIUtils
            .getAllUsedSchemaRefs(openAPI, jxPathContext, OpenLOpenAPIUtils.PathTarget.ALL);

        Map<String, Integer> allUsedSchemaRefsInRequests = OpenLOpenAPIUtils
            .getAllUsedSchemaRefs(openAPI, jxPathContext, OpenLOpenAPIUtils.PathTarget.REQUESTS);

        Set<String> allUnusedRefs = OpenLOpenAPIUtils.getUnusedSchemaRefs(openAPI, allUsedSchemaRefs.keySet());

        Map<String, List<String>> childrenSchemas = OpenLOpenAPIUtils.getChildrenMap(openAPI);
        Set<String> parents = childrenSchemas.keySet();

        Set<String> refsWhichAreFields = OpenLOpenAPIUtils.getRefsUsedInTypes(openAPI);

        // all the requests which were used only once per project needed to be extracted
        // if it's extends from other model it will be an inline type
        Set<String> refsToExpand = allUsedSchemaRefsInRequests.entrySet()
            .stream()
            .filter(x -> x.getValue()
                .equals(1) && (!allUsedSchemaRefs.containsKey(x.getKey()) || allUsedSchemaRefs.get(x.getKey())
                    .equals(1)) && !parents.contains(x.getKey()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // path + schemas
        Map<String, Set<String>> allRefsInResponses = OpenLOpenAPIUtils.getAllUsedRefResponses(openAPI, jxPathContext);

        // all the paths which have primitive responses are possible spreadsheets too
        Set<String> primitiveReturnsPaths = allRefsInResponses.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // searching for paths which response models are not included in ANY requestBody
        Map<String, Set<String>> pathWithPotentialSprResult = allRefsInResponses.entrySet()
            .stream()
            .filter(entry -> !entry.getValue().isEmpty() && entry.getValue()
                .stream()
                .noneMatch(allUsedSchemaRefsInRequests::containsKey))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> spreadsheetPaths = allRefsInResponses.keySet()
            .stream()
            .filter(x -> !pathWithPotentialSprResult.containsKey(x) && !primitiveReturnsPaths.contains(x))
            .collect(Collectors.toSet());

        Set<String> spreadsheetResultRefs = pathWithPotentialSprResult.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        List<DatatypeModel> dts = new ArrayList<>();
        List<SpreadsheetModel> spreadsheetModels = extractSprModels(openAPI,
            jxPathContext,
            pathWithPotentialSprResult.keySet(),
            primitiveReturnsPaths,
            refsToExpand,
            spreadsheetPaths,
            dts);

        Set<String> datatypeRefs = allUsedSchemaRefs.keySet()
            .stream()
            .filter(
                x -> !spreadsheetResultRefs.contains(x) && !refsToExpand.contains(x) || refsWhichAreFields.contains(x))
            .collect(Collectors.toSet());

        dts.addAll(extractDataTypeModels(openAPI, datatypeRefs, false));
        if (generateUnusedModels) {
            dts.addAll(extractDataTypeModels(openAPI, allUnusedRefs, true));
        }
        fillSprValues(spreadsheetModels);
        return new ProjectModel(projectName, dts, spreadsheetModels);
    }

    private void fillSprValues(List<SpreadsheetModel> spreadsheetModels) {
        Map<String, List<InputParameter>> sprTypeNames = spreadsheetModels.stream()
            .collect(Collectors.toMap(SpreadsheetModel::getName, SpreadsheetModel::getParameters));
        for (SpreadsheetModel spreadsheetModel : spreadsheetModels) {
            for (StepModel step : spreadsheetModel.getSteps()) {
                String type = step.getType().replaceAll("[\\[\\]]", "");
                if (sprTypeNames.containsKey(type)) {
                    List<InputParameter> inputParameters = sprTypeNames.get(type);
                    String value = String.join(",", Collections.nCopies(inputParameters.size(), "null"));
                    step.setValue(makeCall(type, value));
                }
            }
        }
    }

    private List<SpreadsheetModel> extractSprModels(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Set<String> pathWithPotentialSprResult,
            Set<String> pathsWithPrimitiveReturns,
            Set<String> refsToExpand,
            Set<String> pathsWithSpreadsheets,
            List<DatatypeModel> dts) {
        List<SpreadsheetModel> spreadSheetModels = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (String path : pathWithPotentialSprResult) {
                PathItem pathItem = paths.get(path);
                if (pathItem != null) {
                    SpreadsheetModel spr = extractSpreadsheetModel(openAPI,
                        jxPathContext,
                        pathItem,
                        path,
                        refsToExpand,
                        PathType.SPREADSHEET_RESULT_PATH,
                        dts);
                    spreadSheetModels.add(spr);
                }
            }
            for (String p : pathsWithPrimitiveReturns) {
                PathItem pathItem = paths.get(p);
                if (pathItem != null) {
                    SpreadsheetModel spreadsheetModel = extractSpreadsheetModel(openAPI,
                        jxPathContext,
                        pathItem,
                        p,
                        refsToExpand,
                        PathType.SIMPLE_RETURN_PATH,
                        dts);
                    spreadSheetModels.add(spreadsheetModel);
                }
            }
            for (String spreadsheet : pathsWithSpreadsheets) {
                PathItem pathItem = paths.get(spreadsheet);
                if (pathItem != null) {
                    SpreadsheetModel spreadsheetModel = extractSpreadsheetModel(openAPI,
                        jxPathContext,
                        pathItem,
                        spreadsheet,
                        refsToExpand,
                        PathType.SPREADSHEET_PATH,
                        dts);
                    spreadSheetModels.add(spreadsheetModel);
                }
            }
        }
        return spreadSheetModels;
    }

    private SpreadsheetModel extractSpreadsheetModel(OpenAPI openAPI,
            JXPathContext jxPathContext,
            PathItem pathItem,
            String path,
            Set<String> refsToExpand,
            PathType pathType,
            List<DatatypeModel> dts) {
        SpreadsheetModel spr = new SpreadsheetModel();
        String usedSchemaInResponse = OpenLOpenAPIUtils.getUsedSchemaInResponse(jxPathContext, pathItem);
        boolean isArray = usedSchemaInResponse.endsWith("[]");
        Schema<?> schema;
        List<InputParameter> parameters = OpenLOpenAPIUtils
            .extractParameters(jxPathContext, openAPI, refsToExpand, pathItem, dts, path);
        String normalizedPath = replaceBrackets(path);
        spr.setName(OpenLOpenAPIUtils.normalizeName(normalizedPath));
        spr.setParameters(parameters);

        List<StepModel> stepModels = new ArrayList<>();
        if (PathType.SPREADSHEET_RESULT_PATH.equals(pathType)) {
            String nameOfSchema = usedSchemaInResponse;
            if (isArray) {
                nameOfSchema = usedSchemaInResponse.replaceAll("[\\[\\]]", "");
            }
            schema = getSchemas(openAPI).get(nameOfSchema);
            spr.setType(SPREADSHEET_RESULT);
            if (schema != null) {
                Map<String, Schema> properties = schema.getProperties();
                if (CollectionUtils.isNotEmpty(properties)) {
                    stepModels = properties.entrySet().stream().map(this::extractStep).collect(Collectors.toList());
                }
            }
        } else if (PathType.SPREADSHEET_PATH.equals(pathType)) {
            spr.setType(usedSchemaInResponse);
            stepModels = Collections
                .singletonList(new StepModel(RESULT, usedSchemaInResponse, "", makeValue(usedSchemaInResponse)));
        } else {
            spr.setType(usedSchemaInResponse);
            String normalizedName = normalizeName(path);
            stepModels = Collections.singletonList(
                new StepModel(normalizedName, usedSchemaInResponse, "", makeValue(usedSchemaInResponse)));
        }
        spr.setSteps(stepModels);

        return spr;
    }

    private String replaceBrackets(String path) {
        return path.replaceAll("\\{.*?}", "");
    }

    private List<DatatypeModel> extractDataTypeModels(OpenAPI openAPI,
            Set<String> allTheRefsWhichAreDatatypes,
            boolean unused) {
        List<DatatypeModel> result = new ArrayList<>();
        for (String datatypeRef : allTheRefsWhichAreDatatypes) {
            String schemaName;
            if (unused) {
                schemaName = datatypeRef;
            } else {
                schemaName = getSimpleName(datatypeRef);
            }
            Schema<?> schema = getSchemas(openAPI).get(schemaName);
            if (schema != null) {
                DatatypeModel dm = createModel(openAPI, schemaName, schema);
                result.add(dm);
            }
        }
        return result;
    }

    private DatatypeModel createModel(OpenAPI openAPI, String schemaName, Schema<?> schema) {
        DatatypeModel dm = new DatatypeModel(normalizeName(schemaName));
        Map<String, Schema> properties;
        List<FieldModel> fields = new ArrayList<>();
        if (schema instanceof ComposedSchema) {
            String parentName = OpenLOpenAPIUtils.getParentName((ComposedSchema) schema, openAPI);
            properties = OpenLOpenAPIUtils.getFieldsOfChild((ComposedSchema) schema);
            dm.setParent(parentName);
        } else {
            properties = schema.getProperties();
        }
        if (properties != null) {
            for (Map.Entry<String, Schema> property : properties.entrySet()) {
                fields.add(extractField(property));
            }
        }
        dm.setFields(fields);
        return dm;
    }

    private FieldModel extractField(Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();

        String typeModel = OpenLOpenAPIUtils.extractType(valueSchema);
        Object defaultValue = valueSchema.getDefault();

        return new FieldModel.Builder().setName(propertyName).setType(typeModel).setDefaultValue(defaultValue).build();
    }

    private StepModel extractStep(Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();
        String typeModel = OpenLOpenAPIUtils.extractType(valueSchema);
        String value = makeValue(typeModel);
        return new StepModel(normalizeName(propertyName), typeModel, "", value);
    }

    private String makeValue(String type) {
        String result = "";
        if (StringUtils.isNotBlank(type)) {
            if (isSimpleType(type)) {
                if ("Integer".equals(type)) {
                    result = "=0";
                } else if ("Long".equals(type)) {
                    result = "=0L";
                } else if ("Double".equals(type)) {
                    result = "=0.0d";
                } else if ("Float".equals(type)) {
                    result = "=0.0f";
                } else if ("String".equals(type)) {
                    result = "=" + "\"\"";
                } else if ("Date".equals(type)) {
                    result = "=new Date()";
                } else if ("OffsetDateTime".equals(type)) {
                    result = "=java.time.OffsetDateTime.now()";
                } else if ("Boolean".equals(type)) {
                    result = "=false";
                } else if ("Object".equals(type)) {
                    result = "=new Object()";
                }
            } else {
                result = createNewInstance(type);
            }
        }
        return result;
    }

    private boolean isSimpleType(String type) {
        return "String".equals(type) || "Float".equals(type) || "Double".equals(type) || "Integer"
            .equals(type) || "Long".equals(type) || "Boolean"
                .equals(type) || "Date".equals(type) || "OffsetDateTime".equals(type) || "Object".equals(type);
    }

    private String createNewInstance(String type) {
        StringBuilder result = new StringBuilder().append("=").append("new ").append(type);
        if (type.endsWith("[]")) {
            result.append("{}");
        } else {
            result.append("()");
        }
        return result.toString();
    }

    private String makeCall(String type, String value) {
        return "=" + type + "(" + value + ")";
    }
}
