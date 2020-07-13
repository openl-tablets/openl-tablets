package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSimpleName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenAPIExperiments implements OpenAPIModelConverter {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";
    private final Logger logger = LoggerFactory.getLogger(OpenAPIExperiments.class);

    private boolean generateUnusedModels = true;

    public OpenAPIExperiments() {
    }

    public OpenAPIExperiments(boolean generateUnusedModels) {
        this.generateUnusedModels = generateUnusedModels;
    }

    @Override
    public ProjectModel extractProjectModel(String pathTo) {
        ParseOptions options = OpenLOpenAPIUtils.getParseOptions();
        OpenAPI openAPI = new OpenAPIV3Parser().read(pathTo, null, options);

        String projectName = openAPI.getInfo().getTitle();

        Map<String, Integer> allUsedSchemaRefs = OpenLOpenAPIUtils.getAllUsedSchemaRefs(openAPI, OpenLOpenAPIUtils.Path.ALL);
        Map<String, Integer> allUsedSchemaRefsInRequests = OpenLOpenAPIUtils.getAllUsedSchemaRefs(openAPI,
            OpenLOpenAPIUtils.Path.REQUESTS);
        Set<String> allUnusedRefs = OpenLOpenAPIUtils.getUnusedSchemaRefs(openAPI, allUsedSchemaRefs.keySet());

        // all the requests which were used only once needed to be extracted
        Set<String> refsToExpand = allUsedSchemaRefsInRequests.entrySet()
            .stream()
            .filter(x -> x.getValue().equals(1) && (!allUsedSchemaRefs.containsKey(x.getKey()) ||
                    allUsedSchemaRefs.get(x.getKey()).equals(1)))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        Map<String, Set<String>> allSchemaRefResponses = OpenLOpenAPIUtils.getAllUsedRefResponses(openAPI);

        // paths with responses with primitive returns are possible spreadsheetResults too
        Set<String> primitiveReturnsPaths = allSchemaRefResponses.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // check path returns are containing in requests? no -> potential spr!!
        Map<String, Set<String>> pathWithPotentialSprResult = allSchemaRefResponses.entrySet()
            .stream()
            .filter(entry -> !entry.getValue().isEmpty() && entry.getValue()
                .stream()
                .noneMatch(allUsedSchemaRefsInRequests::containsKey))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> sprResultRefs = pathWithPotentialSprResult.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Set<String> allTheRefsWhichAreDatatypes = allUsedSchemaRefs.keySet()
            .stream()
            .filter(x -> !sprResultRefs.contains(x) && !refsToExpand.contains(x))
            .collect(Collectors.toSet());

        List<DatatypeModel> dts = extractDataTypeModels(openAPI, allTheRefsWhichAreDatatypes, false);
        if (generateUnusedModels) {
            dts.addAll(extractDataTypeModels(openAPI, allUnusedRefs, true));
        }
        List<SpreadsheetResultModel> spreadsheetResultModels = extractSprModels(openAPI,
            pathWithPotentialSprResult.keySet(),
            primitiveReturnsPaths,
            refsToExpand);

        return new ProjectModel(projectName, dts, spreadsheetResultModels);
    }

    private List<SpreadsheetResultModel> extractSprModels(OpenAPI openAPI,
                                                          Set<String> pathWithPotentialSprResult,
                                                          Set<String> pathsWithPrimitiveReturns,
                                                          Set<String> refsToExpand) {

        List<SpreadsheetResultModel> spreadSheetModels = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (String path : pathWithPotentialSprResult) {
                PathItem pathItem = paths.get(path);
                if (pathItem != null) {
                    SpreadsheetResultModel spreadsheetResultModel = new SpreadsheetResultModel();
                    String signature = extractSignature(openAPI, refsToExpand, pathItem);
                    spreadsheetResultModel.setSignature(signature);
                    spreadsheetResultModel.setName(path.substring(1));
                    DatatypeModel type = extractType(openAPI, pathItem);
                    if (type != null) {
                        spreadsheetResultModel.setType(SPREADSHEET_RESULT);
                        spreadsheetResultModel.setSteps(type.getFields());
                    }
                    spreadSheetModels.add(spreadsheetResultModel);
                }
            }
            for (String p : pathsWithPrimitiveReturns) {
                PathItem pathItem = paths.get(p);
                if (pathItem != null) {
                    SpreadsheetResultModel spreadsheetResultModel = new SpreadsheetResultModel();
                    String signature = extractSignature(openAPI, refsToExpand, pathItem);
                    spreadsheetResultModel.setSignature(signature);
                    spreadsheetResultModel.setName(p.substring(1));
                    DatatypeModel type = extractSimpleType(openAPI, pathItem, p);
                    if (type != null) {
                        spreadsheetResultModel.setType(type.getName());
                        spreadsheetResultModel.setSteps(type.getFields());
                    }
                    spreadSheetModels.add(spreadsheetResultModel);
                }
            }
        }
        return spreadSheetModels;
    }

    private DatatypeModel extractSimpleType(OpenAPI openAPI, PathItem pathItem, String p) {
        DatatypeModel dt = null;
        String usedSchemaInResponse = OpenLOpenAPIUtils.getUsedSchemaInResponse(openAPI, pathItem);
        if (usedSchemaInResponse != null) {
            dt = new DatatypeModel(usedSchemaInResponse);
            dt.setFields(
                Collections.singletonList(new FieldModel.Builder().setName(StringUtils.uncapitalize(p.substring(1)))
                    .setType(usedSchemaInResponse)
                    .setDefaultValue(0)
                    .build()));
        }
        return dt;
    }

    private DatatypeModel extractType(OpenAPI openAPI, PathItem pathItem) {
        DatatypeModel dt = null;
        String usedSchemaInResponse = OpenLOpenAPIUtils.getUsedSchemaInResponse(openAPI, pathItem);
        if (usedSchemaInResponse != null) {
            Schema<?> schema = getSchemas(openAPI).get(usedSchemaInResponse);
            if (schema != null) {
                dt = new DatatypeModel(usedSchemaInResponse);
                Map<String, Schema> properties = schema.getProperties();
                List<FieldModel> fieldModels = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(properties)) {
                    fieldModels = properties.entrySet().stream().map(this::extractField).collect(Collectors.toList());
                }
                dt.setFields(fieldModels);
            }
        }
        return dt;
    }

    private String extractSignature(OpenAPI openAPI, Set<String> refsToExpand, PathItem pathItem) {
        String usedSchemaInRequest = OpenLOpenAPIUtils.getUsedSchemaInRequest(openAPI, pathItem);
        StringBuilder signature = new StringBuilder();
        if (usedSchemaInRequest != null) {
            if (refsToExpand.contains(usedSchemaInRequest)) {
                Schema<?> schema = getSchemas(openAPI).get(getSimpleName(usedSchemaInRequest));
                if (schema != null) {
                    Map<String, Schema> properties = schema.getProperties();
                    if (CollectionUtils.isNotEmpty(properties)) {
                        String param = properties.entrySet()
                            .stream()
                            .map(this::extractField)
                            .map(x -> x.getType() + " " + x.getName())
                            .collect(Collectors.joining(", "));
                        signature.append(param);
                    }
                }
            } else {
                String formattedName = getSimpleName(usedSchemaInRequest);
                signature = new StringBuilder(formattedName + " " + StringUtils.uncapitalize(formattedName));
            }
        }
        return signature.toString();
    }

    private List<DatatypeModel> extractDataTypeModels(OpenAPI openAPI,
                                                      Set<String> allTheRefsWhichAreDatatypes,
                                                      boolean unused) {
        List<DatatypeModel> result = new ArrayList<>();
        for (String datatypeRef : allTheRefsWhichAreDatatypes) {
            // todo it is possible to have there a response,request etc -> use resolve
            String schemaName;
            if (unused) {
                schemaName = datatypeRef;
            } else {
                schemaName = getSimpleName(datatypeRef);
            }
            Schema<?> schema = getSchemas(openAPI).get(schemaName);
            if (schema != null) {
                // todo get parent name
                DatatypeModel dm = new DatatypeModel(schemaName);
                // find parent name
                // dm.setParent();
                List<FieldModel> fields = new ArrayList<>();
                Map<String, Schema> properties = schema.getProperties();
                for (Map.Entry<String, Schema> property : properties.entrySet()) {
                    FieldModel f = extractField(property);
                    fields.add(f);
                }
                dm.setFields(fields);
                result.add(dm);
            }
        }
        return result;
    }

    private FieldModel extractField(Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema valueSchema = property.getValue();

        String typeModel = OpenLOpenAPIUtils.extractType(valueSchema);
        Object defaultValue = valueSchema.getDefault();

        return new FieldModel.Builder().setName(propertyName).setType(typeModel).setDefaultValue(defaultValue).build();
    }

}
