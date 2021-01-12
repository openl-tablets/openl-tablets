package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenAPITypeUtils.SCHEMAS_LINK;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.APPLICATION_JSON;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.TEXT_PLAIN;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.normalizeName;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.jxpath.JXPathContext;
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
    public static final String SPR_RESULT_LINK = SCHEMAS_LINK + SPREADSHEET_RESULT;
    public static final String ANY_SPREADSHEET_RESULT = "AnySpreadsheetResult";
    public static final String RESULT = "Result";
    public static final Pattern PARAMETERS_BRACKETS_MATCHER = Pattern.compile("\\{.*?}");
    private static final Set<String> IGNORED_FIELDS = Collections
        .unmodifiableSet(new HashSet<>(Collections.singletonList("@class")));
    public static final String SPREADSHEET_RESULT_CLASS_NAME = SpreadsheetResult.class.getName();
    public static final String GET_PREFIX = "get";

    public OpenAPIScaffoldingConverter() {
        // default constructor
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

        Set<DatatypeModel> dts = new LinkedHashSet<>();
        List<SpreadsheetParserModel> spreadsheetParserModels = extractSprModels(openAPI,
            jxPathContext,
            pathWithPotentialSprResult.keySet(),
            primitiveReturnsPaths,
            refsToExpand,
            spreadsheetPaths,
            dts,
            childSet);
        Set<String> dataModelRefs = new HashSet<>();
        List<DataModel> dataModels = extractDataModels(spreadsheetParserModels,
            openAPI,
            spreadsheetResultRefs,
            dataModelRefs);
        List<String> linkedRefs = spreadsheetParserModels.stream()
            .filter(SpreadsheetParserModel::isRefIsDataType)
            .map(SpreadsheetParserModel::getReturnRef)
            .collect(Collectors.toList());
        Set<String> datatypeRefs = allUsedSchemaRefs.keySet().stream().filter(x -> {
            boolean notSpreadsheetAndExpanded = !(spreadsheetResultRefs.contains(x) || refsToExpand.contains(x));
            boolean isNotReserved = !x.equals(SPR_RESULT_LINK);
            boolean notCalled = linkedRefs.contains(x);
            return isNotReserved && (notSpreadsheetAndExpanded || notCalled);
        }).collect(Collectors.toSet());
        Set<String> refSpreadsheets = spreadsheetParserModels.stream()
            .filter(x -> !x.isRefIsDataType() && x.getReturnRef() != null)
            .map(SpreadsheetParserModel::getReturnRef)
            .collect(Collectors.toSet());

        Set<String> allFieldsRefs = retrieveAllFieldsRefs(datatypeRefs, refsWithFields);
        // case when any datatype has a link in a field to the spreadsheet
        Set<String> dtToAdd = allFieldsRefs.stream()
            .filter(x -> !SPR_RESULT_LINK.equals(x) && !datatypeRefs.contains(x))
            .collect(Collectors.toSet());

        // If there is a datatype to add which was returned by any spreadsheet model, it will be transformed
        spreadsheetParserModels.stream().filter(x -> dtToAdd.contains(x.getReturnRef())).forEach(x -> {
            SpreadsheetModel model = x.getModel();
            String type = OpenAPITypeUtils.getSimpleName(x.getReturnRef());
            model.setType(type);
            model.getPathInfo().setReturnType(new TypeInfo(type, type, TypeInfo.Type.DATATYPE));
            model.setSteps(makeSingleStep(type));
        });
        fillCallsInSteps(spreadsheetParserModels, datatypeRefs, dataModelRefs);

        datatypeRefs.addAll(dtToAdd);
        refSpreadsheets.removeAll(dtToAdd);

        dts.addAll(extractDataTypeModels(openAPI, datatypeRefs, false));
        dts.addAll(extractDataTypeModels(openAPI, allUnusedRefs, true));

        Set<String> usedInDataTypes = new HashSet<>();
        // searching for links in data types
        dts.forEach(dt -> {
            Set<String> set = dt.getFields().stream().map(FieldModel::getType).collect(Collectors.toSet());
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
        List<String> notUsedDataTypeWithRefToSpreadsheet = dts.stream()
            .filter(x -> !usedInDataTypes.contains(x.getName()) && !ANY_SPREADSHEET_RESULT.equals(x.getName()))
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
        createLostSpreadsheets(openAPI, spreadsheetParserModels, refSpreadsheets, notUsedDataTypeWithRefToSpreadsheet);
        // change steps with in the spreadsheets to these potential models
        setCallsAndReturnTypeToLostSpreadsheet(spreadsheetParserModels, notUsedDataTypeWithRefToSpreadsheet);

        Set<String> dtNames = dts.stream().map(DatatypeModel::getName).collect(Collectors.toSet());
        checkTypes(spreadsheetParserModels, dtNames);

        final Consumer<MethodModel> applyInclude = method -> {
            method.setInclude(openAPI.getPaths().get(method.getPathInfo().getOriginalPath()) != null);
        };
        List<SpreadsheetModel> spreadsheetModels = spreadsheetParserModels.stream()
            .map(SpreadsheetParserModel::getModel)
            .collect(Collectors.toList());
        spreadsheetModels.forEach(applyInclude);
        dataModels.forEach(applyInclude);

        Map<Boolean, List<SpreadsheetModel>> sprModelsDivided = spreadsheetModels.stream()
            .collect(Collectors.partitioningBy(x -> containsRuntimeContext(x.getParameters())));
        List<SpreadsheetModel> sprModelsWithRC = sprModelsDivided.get(Boolean.TRUE);
        boolean isRuntimeContextProvided = !sprModelsWithRC.isEmpty() || (!dataModels.isEmpty() && dataModels.stream()
            .allMatch(dt -> dt.getPathInfo().getRuntimeContextParameter() != null));

        if (isRuntimeContextProvided) {
            // remove defaultRuntimeContext from dts - it will be generated automatically in the interface
            dts.removeIf(dt -> dt.getName().equals(OpenAPITypeUtils.DEFAULT_RUNTIME_CONTEXT));
        }
        removeContextFromParams(sprModelsWithRC);
        return new ProjectModel(projectName,
            isRuntimeContextProvided,
            dts,
            dataModels,
            isRuntimeContextProvided ? sprModelsWithRC : spreadsheetModels,
            isRuntimeContextProvided ? sprModelsDivided.get(Boolean.FALSE) : Collections.emptyList());
    }

    private Set<String> retrieveAllFieldsRefs(Set<String> datatypeRefs, Map<String, Set<String>> refsWithFields) {
        Set<String> allFieldsRefs = new HashSet<>();
        Queue<String> queue = new LinkedList<>(datatypeRefs);
        while (!queue.isEmpty()) {
            final String dtRef = queue.poll();
            refsWithFields.getOrDefault(dtRef, Collections.emptySet())
                .stream()
                .filter(x -> !datatypeRefs.contains(x) && !allFieldsRefs.contains(x))
                .filter(allFieldsRefs::add)
                .forEach(queue::add);
        }
        return allFieldsRefs;
    }

    private void checkTypes(List<SpreadsheetParserModel> parserModels, Set<String> dataTypeNames) {
        for (SpreadsheetParserModel parserModel : parserModels) {
            SpreadsheetModel model = parserModel.getModel();
            PathInfo pathInfo = model.getPathInfo();
            if (pathInfo != null) {
                TypeInfo returnType = pathInfo.getReturnType();
                if (dataTypeNames.contains(OpenAPITypeUtils.removeArrayBrackets(returnType.getSimpleName()))) {
                    returnType.setType(TypeInfo.Type.DATATYPE);
                }
            }

            List<InputParameter> parameters = model.getParameters();
            for (InputParameter parameter : parameters) {
                TypeInfo type = parameter.getType();
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
                SpreadsheetModel sprModel = spreadsheetParserModel.getModel();
                String returnType = OpenAPITypeUtils.removeArrayBrackets(sprModel.getType());
                if (notUsedDataTypeWithRefToSpreadsheet.contains(returnType)) {
                    sprModel.setType(SPREADSHEET_RESULT);
                }
                for (StepModel model : sprModel.getSteps()) {
                    String type = model.getType();
                    String simpleType = OpenAPITypeUtils.removeArrayBrackets(type);
                    if (notUsedDataTypeWithRefToSpreadsheet.contains(simpleType)) {
                        String call = makeCall(type, "");
                        model.setValue(type.endsWith("[]") ? makeArrayCall(type, simpleType, "") : "= " + call);
                    }
                }
            }
        }
    }

    private void createLostSpreadsheets(OpenAPI openAPI,
            List<SpreadsheetParserModel> spreadsheetParserModels,
            Set<String> refSpreadsheets,
            List<String> notUsedDataTypeWithRefToSpreadsheet) {
        for (String modelName : notUsedDataTypeWithRefToSpreadsheet) {
            SpreadsheetParserModel lostModel = new SpreadsheetParserModel();
            SpreadsheetModel model = new SpreadsheetModel();
            model.setName(modelName);
            model.setType(SPREADSHEET_RESULT);
            model.setParameters(Collections.emptyList());
            Schema<?> schema = getSchemas(openAPI).get(modelName);
            List<StepModel> steps = new ArrayList<>();
            if (schema != null) {
                Map<String, Schema> properties = schema.getProperties();
                if (CollectionUtils.isNotEmpty(properties)) {
                    steps = properties.entrySet()
                        .stream()
                        .filter(x -> !IGNORED_FIELDS.contains(x.getKey()))
                        .map(x -> createStep(spreadsheetParserModels, refSpreadsheets, modelName, x))
                        .collect(Collectors.toList());
                }
            }
            model.setSteps(steps);
            model.setPathInfo(new PathInfo("/" + modelName,
                modelName,
                PathItem.HttpMethod.POST.name(),
                new TypeInfo(SPREADSHEET_RESULT_CLASS_NAME, SPREADSHEET_RESULT, TypeInfo.Type.SPREADSHEET)));
            lostModel.setModel(model);
            spreadsheetParserModels.add(lostModel);
        }
    }

    private StepModel createStep(List<SpreadsheetParserModel> spreadsheetParserModels,
            Set<String> refSpreadsheets,
            String modelName,
            Map.Entry<String, Schema> x) {
        StepModel step = extractStep(x);
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(x.getValue(), false);
        String stepType = typeInfo.getSimpleName();
        String type = OpenAPITypeUtils.removeArrayBrackets(stepType);
        String modelToCall = "";
        String value = "";
        if (!type.equals(modelName) && !refSpreadsheets.contains(SCHEMAS_LINK + type)) {
            return step;
        }
        if (type.equals(modelName)) {
            modelToCall = modelName;
        } else {
            Optional<SpreadsheetParserModel> optionalModel = spreadsheetParserModels.stream()
                .filter(z -> z.getReturnRef() != null && z.getReturnRef().equals(SCHEMAS_LINK + type))
                .findFirst();
            if (optionalModel.isPresent()) {
                SpreadsheetParserModel spreadsheetParserModel = optionalModel.get();
                modelToCall = spreadsheetParserModel.getModel().getName();
                value = spreadsheetParserModel.getModel()
                    .getParameters()
                    .stream()
                    .map(InputParameter::getType)
                    .map(OpenAPITypeUtils::getJavaDefaultValue)
                    .collect(Collectors.joining(", "));
            }
        }
        String call = makeCall(modelToCall, value);
        if (stepType.endsWith("[]")) {
            step.setValue(makeArrayCall(stepType, modelToCall, call));
        } else {
            step.setValue("= " + call);
        }
        return step;
    }

    private List<DataModel> extractDataModels(List<SpreadsheetParserModel> spreadsheetModels,
            OpenAPI openAPI,
            Set<String> sprResultRefs,
            Set<String> dataModelsRefs) {
        List<SpreadsheetParserModel> potentialDataModels = spreadsheetModels.stream()
            .filter(x -> x.getModel()
                .getPathInfo()
                .getFormattedPath()
                .startsWith(GET_PREFIX) && (CollectionUtils
                    .isEmpty(x.getModel().getParameters()) || containsOnlyRuntimeContext(x.getModel().getParameters())))
            .collect(Collectors.toList());
        List<DataModel> dataModels = new ArrayList<>();
        for (SpreadsheetParserModel potentialDataModel : potentialDataModels) {
            String originalType = potentialDataModel.getModel().getType();
            String type = OpenAPITypeUtils.removeArrayBrackets(originalType);
            if (!originalType.endsWith("[]") || type.equals(SPREADSHEET_RESULT)) {
                continue;
            }
            PathInfo potentialDataTablePathInfo = potentialDataModel.getModel().getPathInfo();
            String operationMethod = potentialDataTablePathInfo.getOperation();
            // if get operation without parameters or post with only runtime context
            List<InputParameter> parameters = potentialDataModel.getModel().getParameters();
            boolean parametersNotEmpty = CollectionUtils.isNotEmpty(parameters);
            boolean getAndNoParams = parameters.isEmpty() && operationMethod.equals(PathItem.HttpMethod.GET.name());
            boolean postAndRuntimeContext = parametersNotEmpty && operationMethod
                .equals(PathItem.HttpMethod.POST.name());
            if (getAndNoParams || postAndRuntimeContext) {
                String returnRef = potentialDataModel.getReturnRef();
                if (returnRef != null) {
                    sprResultRefs.remove(returnRef);
                    dataModelsRefs.add(returnRef);
                }
                spreadsheetModels.remove(potentialDataModel);
                String dataTableName = formatTableName(potentialDataModel.getModel().getName());
                potentialDataTablePathInfo.setFormattedPath(GET_PREFIX + dataTableName);

                boolean isSimpleType = OpenAPITypeUtils.isSimpleType(type);
                DataModel dataModel = new DataModel(dataTableName,
                    type,
                    potentialDataTablePathInfo,
                    isSimpleType ? createSimpleModel(type)
                                 : createModelForDataTable(openAPI, type, getSchemas(openAPI).get(type)));

                TypeInfo.Type resultType = isSimpleType ? TypeInfo.Type.OBJECT : TypeInfo.Type.DATATYPE;
                dataModel.getPathInfo().getReturnType().setType(resultType);
                if (parametersNotEmpty) {
                    dataModel.getPathInfo().setRuntimeContextParameter(parameters.iterator().next());
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
            Set<String> dataModelRefs) {
        Set<String> calledRefs = new HashSet<>();
        // return type + spreadsheet name
        Set<Pair<String, String>> sprResultNames = new HashSet<>();
        for (SpreadsheetParserModel model : models) {
            String returnRef = model.getReturnRef();
            if (returnRef != null && model.isRefIsDataType() && models.stream()
                .anyMatch(x -> returnRef.equals(x.getReturnRef()) && !x.isRefIsDataType()) && !dataModelRefs
                    .contains(returnRef)) {
                datatypeRefs.remove(returnRef);
            }
        }
        final Set<String> datatypeNames = datatypeRefs.stream()
            .map(OpenAPITypeUtils::getSimpleName)
            .collect(Collectors.toSet());

        for (SpreadsheetParserModel model : models) {
            SpreadsheetModel spreadsheetModel = model.getModel();
            String spreadsheetType = spreadsheetModel.getType();
            if (model.getReturnRef() != null && (SPREADSHEET_RESULT.equals(spreadsheetType) || !datatypeRefs
                .contains(SCHEMAS_LINK + OpenAPITypeUtils.removeArrayBrackets(spreadsheetType)))) {
                sprResultNames.add(Pair.of(spreadsheetModel.getPathInfo().getReturnType().getSimpleName(),
                    spreadsheetModel.getName()));
            }
        }
        for (SpreadsheetParserModel parserModel : models) {
            SpreadsheetModel spreadsheetModel = parserModel.getModel();
            String returnType = spreadsheetModel.getType();
            String refType = parserModel.getReturnRef() != null
                                                                ? OpenAPITypeUtils
                                                                    .getSimpleName(parserModel.getReturnRef())
                                                                : "";
            Optional<Pair<String, String>> willBeCalled = sprResultNames.stream()
                .filter(p -> p.getKey().equals(refType) && !p.getValue().equals(spreadsheetModel.getName()))
                .findAny();
            PathInfo existingPathInfo = spreadsheetModel.getPathInfo();
            if (willBeCalled.isPresent()) {
                // change return type if the array of spreadsheets will be returned
                boolean isArray = returnType.endsWith("[]");
                if (isArray) {
                    int dimension = existingPathInfo.getReturnType().getDimension();
                    spreadsheetModel.setType(SPREADSHEET_RESULT + willBeCalled.get().getValue() + String.join("",
                        Collections.nCopies(dimension, "[]")));
                    existingPathInfo.getReturnType()
                        .setJavaName(OpenAPITypeUtils.getSpreadsheetArrayClassName(dimension));
                }
            }
            for (StepModel step : spreadsheetModel.getSteps()) {
                String stepType = step.getType();
                boolean isArray = stepType.endsWith("[]");
                String type = OpenAPITypeUtils.removeArrayBrackets(step.getType());
                if (type.equals(ANY_SPREADSHEET_RESULT)) {
                    step.setValue(isArray ? makeArrayCall(stepType, "", "") : createNewInstance(SPREADSHEET_RESULT));
                } else if (sprResultNames.stream().anyMatch(x -> x.getKey().equals(type))) {
                    Optional<SpreadsheetParserModel> foundSpr = Optional.empty();
                    if (willBeCalled.isPresent()) {
                        Pair<String, String> called = willBeCalled.get();
                        String calledType = called.getKey();
                        // if step type equals to the returned type of spreadsheet
                        if (type.equals(calledType)) {
                            foundSpr = models.stream()
                                .filter(x -> x.getModel().getName().equals(called.getRight()))
                                .findFirst();
                        }
                    }
                    // the called spreadsheet isn't returned by the model
                    if (Objects.equals(foundSpr, Optional.empty())) {
                        foundSpr = models.stream()
                            .filter(x -> x.getReturnRef() != null && type
                                .equals(OpenAPITypeUtils.getSimpleName(x.getReturnRef())) && !x.getModel()
                                    .getName()
                                    .equals(spreadsheetModel.getName()) && x.getModel()
                                        .getType()
                                        .startsWith(SPREADSHEET_RESULT))
                            .findAny();
                    }
                    // the called spreadsheet was found
                    if (foundSpr.isPresent()) {
                        SpreadsheetParserModel calledSpr = foundSpr.get();
                        String calledRef = calledSpr.getReturnRef();
                        calledRefs.add(calledRef);

                        SpreadsheetModel calledModel = calledSpr.getModel();
                        if (datatypeNames.contains(calledModel.getName())) {
                            String name = OpenAPITypeUtils.getSimpleName(calledRef);
                            calledModel.setName(name);
                            PathInfo calledPathInfo = calledModel.getPathInfo();
                            calledPathInfo.getReturnType()
                                .setJavaName(OpenAPITypeUtils
                                    .getSpreadsheetArrayClassName(calledPathInfo.getReturnType().getDimension()));
                            calledPathInfo.setFormattedPath(name);

                            if (calledRef.equals(parserModel.getReturnRef())) {
                                int dimension = existingPathInfo.getReturnType().getDimension();
                                spreadsheetModel.setType(
                                    SPREADSHEET_RESULT + name + String.join("", Collections.nCopies(dimension, "[]")));
                                existingPathInfo.getReturnType()
                                    .setJavaName(OpenAPITypeUtils.getSpreadsheetArrayClassName(dimension));
                            }
                        }
                        List<InputParameter> parameters = calledModel.getParameters();
                        String value = parameters.stream()
                            .map(InputParameter::getType)
                            .filter(t -> t.getType() != TypeInfo.Type.RUNTIMECONTEXT)
                            .map(OpenAPITypeUtils::getJavaDefaultValue)
                            .collect(Collectors.joining(", "));
                        String calledName = calledModel.getName();
                        String call = makeCall(calledName, value);
                        step.setValue(isArray ? makeArrayCall(stepType, calledName, call) : "= " + call);
                    }
                }
            }
        }
        return calledRefs;
    }

    private String makeArrayCall(String stepType, String name, String call) {
        int dimension = calculateDimension(stepType);
        String openingBrackets = String.join("", Collections.nCopies(dimension, "{"));
        String closingBrackets = String.join("", Collections.nCopies(dimension, "}"));
        String arrayBrackets = String.join("", Collections.nCopies(dimension, "[]"));
        return new StringBuilder().append("= new SpreadsheetResult")
            .append(name)
            .append(arrayBrackets)
            .append(openingBrackets)
            .append(call)
            .append(closingBrackets)
            .toString();
    }

    private int calculateDimension(String stepType) {
        int count = 0;
        boolean brackets = false;
        for (int i = 0; i < stepType.length(); i++) {
            char c = stepType.charAt(i);
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
            .anyMatch(x -> x.getType().getType() == TypeInfo.Type.RUNTIMECONTEXT);
    }

    public boolean containsOnlyRuntimeContext(final Collection<InputParameter> inputParameters) {
        return CollectionUtils.isNotEmpty(inputParameters) && inputParameters.size() == 1 && inputParameters.stream()
            .anyMatch(x -> x.getType().getType() == TypeInfo.Type.RUNTIMECONTEXT);
    }

    private List<SpreadsheetParserModel> extractSprModels(OpenAPI openAPI,
            JXPathContext jxPathContext,
            Set<String> pathWithPotentialSprResult,
            Set<String> pathsWithPrimitiveReturns,
            Set<String> refsToExpand,
            Set<String> pathsWithSpreadsheets,
            Set<DatatypeModel> dts,
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
            Set<DatatypeModel> dts,
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
            Set<DatatypeModel> dts,
            Set<String> childSet) {
        SpreadsheetParserModel spreadsheetParserModel = new SpreadsheetParserModel();
        SpreadsheetModel spr = new SpreadsheetModel();
        spreadsheetParserModel.setModel(spr);
        PathInfo pathInfo = generatePathInfo(path, pathItem);
        spr.setPathInfo(pathInfo);
        Schema<?> responseSchema = OpenLOpenAPIUtils.getUsedSchemaInResponse(jxPathContext, pathItem);
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(responseSchema, false);
        if (PathType.SPREADSHEET_RESULT_PATH.equals(pathType)) {
            typeInfo = new TypeInfo(SPREADSHEET_RESULT_CLASS_NAME,
                typeInfo.getSimpleName(),
                TypeInfo.Type.SPREADSHEET,
                typeInfo.getDimension(),
                typeInfo.isReference());
        }
        String usedSchemaInResponse = typeInfo.getSimpleName();
        pathInfo.setReturnType(typeInfo);
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
            boolean isChild) {
        List<StepModel> stepModels = new ArrayList<>();
        boolean isArray = usedSchemaInResponse.endsWith("[]");
        Schema<?> schema;
        if (PathType.SPREADSHEET_RESULT_PATH == pathType) {
            final String nameOfSchema = isArray ? OpenAPITypeUtils.removeArrayBrackets(usedSchemaInResponse)
                                                : usedSchemaInResponse;
            schema = getSchemas(openAPI).get(nameOfSchema);
            boolean isArrayOrChild = isArray || isChild;
            spr.setType(isArrayOrChild ? usedSchemaInResponse : SPREADSHEET_RESULT);
            if (schema != null) {
                if (isArrayOrChild) {
                    stepModels = makeSingleStep(usedSchemaInResponse);
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
                    .anyMatch(x -> OpenAPITypeUtils.removeArrayBrackets(x.getType()).equals(nameOfSchema));
                spreadsheetParserModel.setStoreInModels(addToDataTypes || isArrayOrChild);
            }
            spreadsheetParserModel.setReturnRef(SCHEMAS_LINK + nameOfSchema);
        } else {
            spr.setType(usedSchemaInResponse);
            stepModels = makeSingleStep(usedSchemaInResponse);
        }
        return stepModels;
    }

    private List<StepModel> makeSingleStep(String stepType) {
        return Collections
            .singletonList(new StepModel(OpenAPIScaffoldingConverter.RESULT, stepType, makeValue(stepType)));
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
                schemaName = OpenAPITypeUtils.getSimpleName(datatypeRef);
            }
            Schema<?> schema = getSchemas(openAPI).get(schemaName);
            if (schema != null) {
                DatatypeModel dm = createModel(openAPI, schemaName, schema);
                result.add(dm);
            }
        }
        return result;
    }

    private DatatypeModel createSimpleModel(String type) {
        DatatypeModel dm = new DatatypeModel("");
        dm.setFields(Collections.singletonList(new FieldModel("this", type)));
        return dm;
    }

    private DatatypeModel createModel(OpenAPI openAPI, String schemaName, Schema<?> schema) {
        DatatypeModel dm = new DatatypeModel(normalizeName(schemaName));
        Map<String, Schema> properties;
        List<FieldModel> fields = new ArrayList<>();
        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
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
            fields = properties.entrySet()
                .stream()
                .filter(property -> !IGNORED_FIELDS.contains(property.getKey()))
                .map(this::extractField)
                .collect(Collectors.toList());
        }
        dm.setFields(fields);
        return dm;
    }

    private DatatypeModel createModelForDataTable(OpenAPI openAPI, String schemaName, Schema<?> schema) {
        DatatypeModel dm = new DatatypeModel(normalizeName(schemaName));
        Map<String, Schema> properties;
        List<FieldModel> fields = new ArrayList<>();
        if (schema instanceof ComposedSchema) {
            properties = OpenAPITypeUtils.getAllProperties((ComposedSchema) schema, openAPI);
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

        TypeInfo typeInfo = OpenAPITypeUtils.extractType(valueSchema, false);
        String typeModel = typeInfo.getSimpleName();
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
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(valueSchema, false);
        String typeModel = typeInfo.getSimpleName();
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
        StringBuilder result = new StringBuilder().append("= ").append("new ").append(type);
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
}
