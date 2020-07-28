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

import org.apache.commons.jxpath.JXPathContext;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenAPIScaffoldingConverter implements OpenAPIModelConverter {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";

    private final Logger logger = LoggerFactory.getLogger(OpenAPIScaffoldingConverter.class);

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

        Set<String> spreadsheetResultRefs = pathWithPotentialSprResult.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        List<DatatypeModel> dts = new ArrayList<>();
        List<SpreadsheetResultModel> spreadsheetResultModels = extractSprModels(openAPI,
            jxPathContext,
            pathWithPotentialSprResult.keySet(),
            primitiveReturnsPaths,
            refsToExpand,
            dts);

        Set<String> datatypeRefs = allUsedSchemaRefs.keySet()
            .stream()
            .filter(x -> !spreadsheetResultRefs.contains(x) && !refsToExpand.contains(x))
            .collect(Collectors.toSet());

        dts.addAll(extractDataTypeModels(openAPI, datatypeRefs, false));
        if (generateUnusedModels) {
            dts.addAll(extractDataTypeModels(openAPI, allUnusedRefs, true));
        }
        fillSprValues(spreadsheetResultModels);
        return new ProjectModel(projectName, dts, spreadsheetResultModels);
    }

    private void fillSprValues(List<SpreadsheetResultModel> spreadsheetResultModels) {
        Map<String, List<InputParameter>> sprTypeNames = spreadsheetResultModels.stream()
            .collect(Collectors.toMap(SpreadsheetResultModel::getName, SpreadsheetResultModel::getParameters));
        for (SpreadsheetResultModel spreadsheetResultModel : spreadsheetResultModels) {
            for (StepModel step : spreadsheetResultModel.getSteps()) {
                String type = step.getType();
                if (sprTypeNames.containsKey(type)) {
                    List<InputParameter> inputParameters = sprTypeNames.get(type);
                    String value = String.join(",", Collections.nCopies(inputParameters.size(), "null"));
                    step.setValue(value);
                }
            }
        }
    }

    private List<SpreadsheetResultModel> extractSprModels(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Set<String> pathWithPotentialSprResult,
            Set<String> pathsWithPrimitiveReturns,
            Set<String> refsToExpand,
            List<DatatypeModel> dts) {
        List<SpreadsheetResultModel> spreadSheetModels = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (String path : pathWithPotentialSprResult) {
                PathItem pathItem = paths.get(path);
                if (pathItem != null) {
                    SpreadsheetResultModel spr = extractSpreadsheetModel(openAPI,
                        jxPathContext,
                        pathItem,
                        path,
                        refsToExpand,
                        false,
                        dts);
                    spreadSheetModels.add(spr);
                }
            }
            for (String p : pathsWithPrimitiveReturns) {
                PathItem pathItem = paths.get(p);
                if (pathItem != null) {
                    SpreadsheetResultModel spreadsheetResultModel = extractSpreadsheetModel(openAPI,
                        jxPathContext,
                        pathItem,
                        p,
                        refsToExpand,
                        true,
                        dts);
                    spreadSheetModels.add(spreadsheetResultModel);
                }
            }
        }
        return spreadSheetModels;
    }

    private SpreadsheetResultModel extractSpreadsheetModel(OpenAPI openAPI,
            JXPathContext jxPathContext,
            PathItem pathItem,
            String path,
            Set<String> refsToExpand,
            boolean isSimplePath,
            List<DatatypeModel> dts) {
        SpreadsheetResultModel spr = new SpreadsheetResultModel();
        String usedSchemaInResponse = OpenLOpenAPIUtils.getUsedSchemaInResponse(jxPathContext, pathItem);
        Schema<?> schema;
        List<InputParameter> parameters = OpenLOpenAPIUtils
            .extractParameters(jxPathContext, refsToExpand, pathItem, dts, path);
        spr.setName(OpenLOpenAPIUtils.normalizeName(path));
        spr.setParameters(parameters);

        List<StepModel> stepModels = new ArrayList<>();
        if (!isSimplePath) {
            schema = getSchemas(openAPI).get(usedSchemaInResponse);
            spr.setType(SPREADSHEET_RESULT);
            if (schema != null) {
                Map<String, Schema> properties = schema.getProperties();
                if (CollectionUtils.isNotEmpty(properties)) {
                    stepModels = properties.entrySet().stream().map(this::extractStep).collect(Collectors.toList());
                }
            }
        } else {
            spr.setType(usedSchemaInResponse);
            String stepName = StringUtils.uncapitalize(path.substring(1));
            stepModels = Collections.singletonList(new StepModel(stepName, usedSchemaInResponse, "", 0));
        }
        spr.setSteps(stepModels);

        return spr;
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
        DatatypeModel dm = new DatatypeModel(StringUtils.capitalize(schemaName));
        if (schema instanceof ComposedSchema) {
            String parentName = OpenLOpenAPIUtils.getParentName((ComposedSchema) schema, openAPI);
            dm.setParent(parentName);
        }
        List<FieldModel> fields = new ArrayList<>();
        Map<String, Schema> properties = schema.getProperties();
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
        return new StepModel(propertyName, typeModel, "", null);
    }

}
