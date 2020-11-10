package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenAPITypeUtils.OBJECT;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.SCHEMAS_LINK;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.getSimpleName;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.isSimpleType;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.APPLICATION_JSON;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.TEXT_PLAIN;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.normalizeName;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.jxpath.JXPathContext;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

public class OpenAPIScaffoldingConverter implements OpenAPIModelConverter {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";
    public static final String RESULT = "Result";
    public static final String DEFAULT_RUNTIME_CONTEXT = "DefaultRulesRuntimeContext";
    public static final Pattern ARRAY_MATCHER = Pattern.compile("[\\[\\]]");
    public static final Pattern PARAMETERS_BRACKETS_MATCHER = Pattern.compile("\\{.*?}");
    private static final Set<String> IGNORED_FIELDS = Collections
        .unmodifiableSet(new HashSet<>(Collections.singletonList("@class")));

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
        if (openAPI == null) {
            throw new IllegalStateException("Error creating the project, uploaded file has invalid structure.");
        }
        JXPathContext jxPathContext = JXPathContext.newContext(openAPI);
        String projectName = openAPI.getInfo().getTitle();
        Map<String, Integer> allUsedSchemaRefs = OpenLOpenAPIUtils
            .getAllUsedSchemaRefs(openAPI, jxPathContext, OpenLOpenAPIUtils.PathTarget.ALL);

        Map<String, Integer> allUsedSchemaRefsInRequests = OpenLOpenAPIUtils
            .getAllUsedSchemaRefs(openAPI, jxPathContext, OpenLOpenAPIUtils.PathTarget.REQUESTS);

        Set<String> allUnusedRefs = OpenLOpenAPIUtils.getUnusedSchemaRefs(openAPI, allUsedSchemaRefs.keySet());

        Map<String, List<String>> childrenSchemas = OpenAPITypeUtils.getChildrenMap(openAPI);
        Set<String> parents = childrenSchemas.keySet();
        Set<String> childSet = childrenSchemas.values()
            .stream()
            .flatMap(Collection::stream)
            .map(OpenAPITypeUtils::getSimpleName)
            .collect(Collectors.toSet());

        Map<String, Set<String>> refsWithFields = OpenLOpenAPIUtils.getRefsInProperties(openAPI, jxPathContext);
        Set<String> fieldsRefs = refsWithFields.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        // all the requests which were used only once per project needed to be extracted
        // if it's extends from other model it will be an inline type
        Set<String> refsToExpand = allUsedSchemaRefsInRequests.entrySet()
            .stream()
            .filter(x -> x.getValue()
                .equals(1) && (!allUsedSchemaRefs.containsKey(x.getKey()) || allUsedSchemaRefs.get(x.getKey())
                    .equals(1)) && !parents.contains(x.getKey()) && !fieldsRefs.contains(x.getKey()))
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
        List<SpreadsheetParserModel> spreadsheetParserModels = extractSprModels(openAPI,
            jxPathContext,
            pathWithPotentialSprResult.keySet(),
            primitiveReturnsPaths,
            refsToExpand,
            spreadsheetPaths,
            dts,
            childSet);
        // find not called potential spreadsheets, which are used as steps to make them data types
        findNotCalledPotentialDataTypes(spreadsheetParserModels);
        List<String> linkedRefs = spreadsheetParserModels.stream()
            .filter(SpreadsheetParserModel::isRefIsDataType)
            .map(SpreadsheetParserModel::getReturnRef)
            .collect(Collectors.toList());
        Set<String> datatypeRefs = allUsedSchemaRefs.keySet()
            .stream()
            .filter(x -> !(spreadsheetResultRefs.contains(x) || refsToExpand.contains(x)) || linkedRefs.contains(x))
            .collect(Collectors.toSet());
        List<SpreadsheetModel> spreadsheetModels = spreadsheetParserModels.stream()
            .map(SpreadsheetParserModel::getModel)
            .collect(Collectors.toList());
        List<DataModel> dataModels = extractDataModels(spreadsheetModels, openAPI);
        Map<Boolean, List<SpreadsheetModel>> sprModelsDivided = spreadsheetModels.stream()
            .collect(Collectors.partitioningBy(x -> containsRuntimeContext(x.getParameters())));
        List<SpreadsheetModel> sprModelsWithRC = sprModelsDivided.get(Boolean.TRUE);
        boolean isRuntimeContextProvided = !sprModelsWithRC.isEmpty();
        removeContextFromParams(sprModelsWithRC);
        fillCallsInSteps(spreadsheetModels, datatypeRefs);

        List<String> allFieldsRefs = refsWithFields.entrySet()
            .stream()
            .filter(x -> datatypeRefs.contains(x.getKey()))
            .flatMap(x -> x.getValue().stream())
            .collect(Collectors.toList());

        List<String> dtToAdd = allFieldsRefs.stream()
            .filter(x -> !datatypeRefs.contains(x))
            .collect(Collectors.toList());
        datatypeRefs.addAll(dtToAdd);

        dts.addAll(extractDataTypeModels(openAPI, datatypeRefs, false));
        if (generateUnusedModels) {
            dts.addAll(extractDataTypeModels(openAPI, allUnusedRefs, true));
        }

        return new ProjectModel(projectName,
            isRuntimeContextProvided,
            dts,
            dataModels,
            isRuntimeContextProvided ? sprModelsWithRC : spreadsheetModels,
            isRuntimeContextProvided ? sprModelsDivided.get(Boolean.FALSE) : Collections.emptyList());
    }

    private List<DataModel> extractDataModels(List<SpreadsheetModel> spreadsheetModels, OpenAPI openAPI) {
        List<SpreadsheetModel> potentialDataModels = spreadsheetModels.stream()
            .filter(x -> CollectionUtils.isEmpty(x.getParameters()) || containsOnlyRuntimeContext(x.getParameters()))
            .collect(Collectors.toList());
        List<DataModel> dataModels = new ArrayList<>();
        for (SpreadsheetModel potentialDataModel : potentialDataModels) {
            String originalType = potentialDataModel.getType();
            String type = ARRAY_MATCHER.matcher(originalType).replaceAll("");
            if (!originalType.endsWith("[]") && !OpenAPITypeUtils.isCustomType(type)) {
                continue;
            }
            String operationMethod = potentialDataModel.getPathInfo().getOperation();
            // if get operation without parameters or post with only runtime context
            List<InputParameter> parameters = potentialDataModel.getParameters();
            boolean parametersNotEmpty = CollectionUtils.isNotEmpty(parameters);
            if (parameters.isEmpty() && operationMethod.equals(PathItem.HttpMethod.GET
                .name()) || parametersNotEmpty && operationMethod.equals(PathItem.HttpMethod.POST.name())) {
                spreadsheetModels.remove(potentialDataModel);
                String dataTableName = formatTableName(potentialDataModel.getName());
                dataModels.add(new DataModel(dataTableName,
                    type,
                    potentialDataModel.getPathInfo(),
                    createModel(openAPI, type, getSchemas(openAPI).get(type))));
            }
        }
        return dataModels;
    }

    private void findNotCalledPotentialDataTypes(List<SpreadsheetParserModel> spreadsheetParserModels) {
        Set<String> sprTableNames = spreadsheetParserModels.stream()
            .map(x -> x.getModel().getName())
            .collect(Collectors.toSet());
        Set<String> calledSpr = new HashSet<>();
        Set<String> stepTypes = new HashSet<>();
        Set<StepModel> allSteps = spreadsheetParserModels.stream()
            .flatMap(x -> x.getModel().getSteps().stream())
            .collect(Collectors.toSet());
        for (StepModel step : allSteps) {
            String type = ARRAY_MATCHER.matcher(step.getType()).replaceAll("");
            if (isSimpleType(type)) {
                continue;
            }
            stepTypes.add(type);
            if (sprTableNames.contains(type)) {
                calledSpr.add(type);
            }
        }
        sprTableNames.removeAll(calledSpr);
        List<SpreadsheetParserModel> pr = spreadsheetParserModels.stream().filter(x -> {
            boolean isTypeInSteps = x.getReturnRef() != null && stepTypes.contains(getSimpleName(x.getReturnRef()));
            boolean notCalled = sprTableNames.contains(x.getModel().getName());
            boolean isSprResult = x.getModel().getType().equals(SPREADSHEET_RESULT);
            return isTypeInSteps && notCalled && isSprResult;
        }).collect(Collectors.toList());
        makeReturnType(pr);

    }

    private void makeReturnType(List<SpreadsheetParserModel> models) {
        for (SpreadsheetParserModel spr : models) {
            spr.setStoreInModels(true);
            SpreadsheetModel model = spr.getModel();
            String type = getSimpleName(spr.getReturnRef());
            model.setType(type);
            model.setSteps(makeSingleStep(type, RESULT));
        }
    }

    private void removeContextFromParams(List<SpreadsheetModel> sprModelsWithRC) {
        for (SpreadsheetModel spreadsheetModel : sprModelsWithRC) {
            spreadsheetModel.getParameters().removeIf(parameter -> parameter.getType().equals(DEFAULT_RUNTIME_CONTEXT));
        }
    }

    private void fillCallsInSteps(final List<SpreadsheetModel> models, Set<String> datatypeRefs) {
        Set<String> dts = datatypeRefs.stream().map(OpenAPITypeUtils::getSimpleName).collect(Collectors.toSet());
        Set<String> sprNames = models.stream()
            .map(SpreadsheetModel::getName)
            .filter(x -> !dts.contains(x))
            .collect(Collectors.toSet());
        for (SpreadsheetModel model : models) {
            for (StepModel step : model.getSteps()) {
                String stepType = step.getType();
                boolean isArray = stepType.endsWith("[]");
                String type = ARRAY_MATCHER.matcher(stepType).replaceAll("");
                if (sprNames.contains(type)) {
                    Optional<SpreadsheetModel> foundSpr = models.stream()
                        .filter(x -> x.getName().equals(type))
                        .findAny();
                    if (foundSpr.isPresent()) {
                        SpreadsheetModel calledSpr = foundSpr.get();
                        String value = String.join(",", Collections.nCopies(calledSpr.getParameters().size(), "null"));
                        String call = makeCall(type, value);
                        if (isArray) {
                            step.setValue(makeArrayCall(stepType, call));
                        } else {
                            step.setValue("=" + call);
                        }
                    }
                }
            }
        }
    }

    private String makeArrayCall(String stepType, String call) {
        int dimension = calculateDimension(stepType);
        String openingBrackets = String.join("", Collections.nCopies(dimension, "{"));
        String closingBrackets = String.join("", Collections.nCopies(dimension, "}"));
        return "=new SpreadsheetResult" + stepType + openingBrackets + call + closingBrackets;
    }

    private int calculateDimension(String stepType) {
        int count = 0;
        boolean brackets = false;
        for (char c : stepType.toCharArray()) {
            if (c == '[') {
                if (!brackets) {
                    count++;
                }
                brackets = true;
            } else if (c == ']') {
                brackets = false;
            }
        }
        return count;
    }

    public boolean containsRuntimeContext(final Collection<InputParameter> inputParameters) {
        return CollectionUtils.isNotEmpty(inputParameters) && inputParameters.stream()
            .anyMatch(x -> x.getType().equals(DEFAULT_RUNTIME_CONTEXT));
    }

    public boolean containsOnlyRuntimeContext(final Collection<InputParameter> inputParameters) {
        return CollectionUtils.isNotEmpty(inputParameters) && inputParameters.size() == 1 && inputParameters.stream()
            .anyMatch(x -> x.getType().equals(DEFAULT_RUNTIME_CONTEXT));
    }

    private List<SpreadsheetParserModel> extractSprModels(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Set<String> pathWithPotentialSprResult,
            Set<String> pathsWithPrimitiveReturns,
            Set<String> refsToExpand,
            Set<String> pathsWithSpreadsheets,
            List<DatatypeModel> dts,
            Set<String> childSet) {
        List<SpreadsheetParserModel> spreadSheetModels = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            extractSpreadsheets(openAPI,
                jxPathContext,
                pathWithPotentialSprResult,
                refsToExpand,
                dts,
                spreadSheetModels,
                paths,
                PathType.SPREADSHEET_RESULT_PATH,
                childSet);
            extractSpreadsheets(openAPI,
                jxPathContext,
                pathsWithPrimitiveReturns,
                refsToExpand,
                dts,
                spreadSheetModels,
                paths,
                PathType.SIMPLE_RETURN_PATH,
                childSet);
            extractSpreadsheets(openAPI,
                jxPathContext,
                pathsWithSpreadsheets,
                refsToExpand,
                dts,
                spreadSheetModels,
                paths,
                PathType.SPREADSHEET_PATH,
                childSet);
        }
        return spreadSheetModels;
    }

    private void extractSpreadsheets(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Set<String> pathWithPotentialSprResult,
            Set<String> refsToExpand,
            List<DatatypeModel> dts,
            List<SpreadsheetParserModel> spreadSheetModels,
            Paths paths,
            PathType spreadsheetResultPath,
            Set<String> childSet) {
        for (String path : pathWithPotentialSprResult) {
            PathItem pathItem = paths.get(path);
            if (pathItem != null) {
                SpreadsheetParserModel spr = extractSpreadsheetModel(openAPI,
                    jxPathContext,
                    pathItem,
                    path,
                    refsToExpand,
                    spreadsheetResultPath,
                    dts,
                    childSet);
                spreadSheetModels.add(spr);
            }
        }
    }

    private SpreadsheetParserModel extractSpreadsheetModel(OpenAPI openAPI,
            JXPathContext jxPathContext,
            PathItem pathItem,
            String path,
            Set<String> refsToExpand,
            PathType pathType,
            List<DatatypeModel> dts,
            Set<String> childSet) {
        SpreadsheetParserModel spreadsheetParserModel = new SpreadsheetParserModel();
        SpreadsheetModel spr = new SpreadsheetModel();
        spreadsheetParserModel.setModel(spr);
        PathInfo pathInfo = generatePathInfo(path, pathItem);
        spr.setPathInfo(pathInfo);
        Schema<?> responseSchema = OpenLOpenAPIUtils.getUsedSchemaInResponse(jxPathContext, pathItem);
        String usedSchemaInResponse = OpenAPITypeUtils.extractType(responseSchema);
        pathInfo.setReturnType(extractReturnType(pathType, usedSchemaInResponse));
        boolean isChild = childSet.contains(usedSchemaInResponse);
        List<InputParameter> parameters = OpenLOpenAPIUtils
            .extractParameters(jxPathContext, openAPI, refsToExpand, pathItem, dts, path);
        String normalizedPath = replaceBrackets(path);
        String formattedName = normalizeName(normalizedPath);
        spr.setName(formattedName);
        spr.setParameters(parameters);
        pathInfo.setFormattedPath(formattedName);
        List<StepModel> stepModels = getStepModels(openAPI,
            pathType,
            spreadsheetParserModel,
            spr,
            usedSchemaInResponse,
            formattedName,
            isChild);
        spr.setSteps(stepModels);
        return spreadsheetParserModel;
    }

    private PathInfo generatePathInfo(String path, PathItem pathItem) {
        PathInfo pathInfo = new PathInfo();
        final OperationInfo operation = findOperation(pathItem);
        pathInfo.setOriginalPath(path);
        pathInfo.setOperation(operation.getMethod());
        pathInfo.setConsumes(operation.getConsumes());
        pathInfo.setProduces(operation.getProduces());
        return pathInfo;
    }

    private List<StepModel> getStepModels(OpenAPI openAPI,
            PathType pathType,
            SpreadsheetParserModel spreadsheetParserModel,
            SpreadsheetModel spr,
            String usedSchemaInResponse,
            String formattedName,
            boolean isChild) {
        List<StepModel> stepModels = new ArrayList<>();
        boolean isArray = usedSchemaInResponse.endsWith("[]");
        Schema<?> schema;
        if (PathType.SPREADSHEET_RESULT_PATH == pathType) {
            final String nameOfSchema = isArray ? ARRAY_MATCHER.matcher(usedSchemaInResponse).replaceAll("")
                                                : usedSchemaInResponse;
            schema = getSchemas(openAPI).get(nameOfSchema);
            boolean isArrayOrChild = isArray || isChild;
            spr.setType(isArrayOrChild ? usedSchemaInResponse : SPREADSHEET_RESULT);
            if (schema != null) {
                if (isArrayOrChild) {
                    stepModels = makeSingleStep(usedSchemaInResponse, RESULT);
                } else {
                    Map<String, Schema> properties = schema.getProperties();
                    if (CollectionUtils.isNotEmpty(properties)) {
                        stepModels = properties.entrySet()
                            .stream()
                            .filter(x -> !IGNORED_FIELDS.contains(x.getKey()))
                            .map(this::extractStep)
                            .collect(Collectors.toList());
                    }
                }
                boolean addToDataTypes = stepModels.stream()
                    .anyMatch(x -> ARRAY_MATCHER.matcher(x.getType()).replaceAll("").equals(nameOfSchema));
                spreadsheetParserModel.setStoreInModels(addToDataTypes || isArrayOrChild);
            }
            spreadsheetParserModel.setReturnRef(SCHEMAS_LINK + nameOfSchema);
        } else if (PathType.SPREADSHEET_PATH == pathType) {
            spr.setType(usedSchemaInResponse);
            stepModels = makeSingleStep(usedSchemaInResponse, RESULT);
        } else {
            spr.setType(usedSchemaInResponse);
            stepModels = makeSingleStep(usedSchemaInResponse, formattedName);
        }
        return stepModels;
    }

    private List<StepModel> makeSingleStep(String stepType, String stepName) {
        return Collections.singletonList(new StepModel(stepName, stepType, makeValue(stepType)));
    }

    private OperationInfo findOperation(PathItem pathItem) {
        String method = null;
        String consumes = null;
        String produces = null;
        Operation operation = null;
        if (pathItem != null) {
            Map<PathItem.HttpMethod, Operation> operationsMap = pathItem.readOperationsMap();
            if (CollectionUtils.isNotEmpty(operationsMap)) {
                if (operationsMap.get(PathItem.HttpMethod.GET) != null) {
                    method = PathItem.HttpMethod.GET.name();
                    operation = operationsMap.get(PathItem.HttpMethod.GET);
                } else if (operationsMap.get(PathItem.HttpMethod.POST) != null) {
                    method = PathItem.HttpMethod.POST.name();
                    operation = operationsMap.get(PathItem.HttpMethod.POST);
                } else {
                    Map.Entry<PathItem.HttpMethod, Operation> firstFoundOperation = operationsMap.entrySet()
                        .iterator()
                        .next();
                    method = firstFoundOperation.getKey().name();
                    operation = firstFoundOperation.getValue();
                }
            }
            if (operation != null) {
                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null) {
                    Content content = requestBody.getContent();
                    if (CollectionUtils.isNotEmpty(content)) {
                        if (content.containsKey(APPLICATION_JSON)) {
                            consumes = APPLICATION_JSON;
                        } else if (content.containsKey(TEXT_PLAIN)) {
                            consumes = TEXT_PLAIN;
                        } else {
                            consumes = content.keySet().iterator().next();
                        }
                    }
                }

                ApiResponses responses = operation.getResponses();
                ApiResponse successResponse = responses.get("200");
                ApiResponse defaultResponse = responses.getDefault();
                Content c = null;
                if (successResponse != null) {
                    c = successResponse.getContent();
                } else if (defaultResponse != null) {
                    c = defaultResponse.getContent();
                } else {
                    if (CollectionUtils.isNotEmpty(responses)) {
                        ApiResponse firstResponse = responses.values().iterator().next();
                        c = firstResponse.getContent();
                    }
                }
                if (CollectionUtils.isNotEmpty(c)) {
                    if (c.containsKey(APPLICATION_JSON)) {
                        produces = APPLICATION_JSON;
                    } else if (c.containsKey(TEXT_PLAIN)) {
                        produces = TEXT_PLAIN;
                    } else {
                        produces = c.keySet().iterator().next();
                    }
                }
            }

        }
        return new OperationInfo(method, produces, consumes);
    }

    private String replaceBrackets(String path) {
        return PARAMETERS_BRACKETS_MATCHER.matcher(path).replaceAll("");
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
            String parentName = OpenAPITypeUtils.getParentName((ComposedSchema) schema, openAPI);
            properties = OpenAPITypeUtils.getFieldsOfChild((ComposedSchema) schema);
            dm.setParent(parentName);
        } else {
            properties = schema.getProperties();
        }
        if (properties != null) {
            fields = properties.entrySet()
                .stream()
                .filter(property -> !IGNORED_FIELDS.contains(property.getKey()))
                .map(this::extractField)
                .collect(Collectors.toList());
        }
        dm.setFields(fields);
        return dm;
    }

    private FieldModel extractField(Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();

        String typeModel = OpenAPITypeUtils.extractType(valueSchema);
        Object defaultValue;
        if ((valueSchema instanceof IntegerSchema) && valueSchema.getFormat() == null) {
            if (valueSchema.getDefault() == null) {
                defaultValue = BigInteger.ZERO;
            } else {
                defaultValue = valueSchema.getDefault();
            }
        } else if (valueSchema instanceof NumberSchema && valueSchema.getFormat() == null && valueSchema
            .getDefault() != null) {
            defaultValue = valueSchema.getDefault().toString();
        } else {
            defaultValue = valueSchema.getDefault();
        }

        return new FieldModel(propertyName, typeModel, defaultValue);
    }

    private StepModel extractStep(Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();
        String typeModel = OpenAPITypeUtils.extractType(valueSchema);
        String value = makeValue(typeModel);
        return new StepModel(normalizeName(propertyName), typeModel, value);
    }

    private String makeValue(String type) {
        String result = "";
        if (StringUtils.isNotBlank(type)) {
            if (OpenAPITypeUtils.isSimpleType(type)) {
                result = OpenAPITypeUtils.getSimpleValue(type);
            } else {
                result = createNewInstance(type);
            }
        }
        return result;
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
        return type + "(" + value + ")";
    }

    private String formatTableName(final String name) {
        String value = name.replaceFirst("^get", "");
        return name.equals(value) ? value : StringUtils.capitalize(value);
    }

    private String extractReturnType(PathType type, String usedSchemaInResponse) {
        String result = "";
        switch (type) {
            case SPREADSHEET_RESULT_PATH:
            case SPREADSHEET_PATH:
                result = OBJECT;
                break;
            case SIMPLE_RETURN_PATH:
                result = usedSchemaInResponse;
                break;
        }
        return result;
    }
}
