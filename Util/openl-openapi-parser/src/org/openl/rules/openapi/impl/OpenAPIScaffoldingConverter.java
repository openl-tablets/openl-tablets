package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenAPITypeUtils.LINK_TO_DEFAULT_RUNTIME_CONTEXT;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.SCHEMAS_LINK;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.getSimpleName;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.APPLICATION_JSON;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.TEXT_PLAIN;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.normalizeName;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.resolve;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.lang3.tuple.Pair;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.MethodModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.OpenAPIRefResolver;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public class OpenAPIScaffoldingConverter implements OpenAPIModelConverter {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";
    public static final String SPR_RESULT_LINK = SCHEMAS_LINK + SPREADSHEET_RESULT;
    public static final String RESULT = "Result";
    public static final Pattern PARAMETERS_BRACKETS_MATCHER = Pattern.compile("\\{.*?}");
    private static final Set<String> IGNORED_FIELDS = Set.copyOf(List.of("@class"));
    public static final String SPREADSHEET_RESULT_CLASS_NAME = SpreadsheetResult.class.getName();
    public static final String GET_PREFIX = "get";

    @Override
    public ProjectModel extractProjectModel(String pathTo) {
        ParseOptions options = OpenLOpenAPIUtils.getParseOptions();
        var openAPI = new OpenAPIV3Parser().read(pathTo, null, options);
        if (openAPI == null) {
            throw new IllegalStateException("Error creating the project, uploaded file has invalid structure.");
        }
        var openAPIRefResolver = new OpenAPIRefResolver(openAPI);

        var projectName = openAPI.getInfo().getTitle();

        var paths = openAPI.getPaths();

        var allUsedSchemaRefs = OpenLOpenAPIUtils.getAllUsedSchemaRefs(paths, openAPIRefResolver);

        var pathsWithRequestsRefs = OpenLOpenAPIUtils.collectPathsWithParams(paths,
                openAPIRefResolver);

        var allUsedSchemaRefsInRequests = pathsWithRequestsRefs.values()
                .stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));

        var isRuntimeContextProvided = allUsedSchemaRefsInRequests.keySet()
                .stream()
                .anyMatch(LINK_TO_DEFAULT_RUNTIME_CONTEXT::equals);

        var allUnusedRefs = OpenLOpenAPIUtils.getUnusedSchemaRefs(openAPI, allUsedSchemaRefs.keySet());

        var childrenSchemas = OpenAPITypeUtils.getChildrenMap(openAPI);
        Set<String> parents = childrenSchemas.keySet();
        var childSet = childrenSchemas.values()
                .stream()
                .flatMap(Collection::stream)
                .map(OpenAPITypeUtils::getSimpleName)
                .collect(Collectors.toSet());

        var refsWithFields = OpenLOpenAPIUtils.getRefsInProperties(openAPI, openAPIRefResolver);
        var fieldsRefs = refsWithFields.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // all the requests which were used only once per project needed to be extracted
        // if it's extends from other model it will be an inline type
        var refsToExpand = allUsedSchemaRefsInRequests.entrySet().stream().filter(refWithCount -> {
            var ref = refWithCount.getKey();
            var numberOfRefUsage = refWithCount.getValue();
            var refIsParentOrField = !parents.contains(ref) && !fieldsRefs.contains(ref);
            var refIsNotChild = !childSet.contains(getSimpleName(ref));
            var refIsNotRuntimeContext = !ref.equals(LINK_TO_DEFAULT_RUNTIME_CONTEXT);
            return refIsNotRuntimeContext && numberOfRefUsage
                    .equals(1) && (!allUsedSchemaRefs.containsKey(ref) || allUsedSchemaRefs.get(ref)
                    .equals(1)) && refIsParentOrField && refIsNotChild;
        }).map(Map.Entry::getKey).collect(Collectors.toSet());

        var refsByPathAndOperation = OpenLOpenAPIUtils
                .getAllUsedRefResponses(paths, openAPIRefResolver);

        // all the path methods which have primitive responses are possible spreadsheets too
        var primitiveReturnPathOperations = refsByPathAndOperation.entrySet()
                .stream()
                .filter(pathWithOperation -> pathWithOperation.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // searching for path methods which response models are not included in ANY requestBody
        var operationsPathWithPotentialSpr = refsByPathAndOperation
                .entrySet()
                .stream()
                .filter(pathOperation -> !pathOperation.getValue().isEmpty() && pathOperation.getValue()
                        .stream()
                        .noneMatch(allUsedSchemaRefsInRequests::containsKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var spreadsheetOperations = refsByPathAndOperation.keySet()
                .stream()
                .filter(pathWithOperation -> !operationsPathWithPotentialSpr
                        .containsKey(pathWithOperation) && !primitiveReturnPathOperations.contains(pathWithOperation))
                .collect(Collectors.toSet());

        var spreadsheetResultRefs = operationsPathWithPotentialSpr.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        var spreadsheetParserModels = extractSprModels(paths,
                openAPIRefResolver,
                operationsPathWithPotentialSpr.keySet(),
                primitiveReturnPathOperations,
                spreadsheetOperations,
                refsToExpand,
                childSet);
        var dataModelRefs = new HashSet<String>();
        var dataModels = extractDataModels(spreadsheetParserModels,
                openAPIRefResolver,
                openAPI,
                spreadsheetResultRefs,
                dataModelRefs);
        // self-linked, array/child returned
        var linkedRefs = spreadsheetParserModels.stream()
                .filter(SpreadsheetParserModel::isRefIsDataType)
                .map(SpreadsheetParserModel::getReturnRef)
                .collect(Collectors.toList());
        var datatypeRefs = allUsedSchemaRefs.keySet().stream().filter(x -> {
            var notSpreadsheetAndExpanded = !(spreadsheetResultRefs.contains(x) || refsToExpand.contains(x));
            var isNotReserved = !x.equals(SPR_RESULT_LINK);
            var notDatatype = linkedRefs.contains(x);
            return isNotReserved && (notSpreadsheetAndExpanded || notDatatype);
        }).collect(Collectors.toSet());

        var refSpreadsheets = spreadsheetParserModels.stream()
                .filter(x -> !x.isRefIsDataType() && x.getReturnRef() != null)
                .map(SpreadsheetParserModel::getReturnRef)
                .collect(Collectors.toSet());

        var allFieldsRefs = retrieveAllFieldsRefs(datatypeRefs, refsWithFields);
        // case when any datatype has a link in a field to the spreadsheet
        var dtToAdd = allFieldsRefs.stream().filter(x -> {
            var isNotSpreadsheetResult = !SPR_RESULT_LINK.equals(x);
            var isNotPresentedInDataTypes = !datatypeRefs.contains(x);
            return isNotSpreadsheetResult && isNotPresentedInDataTypes;
        }).collect(Collectors.toSet());

        // If there is a datatype to add which was returned by any spreadsheet model, it will be transformed
        spreadsheetParserModels.stream().filter(x -> dtToAdd.contains(x.getReturnRef())).forEach(x -> {
            var model = x.getModel();
            String type = OpenAPITypeUtils.getSimpleName(x.getReturnRef());
            model.setType(type);
            model.getPathInfo().setReturnType(new TypeInfo(type, type, TypeInfo.Type.DATATYPE));
            model.setSteps(makeSingleStep(type));
        });

        fillCallsInSteps(spreadsheetParserModels, datatypeRefs, dataModelRefs, dtToAdd);

        datatypeRefs.addAll(dtToAdd);
        refSpreadsheets.removeAll(dtToAdd);

        var dts = new LinkedHashSet<DatatypeModel>(extractDataTypeModels(openAPIRefResolver, openAPI, datatypeRefs));
        dts.addAll(extractDataTypeModels(openAPIRefResolver, openAPI, allUnusedRefs));

        var usedInDataTypes = new HashSet<String>();
        // searching for links in data types
        dts.forEach(dt -> {
            var set = dt.getFields().stream().map(FieldModel::getType).collect(Collectors.toSet());
            if (!set.contains(dt.getName())) {
                dt.getFields()
                        .stream()
                        .filter(fieldModel -> !OpenAPITypeUtils.isSimpleType(fieldModel.getType()))
                        .map(fieldModel -> OpenAPITypeUtils.removeArrayBrackets(fieldModel.getType()))
                        .forEach(usedInDataTypes::add);
            }
        });
        // if no links from data types, but model has links to the spreadsheets -> it will be a spreadsheet
        // any spreadsheet result filtering there to avoid the broken project
        var notUsedDataTypeWithRefToSpreadsheet = dts.stream()
                .filter(x -> !usedInDataTypes.contains(x.getName()))
                .map(x -> Pair.of(x.getName(), x.getFields()))
                .filter(y -> y.getRight()
                        .stream()
                        .anyMatch(field -> refSpreadsheets
                                .contains(SCHEMAS_LINK + OpenAPITypeUtils.removeArrayBrackets(field.getType()))))
                .map(Pair::getLeft)
                .collect(Collectors.toList());

        dts.removeIf(
                x -> notUsedDataTypeWithRefToSpreadsheet.contains(x.getName()) || SPREADSHEET_RESULT.equals(x.getName()));
        // create spreadsheet from potential models
        createLostSpreadsheets(openAPIRefResolver,
                openAPI,
                spreadsheetParserModels,
                refSpreadsheets,
                notUsedDataTypeWithRefToSpreadsheet,
                pathsWithRequestsRefs,
                isRuntimeContextProvided);
        // change steps with in the spreadsheets to these potential models
        setCallsAndReturnTypeToLostSpreadsheet(spreadsheetParserModels, notUsedDataTypeWithRefToSpreadsheet);

        var dtNames = dts.stream().map(DatatypeModel::getName).collect(Collectors.toSet());
        checkTypes(spreadsheetParserModels, dtNames);

        final Consumer<MethodModel> applyInclude = method -> method
                .setInclude(paths.get(method.getPathInfo().getOriginalPath()) != null);
        var spreadsheetModels = spreadsheetParserModels.stream()
                .map(SpreadsheetParserModel::getModel)
                .collect(Collectors.toList());
        spreadsheetModels.forEach(applyInclude);
        dataModels.forEach(applyInclude);

        var sprModelsDivided = spreadsheetModels.stream()
                .collect(Collectors.partitioningBy(spreadsheetModel -> containsRuntimeContext(
                        pathsWithRequestsRefs.get(spreadsheetModel.getPathInfo().getOriginalPath()))));
        List<SpreadsheetModel> sprModelsWithRC = sprModelsDivided.get(true);

        // remove defaultRuntimeContext from dts - it will be generated automatically in the interface
        dts.removeIf(dt -> dt.getName().equals(OpenAPITypeUtils.DEFAULT_RUNTIME_CONTEXT));

        removeContextFromParams(sprModelsWithRC);
        return new ProjectModel(projectName,
                isRuntimeContextProvided,
                dts,
                dataModels,
                isRuntimeContextProvided ? sprModelsWithRC : spreadsheetModels,
                isRuntimeContextProvided ? sprModelsDivided.get(false) : List.of());
    }

    private Set<String> retrieveAllFieldsRefs(Set<String> datatypeRefs, Map<String, Set<String>> refsWithFields) {
        var allFieldsRefs = new HashSet<String>();
        var queue = new ArrayDeque<String>(datatypeRefs);
        while (!queue.isEmpty()) {
            final var dtRef = queue.poll();
            refsWithFields.getOrDefault(dtRef, Set.of())
                    .stream()
                    .filter(x -> !datatypeRefs.contains(x) && !allFieldsRefs.contains(x))
                    .filter(allFieldsRefs::add)
                    .forEach(queue::add);
        }
        return allFieldsRefs;
    }

    private void checkTypes(List<SpreadsheetParserModel> parserModels, Set<String> dataTypeNames) {
        for (SpreadsheetParserModel parserModel : parserModels) {
            var model = parserModel.getModel();
            var pathInfo = model.getPathInfo();
            if (pathInfo != null) {
                var returnType = pathInfo.getReturnType();
                if (dataTypeNames.contains(OpenAPITypeUtils.removeArrayBrackets(returnType.getSimpleName()))) {
                    returnType.setType(TypeInfo.Type.DATATYPE);
                }
            }

            List<InputParameter> parameters = model.getParameters();
            for (InputParameter parameter : parameters) {
                var type = parameter.getType();
                if (dataTypeNames.contains(OpenAPITypeUtils.removeArrayBrackets(type.getSimpleName()))) {
                    type.setType(TypeInfo.Type.DATATYPE);
                }
            }
        }
    }

    private void setCallsAndReturnTypeToLostSpreadsheet(List<SpreadsheetParserModel> spreadsheetParserModels,
                                                        List<String> notUsedDataTypeWithRefToSpreadsheet) {
        if (!notUsedDataTypeWithRefToSpreadsheet.isEmpty()) {
            for (SpreadsheetParserModel spreadsheetParserModel : spreadsheetParserModels) {
                var sprModel = spreadsheetParserModel.getModel();
                String returnType = OpenAPITypeUtils.removeArrayBrackets(sprModel.getType());
                if (notUsedDataTypeWithRefToSpreadsheet.contains(returnType)) {
                    var pathInfo = sprModel.getPathInfo();
                    var pathReturnType = pathInfo.getReturnType();
                    var dimension = pathReturnType.getDimension();
                    if (dimension == 0) {
                        sprModel.setType(SPREADSHEET_RESULT);
                        pathInfo.setReturnType(
                                new TypeInfo(SPREADSHEET_RESULT_CLASS_NAME, SPREADSHEET_RESULT, TypeInfo.Type.SPREADSHEET));
                    } else {
                        sprModel.setType(
                                SPREADSHEET_RESULT + returnType + String.join("", Collections.nCopies(dimension, "[]")));
                        pathReturnType.setJavaName(OpenAPITypeUtils.getSpreadsheetArrayClassName(dimension));
                        pathReturnType.setType(TypeInfo.Type.SPREADSHEET);
                    }
                }
                for (StepModel model : sprModel.getSteps()) {
                    var type = model.getType();
                    String simpleType = OpenAPITypeUtils.removeArrayBrackets(type);
                    if (notUsedDataTypeWithRefToSpreadsheet.contains(simpleType)) {
                        var call = makeCall(type, "");
                        model.setValue(type.endsWith("[]") ? makeArrayCall(type, simpleType, "") : "= " + call);
                    }
                }
            }
        }
    }

    private void createLostSpreadsheets(OpenAPIRefResolver openAPIRefResolver,
                                        OpenAPI openAPI,
                                        List<SpreadsheetParserModel> spreadsheetParserModels,
                                        Set<String> refSpreadsheets,
                                        List<String> notUsedDataTypeWithRefToSpreadsheet,
                                        Map<String, Map<String, Integer>> pathsWithRequestsRefs,
                                        boolean isRuntimeContextProvided) {
        for (String modelName : notUsedDataTypeWithRefToSpreadsheet) {
            var lostModel = new SpreadsheetParserModel();
            var model = new SpreadsheetModel();
            model.setName(modelName);
            model.setType(SPREADSHEET_RESULT);
            model.setParameters(List.of());
            Schema<?> schema = getSchemas(openAPI).get(modelName);
            List<StepModel> steps = new ArrayList<>();
            if (schema != null) {
                Map<String, Schema> properties = schema.getProperties();
                if (CollectionUtils.isNotEmpty(properties)) {
                    steps = properties.entrySet()
                            .stream()
                            .filter(propertyEntry -> !IGNORED_FIELDS.contains(propertyEntry.getKey()))
                            .map(propertyEntry -> createStep(openAPIRefResolver,
                                    spreadsheetParserModels,
                                    refSpreadsheets,
                                    modelName,
                                    propertyEntry))
                            .collect(Collectors.toList());
                }
            }
            model.setSteps(steps);
            var originalPath = "/" + modelName;
            model.setPathInfo(new PathInfo(originalPath,
                    modelName,
                    PathInfo.Operation.POST,
                    new TypeInfo(SPREADSHEET_RESULT_CLASS_NAME, SPREADSHEET_RESULT, TypeInfo.Type.SPREADSHEET)));
            lostModel.setModel(model);
            spreadsheetParserModels.add(lostModel);
            if (isRuntimeContextProvided && !pathsWithRequestsRefs.containsKey(originalPath)) {
                var mapWithRC = new HashMap<String, Integer>();
                mapWithRC.put(LINK_TO_DEFAULT_RUNTIME_CONTEXT, 1);
                pathsWithRequestsRefs.put(originalPath, mapWithRC);
            }
        }
    }

    private StepModel createStep(OpenAPIRefResolver openAPIRefResolver,
                                 List<SpreadsheetParserModel> spreadsheetParserModels,
                                 Set<String> refSpreadsheets,
                                 String modelName,
                                 Map.Entry<String, Schema> propertyEntry) {
        var step = extractStep(openAPIRefResolver, propertyEntry);
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(openAPIRefResolver, propertyEntry.getValue(), false);
        var stepType = typeInfo.getSimpleName();
        String type = OpenAPITypeUtils.removeArrayBrackets(stepType);
        var modelToCall = "";
        var value = "";
        if (!type.equals(modelName) && !refSpreadsheets.contains(SCHEMAS_LINK + type)) {
            return step;
        }
        if (type.equals(modelName)) {
            modelToCall = modelName;
        } else {
            Optional<SpreadsheetParserModel> optionalModel = Optional.empty();
            for (SpreadsheetParserModel parserModel : spreadsheetParserModels) {
                var dimension = parserModel.getModel().getPathInfo().getReturnType().getDimension();
                var returnRef = parserModel.getReturnRef();
                if (returnRef != null && returnRef.equals(SCHEMAS_LINK + type) && dimension == 0) {
                    optionalModel = Optional.of(parserModel);
                    break;
                }
            }
            if (optionalModel.isPresent()) {
                var spreadsheetParserModel = optionalModel.get();
                modelToCall = spreadsheetParserModel.getModel().getName();
                value = spreadsheetParserModel.getModel()
                        .getParameters()
                        .stream()
                        .map(InputParameter::getType)
                        .filter(t -> t.getType() != TypeInfo.Type.RUNTIMECONTEXT)
                        .map(OpenAPITypeUtils::getJavaDefaultValue)
                        .collect(Collectors.joining(", "));
            }
        }
        var call = makeCall(modelToCall, value);
        if (stepType.endsWith("[]")) {
            step.setValue(makeArrayCall(stepType, modelToCall, call));
        } else {
            step.setValue("= " + call);
        }
        return step;
    }

    private List<DataModel> extractDataModels(List<SpreadsheetParserModel> spreadsheetModels,
                                              OpenAPIRefResolver openAPIRefResolver,
                                              OpenAPI openAPI,
                                              Set<String> sprResultRefs,
                                              Set<String> dataModelsRefs) {
        var potentialDataModels = spreadsheetModels.stream()
                .filter(x -> x.getModel()
                        .getPathInfo()
                        .getFormattedPath()
                        .startsWith(GET_PREFIX) && (CollectionUtils
                        .isEmpty(x.getModel().getParameters()) || containsOnlyRuntimeContext(x.getModel().getParameters())))
                .collect(Collectors.toList());
        var dataModels = new ArrayList<DataModel>();
        for (SpreadsheetParserModel potentialDataModel : potentialDataModels) {
            final var returnType = potentialDataModel.getModel().getPathInfo().getReturnType();
            String type = OpenAPITypeUtils.removeArrayBrackets(returnType.getSimpleName());
            if (returnType.getDimension() == 0 || type.equals(SPREADSHEET_RESULT)) {
                continue;
            }
            var potentialDataTablePathInfo = potentialDataModel.getModel().getPathInfo();
            var operationMethod = potentialDataTablePathInfo.getOperation().name();
            // if get operation without parameters or post with only runtime context
            List<InputParameter> parameters = potentialDataModel.getModel().getParameters();
            var parametersNotEmpty = CollectionUtils.isNotEmpty(parameters);
            var getAndNoParams = parameters.isEmpty() && operationMethod.equals(PathItem.HttpMethod.GET.name());
            var postAndRuntimeContext = parametersNotEmpty && operationMethod
                    .equals(PathItem.HttpMethod.POST.name());
            if (getAndNoParams || postAndRuntimeContext) {
                var returnRef = potentialDataModel.getReturnRef();
                if (returnRef != null) {
                    sprResultRefs.remove(returnRef);
                    dataModelsRefs.add(returnRef);
                }
                spreadsheetModels.remove(potentialDataModel);
                var dataTableName = formatTableName(potentialDataModel.getModel().getName());
                potentialDataTablePathInfo.setFormattedPath(GET_PREFIX + dataTableName);

                var isSimpleType = OpenAPITypeUtils.isSimpleType(type);
                var dataModel = new DataModel(dataTableName,
                        type,
                        potentialDataTablePathInfo,
                        isSimpleType ? createSimpleModel(type)
                                : createModelForDataTable(openAPIRefResolver,
                                openAPI,
                                type,
                                getSchemas(openAPI).get(type)));

                TypeInfo.Type resultType = isSimpleType ? TypeInfo.Type.OBJECT : TypeInfo.Type.DATATYPE;
                dataModel.getPathInfo().getReturnType().setType(resultType);
                if (parametersNotEmpty) {
                    dataModel.getPathInfo().setRuntimeContextParameter(parameters.getFirst());
                }
                dataModels.add(dataModel);
            }
        }
        return dataModels;
    }

    private void removeContextFromParams(List<SpreadsheetModel> sprModelsWithRC) {
        for (SpreadsheetModel spreadsheetModel : sprModelsWithRC) {
            spreadsheetModel.getParameters()
                    .stream()
                    .filter(p -> p.getType().getType() == TypeInfo.Type.RUNTIMECONTEXT)
                    .findFirst()
                    .ifPresent(context -> {
                        spreadsheetModel.getParameters().remove(context);
                        spreadsheetModel.getPathInfo().setRuntimeContextParameter(context);
                    });
        }
    }

    private Set<String> fillCallsInSteps(final List<SpreadsheetParserModel> models,
                                         Set<String> datatypeRefs,
                                         Set<String> dataModelRefs,
                                         Set<String> lostDt) {
        var calledRefs = new HashSet<String>();
        final var fixedDataTypes = Stream.concat(dataModelRefs.stream(), lostDt.stream())
                .collect(Collectors.toSet());
        // return type + spreadsheet name
        var sprResultNames = new HashSet<Pair<String, String>>();
        for (SpreadsheetParserModel model : models) {
            var returnRef = model.getReturnRef();
            if (returnRef != null && model.isRefIsDataType() && models.stream()
                    .anyMatch(x -> returnRef.equals(x.getReturnRef()) && !x.isRefIsDataType()) && !fixedDataTypes
                    .contains(returnRef)) {
                datatypeRefs.remove(returnRef);
            }
        }
        final var datatypeNames = Stream.concat(datatypeRefs.stream(), fixedDataTypes.stream())
                .collect(Collectors.toSet())
                .stream()
                .map(ref -> OpenAPITypeUtils.getSimpleName(ref).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        var reservedWords = new HashSet<String>(datatypeNames);
        var spreadsheetWithParameterNames = new HashMap<String, Set<String>>();

        for (SpreadsheetParserModel model : models) {
            var spreadsheetModel = model.getModel();
            var parameterNames = spreadsheetModel.getParameters()
                    .stream()
                    .map(InputParameter::getFormattedName)
                    .collect(Collectors.toSet());
            var spreadsheetType = spreadsheetModel.getType();
            var returnRef = model.getReturnRef();
            final var spreadsheetName = spreadsheetModel.getName();
            var pathInfo = spreadsheetModel.getPathInfo();
            final var lowerCasedSpreadsheetName = spreadsheetName.toLowerCase(Locale.ROOT);
            var spreadsheetWithSameNameAndParametersExists = spreadsheetWithParameterNames
                    .containsKey(lowerCasedSpreadsheetName) && spreadsheetWithParameterNames.get(lowerCasedSpreadsheetName)
                    .equals(parameterNames);
            if (spreadsheetWithSameNameAndParametersExists && returnRef == null) {
                var name = makeName(spreadsheetModel.getName(), reservedWords);
                spreadsheetModel.setName(name);
                pathInfo.setFormattedPath(name);
            } else if (returnRef != null && (SPREADSHEET_RESULT.equals(spreadsheetType) || !datatypeRefs
                    .contains(SCHEMAS_LINK + OpenAPITypeUtils.removeArrayBrackets(spreadsheetType)))) {
                var returnType = pathInfo.getReturnType();
                if (returnType.getDimension() == 0 && (datatypeNames
                        .contains(lowerCasedSpreadsheetName) || spreadsheetWithSameNameAndParametersExists)) {
                    var modifiedName = findSpreadsheetName(returnRef, reservedWords);
                    spreadsheetModel.setName(modifiedName);
                    returnType.setJavaName(OpenAPITypeUtils.getSpreadsheetArrayClassName(returnType.getDimension()));
                    pathInfo.setFormattedPath(modifiedName);
                }
                sprResultNames.add(Pair.of(returnType.getSimpleName(), spreadsheetModel.getName()));
            }
            spreadsheetWithParameterNames.put(spreadsheetModel.getName().toLowerCase(Locale.ROOT), parameterNames);
            reservedWords.add(spreadsheetModel.getName().toLowerCase(Locale.ROOT));
        }
        for (SpreadsheetParserModel parserModel : models) {
            var spreadsheetModel = parserModel.getModel();
            String refType = parserModel.getReturnRef() != null
                    ? OpenAPITypeUtils
                    .getSimpleName(parserModel.getReturnRef())
                    : "";
            Optional<Pair<String, String>> willBeCalled = sprResultNames.stream()
                    .filter(p -> p.getKey().equals(refType) && !p.getValue().equals(spreadsheetModel.getName()))
                    .findAny();
            var existingPathInfo = spreadsheetModel.getPathInfo();
            if (willBeCalled.isPresent()) {
                // change return type if the array of spreadsheets will be returned
                var dimension = existingPathInfo.getReturnType().getDimension();
                if (dimension > 0) {
                    spreadsheetModel.setType(SPREADSHEET_RESULT + willBeCalled.get().getValue() + String.join("",
                            Collections.nCopies(dimension, "[]")));
                    existingPathInfo.getReturnType()
                            .setJavaName(OpenAPITypeUtils.getSpreadsheetArrayClassName(dimension));
                }
            }
            for (StepModel step : spreadsheetModel.getSteps()) {
                var stepType = step.getType();
                var isArray = stepType.endsWith("[]");
                String type = OpenAPITypeUtils.removeArrayBrackets(step.getType());
                if (sprResultNames.stream().anyMatch(x -> x.getKey().equals(type))) {
                    Optional<SpreadsheetParserModel> foundSpr = Optional.empty();
                    if (willBeCalled.isPresent()) {
                        Pair<String, String> called = willBeCalled.get();
                        var calledType = called.getKey();
                        // if step type equals to the returned type of spreadsheet
                        if (type.equals(calledType)) {
                            foundSpr = models.stream()
                                    .filter(x -> x.getModel().getName().equals(called.getRight()))
                                    .findFirst();
                        }
                    }
                    // the called spreadsheet is not returned by the model
                    if (Objects.equals(foundSpr, Optional.empty())) {
                        foundSpr = models.stream().filter(sprModel -> {
                            var typesAreTheSame = sprModel.getReturnRef() != null && type
                                    .equals(OpenAPITypeUtils.getSimpleName(sprModel.getReturnRef()));
                            var notItSelf = !sprModel.getModel().getName().equals(spreadsheetModel.getName());
                            var isSpreadsheetResult = sprModel.getModel().getType().equals(SPREADSHEET_RESULT);
                            return typesAreTheSame && notItSelf && isSpreadsheetResult;
                        }).findAny();
                    }
                    // the called spreadsheet was found
                    if (foundSpr.isPresent()) {
                        var calledSpr = foundSpr.get();
                        var calledRef = calledSpr.getReturnRef();
                        calledRefs.add(calledRef);

                        var calledModel = calledSpr.getModel();
                        List<InputParameter> parameters = calledModel.getParameters();
                        var value = parameters.stream()
                                .map(InputParameter::getType)
                                .filter(t -> t.getType() != TypeInfo.Type.RUNTIMECONTEXT)
                                .map(OpenAPITypeUtils::getJavaDefaultValue)
                                .collect(Collectors.joining(", "));
                        var calledName = calledModel.getName();
                        var call = makeCall(calledName, value);
                        step.setValue(isArray ? makeArrayCall(stepType, calledName, call) : "= " + call);
                    }
                }
            }
        }
        return calledRefs;
    }

    private String findSpreadsheetName(final String returnRef, final Set<String> reservedNames) {
        String nameCandidate = OpenAPITypeUtils.getSimpleName(returnRef);
        return makeName(nameCandidate, reservedNames);
    }

    private String makeName(String candidate, final Set<String> reservedWords) {
        if (CollectionUtils.isNotEmpty(reservedWords) && reservedWords.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = candidate + "1";
            return makeName(candidate, reservedWords);
        }
        return candidate;
    }

    private String makeArrayCall(String stepType, String name, String call) {
        var dimension = calculateDimension(stepType);
        String openingBrackets = String.join("", Collections.nCopies(dimension, "{"));
        String closingBrackets = String.join("", Collections.nCopies(dimension, "}"));
        String arrayBrackets = String.join("", Collections.nCopies(dimension, "[]"));
        return "= new SpreadsheetResult" + name + arrayBrackets + openingBrackets + call + closingBrackets;
    }

    private int calculateDimension(String stepType) {
        var count = 0;
        var brackets = false;
        for (var i = 0; i < stepType.length(); i++) {
            var c = stepType.charAt(i);
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

    public boolean containsRuntimeContext(final Map<String, Integer> inputParametersEntry) {
        return inputParametersEntry != null && inputParametersEntry.containsKey(LINK_TO_DEFAULT_RUNTIME_CONTEXT);
    }

    public boolean containsOnlyRuntimeContext(final Collection<InputParameter> inputParameters) {
        return CollectionUtils.isNotEmpty(inputParameters) && inputParameters.size() == 1 && inputParameters.stream()
                .anyMatch(x -> x.getType().getType() == TypeInfo.Type.RUNTIMECONTEXT);
    }

    private List<SpreadsheetParserModel> extractSprModels(Paths paths,
                                                          OpenAPIRefResolver openAPIRefResolver,
                                                          Set<Pair<String, PathItem.HttpMethod>> pathWithPotentialSprResult,
                                                          Set<Pair<String, PathItem.HttpMethod>> pathsWithPrimitiveReturns,
                                                          Set<Pair<String, PathItem.HttpMethod>> pathsWithSpreadsheets,
                                                          Set<String> refsToExpand,
                                                          Set<String> childSet) {
        var spreadSheetModels = new ArrayList<SpreadsheetParserModel>();
        if (paths != null) {
            extractSpreadsheets(openAPIRefResolver,
                    pathWithPotentialSprResult,
                    refsToExpand,
                    spreadSheetModels,
                    paths,
                    PathType.SPREADSHEET_RESULT_PATH,
                    childSet);
            extractSpreadsheets(openAPIRefResolver,
                    pathsWithPrimitiveReturns,
                    refsToExpand,
                    spreadSheetModels,
                    paths,
                    PathType.SIMPLE_RETURN_PATH,
                    childSet);
            extractSpreadsheets(openAPIRefResolver,
                    pathsWithSpreadsheets,
                    refsToExpand,
                    spreadSheetModels,
                    paths,
                    PathType.SPREADSHEET_PATH,
                    childSet);
        }
        return spreadSheetModels;
    }

    private void extractSpreadsheets(OpenAPIRefResolver openAPIRefResolver,
                                     Set<Pair<String, PathItem.HttpMethod>> pathWithMethod,
                                     Set<String> refsToExpand,
                                     List<SpreadsheetParserModel> spreadSheetModels,
                                     Paths paths,
                                     PathType spreadsheetResultPath,
                                     Set<String> childSet) {
        final var pathWithOperationsMap = pathWithMethod.stream()
                .collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toSet())));

        for (Map.Entry<String, Set<PathItem.HttpMethod>> pathWithOperations : pathWithOperationsMap.entrySet()) {
            final var pathUrl = pathWithOperations.getKey();
            var pathItem = paths.get(pathUrl);
            if (pathItem != null) {
                var spr = extractSpreadsheetModel(openAPIRefResolver,
                        pathWithOperations.getValue(),
                        pathItem,
                        pathUrl,
                        refsToExpand,
                        spreadsheetResultPath,
                        childSet);
                spreadSheetModels.addAll(spr);
            }
        }
    }

    private List<SpreadsheetParserModel> extractSpreadsheetModel(OpenAPIRefResolver openAPIRefResolver,
                                                                 Set<PathItem.HttpMethod> methods,
                                                                 PathItem pathItem,
                                                                 String path,
                                                                 Set<String> refsToExpand,
                                                                 PathType pathType,
                                                                 Set<String> childSet) {
        var spreadsheetParserModels = new ArrayList<SpreadsheetParserModel>();
        var multipleOperations = methods.size() > 1;

        final var filteredMap = pathItem.readOperationsMap()
                .entrySet()
                .stream()
                .filter(m -> methods.contains(m.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : filteredMap.entrySet()) {
            var spreadsheetParserModel = new SpreadsheetParserModel();
            var spr = new SpreadsheetModel();
            spreadsheetParserModel.setModel(spr);
            var pathInfo = generatePathInfo(path, operationEntry);
            spr.setPathInfo(pathInfo);
            var responseSchema = OpenLOpenAPIUtils.getUsedSchemaInResponse(openAPIRefResolver,
                    operationEntry.getValue());
            if (responseSchema == null) {
                continue;
            }
            TypeInfo typeInfo = OpenAPITypeUtils.extractType(openAPIRefResolver, responseSchema, false);
            if (PathType.SPREADSHEET_RESULT_PATH.equals(pathType)) {
                typeInfo = new TypeInfo(SPREADSHEET_RESULT_CLASS_NAME,
                        typeInfo.getSimpleName(),
                        TypeInfo.Type.SPREADSHEET,
                        typeInfo.getDimension(),
                        typeInfo.isReference());
            }
            var usedSchemaInResponse = typeInfo.getSimpleName();
            pathInfo.setReturnType(typeInfo);
            var isChild = childSet.contains(usedSchemaInResponse);
            var parameters = OpenLOpenAPIUtils
                    .extractParameters(openAPIRefResolver, refsToExpand, pathItem, operationEntry);
            var normalizedPath = replaceBrackets(path);
            var formattedName = generateSpreadsheetName(normalizedPath,
                    multipleOperations,
                    operationEntry.getKey().name());
            spr.setName(formattedName);
            spr.setParameters(parameters);
            pathInfo.setFormattedPath(formattedName);
            var stepModels = getStepModels(typeInfo,
                    openAPIRefResolver,
                    pathType,
                    spreadsheetParserModel,
                    spr,
                    responseSchema,
                    isChild);
            spr.setSteps(stepModels);
            spreadsheetParserModels.add(spreadsheetParserModel);
        }
        return spreadsheetParserModels;
    }

    private String generateSpreadsheetName(String normalizedPath, boolean multipleOperations, String operationName) {
        String potentialName = normalizeName(normalizedPath);
        if (multipleOperations) {
            potentialName += operationName;
        }
        return potentialName;
    }

    private PathInfo generatePathInfo(String path, Map.Entry<PathItem.HttpMethod, Operation> operationEntry) {
        var pathInfo = new PathInfo();
        final var operationInfo = getOperationInfo(operationEntry.getValue(), operationEntry.getKey());
        pathInfo.setOriginalPath(path);
        pathInfo.setOperation(Optional.ofNullable(operationInfo.getMethod())
                .map(String::toUpperCase)
                .map(PathInfo.Operation::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("Invalid method operation")));
        pathInfo.setConsumes(operationInfo.getConsumes());
        pathInfo.setProduces(operationInfo.getProduces());
        return pathInfo;
    }

    private List<StepModel> getStepModels(TypeInfo typeInfo,
                                          OpenAPIRefResolver openAPIRefResolver,
                                          PathType pathType,
                                          SpreadsheetParserModel spreadsheetParserModel,
                                          SpreadsheetModel spr,
                                          Schema<?> usedSchemaInResponse,
                                          boolean isChild) {
        List<StepModel> stepModels = new ArrayList<>();
        var isArray = typeInfo.getDimension() > 0;
        var simpleName = typeInfo.getSimpleName();
        final String nameOfSchema = isArray ? OpenAPITypeUtils.removeArrayBrackets(simpleName) : simpleName;
        if (PathType.SPREADSHEET_RESULT_PATH == pathType) {
            var schema = resolve(openAPIRefResolver, usedSchemaInResponse, Schema::get$ref);
            var isArrayOrChild = isArray || isChild;
            spr.setType(isArrayOrChild ? simpleName : SPREADSHEET_RESULT);
            if (schema != null) {
                if (isArrayOrChild) {
                    stepModels = makeSingleStep(simpleName);
                } else {
                    Map<String, Schema> properties = schema.getProperties();
                    if (CollectionUtils.isNotEmpty(properties)) {
                        stepModels = properties.entrySet()
                                .stream()
                                .filter(x -> !IGNORED_FIELDS.contains(x.getKey()))
                                .map(p -> extractStep(openAPIRefResolver, p))
                                .collect(Collectors.toList());
                    }
                }
                var addToDataTypes = stepModels.stream()
                        .anyMatch(x -> OpenAPITypeUtils.removeArrayBrackets(x.getType()).equals(nameOfSchema));
                spreadsheetParserModel.setStoreInModels(addToDataTypes || isArrayOrChild);
            }
            spreadsheetParserModel.setReturnRef(SCHEMAS_LINK + nameOfSchema);
        } else {
            spr.setType(simpleName);
            stepModels = makeSingleStep(simpleName);
        }
        return stepModels;
    }

    private List<StepModel> makeSingleStep(String stepType) {
        return List
                .of(new StepModel(OpenAPIScaffoldingConverter.RESULT, stepType, makeValue(stepType)));
    }

    private OperationInfo getOperationInfo(Operation operation, PathItem.HttpMethod method) {
        String consumes = null;
        String produces = null;
        if (operation != null) {
            var requestBody = operation.getRequestBody();
            if (requestBody != null) {
                var content = requestBody.getContent();
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

            var responses = operation.getResponses();
            var successResponse = responses.get("200");
            var defaultResponse = responses.getDefault();
            Content c = null;
            if (successResponse != null) {
                c = successResponse.getContent();
            } else if (defaultResponse != null) {
                c = defaultResponse.getContent();
            } else {
                if (CollectionUtils.isNotEmpty(responses)) {
                    var firstResponse = responses.values().iterator().next();
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
        return new OperationInfo(method.name(), produces, consumes);
    }

    private String replaceBrackets(String path) {
        return PARAMETERS_BRACKETS_MATCHER.matcher(path).replaceAll("");
    }

    private List<DatatypeModel> extractDataTypeModels(OpenAPIRefResolver openAPIRefResolver,
                                                      OpenAPI openAPI,
                                                      Set<String> allTheRefsWhichAreDataTypes) {
        var result = new ArrayList<DatatypeModel>();
        for (String datatypeRef : allTheRefsWhichAreDataTypes) {
            var schema = (Schema<?>) OpenLOpenAPIUtils.resolveByRef(openAPIRefResolver, datatypeRef);
            if (schema != null && OpenAPITypeUtils.isComplexSchema(openAPIRefResolver, schema)) {
                var dm = createModel(openAPIRefResolver,
                        openAPI,
                        OpenAPITypeUtils.getSimpleName(datatypeRef),
                        schema);
                result.add(dm);
            }
        }
        return result;
    }

    private DatatypeModel createSimpleModel(String type) {
        var dm = new DatatypeModel("");
        dm.setFields(List.of(new FieldModel("this", type)));
        return dm;
    }

    private DatatypeModel createModel(OpenAPIRefResolver openAPIRefResolver,
                                      OpenAPI openAPI,
                                      String schemaName,
                                      Schema<?> schema) {
        var dm = new DatatypeModel(normalizeName(schemaName));
        Map<String, Schema> properties;
        List<FieldModel> fields = new ArrayList<>();
        if (schema instanceof ComposedSchema composedSchema) {
            String parentName = OpenAPITypeUtils.getParentName(composedSchema, openAPI);
            properties = OpenAPITypeUtils.getFieldsOfChild(composedSchema);
            if (composedSchema.getProperties() != null) {
                composedSchema.getProperties().forEach(properties::putIfAbsent);
            }
            dm.setParent(parentName);
        } else {
            properties = schema.getProperties();
        }
        if (properties != null) {
            fields = properties.entrySet().stream().filter(property -> {
                var isIgnoredField = IGNORED_FIELDS.contains(property.getKey());
                var ref = property.getValue().get$ref();
                var isRuntimeContext = ref != null && ref.equals(LINK_TO_DEFAULT_RUNTIME_CONTEXT);
                return !(isIgnoredField || isRuntimeContext);
            }).map(p -> extractField(openAPIRefResolver, p)).collect(Collectors.toList());
        }
        dm.setFields(fields);
        return dm;
    }

    private DatatypeModel createModelForDataTable(OpenAPIRefResolver openAPIRefResolver,
                                                  OpenAPI openAPI,
                                                  String schemaName,
                                                  Schema<?> schema) {
        var dm = new DatatypeModel(normalizeName(schemaName));
        Map<String, Schema> properties;
        List<FieldModel> fields = new ArrayList<>();
        if (schema instanceof ComposedSchema composedSchema) {
            properties = OpenAPITypeUtils.getAllProperties(composedSchema, openAPI);
        } else {
            properties = schema.getProperties();
        }
        if (properties != null) {
            fields = properties.entrySet()
                    .stream()
                    .filter(property -> !IGNORED_FIELDS.contains(property.getKey()))
                    .map(p -> extractField(openAPIRefResolver, p))
                    .collect(Collectors.toList());
        }
        dm.setFields(fields);
        return dm;
    }

    private FieldModel extractField(OpenAPIRefResolver openAPIRefResolver, Map.Entry<String, Schema> property) {
        var propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();

        TypeInfo typeInfo = OpenAPITypeUtils.extractType(openAPIRefResolver, valueSchema, false);
        var typeModel = typeInfo.getSimpleName();
        Object defaultValue;
        if ((valueSchema instanceof IntegerSchema) && valueSchema.getFormat() == null) {
            if (valueSchema.getDefault() == null) {
                defaultValue = 0;
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

    private StepModel extractStep(OpenAPIRefResolver openAPIRefResolver, Map.Entry<String, Schema> property) {
        var propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(openAPIRefResolver, valueSchema, false);
        var typeModel = typeInfo.getSimpleName();
        var value = makeValue(typeModel);
        return new StepModel(normalizeName(propertyName), typeModel, value);
    }

    private String makeValue(String type) {
        var result = "";
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
        var result = new StringBuilder().append("= ").append("new ").append(type);
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
        var value = name.replaceFirst("^get", "");
        return name.equals(value) ? value : StringUtils.capitalize(value);
    }
}
