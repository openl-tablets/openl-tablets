package org.openl.rules.openapi.impl;

import static org.openl.rules.openapi.impl.OpenAPITypeUtils.LINK_TO_DEFAULT_RUNTIME_CONTEXT;
import static org.openl.rules.openapi.impl.OpenAPITypeUtils.SCHEMAS_LINK;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.APPLICATION_JSON;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.TEXT_PLAIN;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.getSchemas;
import static org.openl.rules.openapi.impl.OpenLOpenAPIUtils.normalizeName;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import java.util.stream.Stream;

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
import org.openl.rules.variation.ArgumentReplacementVariation;
import org.openl.rules.variation.ComplexVariation;
import org.openl.rules.variation.DeepCloningVariation;
import org.openl.rules.variation.JXPathVariation;
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.Variation;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;
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
    public static final String RESULT = "Result";
    public static final Pattern PARAMETERS_BRACKETS_MATCHER = Pattern.compile("\\{.*?}");
    private static final Set<String> IGNORED_FIELDS = Collections
        .unmodifiableSet(new HashSet<>(Collections.singletonList("@class")));
    public static final String SPREADSHEET_RESULT_CLASS_NAME = SpreadsheetResult.class.getName();
    public static final String GET_PREFIX = "get";

    public static final Set<String> VARIATIONS_SCHEMAS_NAME = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(Variation.class.getSimpleName(),
            NoVariation.class.getSimpleName(),
            VariationsPack.class.getSimpleName(),
            ArgumentReplacementVariation.class.getSimpleName(),
            ComplexVariation.class.getSimpleName(),
            DeepCloningVariation.class.getSimpleName(),
            JXPathVariation.class.getSimpleName(),
            VariationsResult.class.getSimpleName())));

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

        boolean areVariationsProvided = OpenLOpenAPIUtils.checkVariations(openAPI, VARIATIONS_SCHEMAS_NAME);

        // collect all refs which are using variations
        final Set<String> ignoredRefs = VARIATIONS_SCHEMAS_NAME.stream()
            .map(s -> SCHEMAS_LINK + s)
            .collect(Collectors.toSet());

        Map<String, Integer> allUsedSchemaRefs = OpenLOpenAPIUtils.getAllUsedSchemaRefs(openAPI, jxPathContext);

        Map<String, Map<String, Integer>> pathsWithRequestsRefs = OpenLOpenAPIUtils.collectPathsWithParams(openAPI,
            jxPathContext);

        Set<String> pathsToIgnore = pathsWithRequestsRefs.entrySet().stream().filter(pathEntry -> {
            Map<String, Integer> usedSchemas = pathEntry.getValue();
            return usedSchemas.keySet().stream().anyMatch(ignoredRefs::contains);
        }).map(Map.Entry::getKey).collect(Collectors.toSet());

        if (areVariationsProvided) {
            pathsWithRequestsRefs.keySet().removeAll(pathsToIgnore);
        }

        Map<String, Integer> allUsedSchemaRefsInRequests = pathsWithRequestsRefs.values()
            .stream()
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));

        boolean isRuntimeContextProvided = allUsedSchemaRefsInRequests.keySet()
            .stream()
            .anyMatch(LINK_TO_DEFAULT_RUNTIME_CONTEXT::equals);

        Set<String> allUnusedRefs = OpenLOpenAPIUtils.getUnusedSchemaRefs(openAPI, allUsedSchemaRefs.keySet());

        Map<String, List<String>> childrenSchemas = OpenAPITypeUtils.getChildrenMap(openAPI);
        Set<String> parents = childrenSchemas.keySet();
        Set<String> childSet = childrenSchemas.values()
            .stream()
            .flatMap(Collection::stream)
            .map(OpenAPITypeUtils::getSimpleName)
            .collect(Collectors.toSet());

        Map<String, Set<String>> refsWithFields = OpenLOpenAPIUtils.getRefsInProperties(openAPI, jxPathContext);
        if (areVariationsProvided) {
            refsWithFields.entrySet().removeIf(entry -> {
                String refKey = entry.getKey();
                if (ignoredRefs.contains(refKey)) {
                    return true;
                }
                Set<String> fieldRefs = entry.getValue();
                if (fieldRefs.stream().anyMatch(ignoredRefs::contains)) {
                    ignoredRefs.add(refKey);
                    return true;
                }
                return false;
            });
        }
        Set<String> fieldsRefs = refsWithFields.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        // all the requests which were used only once per project needed to be extracted
        // if it's extends from other model it will be an inline type
        Set<String> refsToExpand = allUsedSchemaRefsInRequests.entrySet().stream().filter(refWithCount -> {
            String ref = refWithCount.getKey();
            Integer numberOfRefUsage = refWithCount.getValue();
            boolean refIsParentOrField = !parents.contains(ref) && !fieldsRefs.contains(ref);
            boolean refIsNotRuntimeContext = !ref.equals(LINK_TO_DEFAULT_RUNTIME_CONTEXT);
            return refIsNotRuntimeContext && numberOfRefUsage
                .equals(1) && (!allUsedSchemaRefs.containsKey(ref) || allUsedSchemaRefs.get(ref)
                    .equals(1)) && refIsParentOrField;
        }).map(Map.Entry::getKey).collect(Collectors.toSet());

        // path + schemas
        Map<String, Set<String>> allRefsInResponses = OpenLOpenAPIUtils.getAllUsedRefResponses(openAPI, jxPathContext);

        if (areVariationsProvided) {
            allRefsInResponses.keySet().removeAll(pathsToIgnore);
        }

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

        List<SpreadsheetParserModel> spreadsheetParserModels = extractSprModels(openAPI,
            jxPathContext,
            pathWithPotentialSprResult.keySet(),
            primitiveReturnsPaths,
            refsToExpand,
            spreadsheetPaths,
            childSet);
        Set<String> dataModelRefs = new HashSet<>();
        List<DataModel> dataModels = extractDataModels(spreadsheetParserModels,
            jxPathContext,
            openAPI,
            spreadsheetResultRefs,
            dataModelRefs);
        List<String> linkedRefs = spreadsheetParserModels.stream()
            .filter(SpreadsheetParserModel::isRefIsDataType)
            .map(SpreadsheetParserModel::getReturnRef)
            .collect(Collectors.toList());
        Set<String> datatypeRefs = allUsedSchemaRefs.keySet().stream().filter(x -> {
            boolean notSpreadsheetAndExpanded = !(spreadsheetResultRefs.contains(x) || refsToExpand.contains(x));
            boolean isIgnored = ignoredRefs.contains(x);
            boolean isNotReserved = !x.equals(SPR_RESULT_LINK);
            boolean notCalled = linkedRefs.contains(x);
            if (isIgnored) {
                return false;
            } else {
                return isNotReserved && (notSpreadsheetAndExpanded || notCalled);
            }
        }).collect(Collectors.toSet());
        Set<String> refSpreadsheets = spreadsheetParserModels.stream()
            .filter(x -> !x.isRefIsDataType() && x.getReturnRef() != null)
            .map(SpreadsheetParserModel::getReturnRef)
            .collect(Collectors.toSet());

        Set<String> allFieldsRefs = retrieveAllFieldsRefs(datatypeRefs, refsWithFields);
        // case when any datatype has a link in a field to the spreadsheet
        Set<String> dtToAdd = allFieldsRefs.stream().filter(x -> {
            boolean isNotSpreadsheetResult = !SPR_RESULT_LINK.equals(x);
            boolean isNotPresentedInDataTypes = !datatypeRefs.contains(x);
            boolean ignoredRef = ignoredRefs.contains(x);
            return isNotSpreadsheetResult && isNotPresentedInDataTypes && !ignoredRef;
        }).collect(Collectors.toSet());

        // If there is a datatype to add which was returned by any spreadsheet model, it will be transformed
        spreadsheetParserModels.stream().filter(x -> dtToAdd.contains(x.getReturnRef())).forEach(x -> {
            SpreadsheetModel model = x.getModel();
            String type = OpenAPITypeUtils.getSimpleName(x.getReturnRef());
            model.setType(type);
            model.getPathInfo().setReturnType(new TypeInfo(type, type, TypeInfo.Type.DATATYPE));
            model.setSteps(makeSingleStep(type));
        });
        fillCallsInSteps(spreadsheetParserModels, datatypeRefs, dataModelRefs, dtToAdd);

        datatypeRefs.addAll(dtToAdd);
        refSpreadsheets.removeAll(dtToAdd);

        Set<DatatypeModel> dts = new LinkedHashSet<>(extractDataTypeModels(jxPathContext, openAPI, datatypeRefs));
        allUnusedRefs.removeAll(ignoredRefs);
        dts.addAll(extractDataTypeModels(jxPathContext, openAPI, allUnusedRefs));

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
        createLostSpreadsheets(jxPathContext,
            openAPI,
            spreadsheetParserModels,
            refSpreadsheets,
            notUsedDataTypeWithRefToSpreadsheet,
            pathsWithRequestsRefs,
            isRuntimeContextProvided);
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
            .collect(Collectors.partitioningBy(spreadsheetModel -> containsRuntimeContext(
                pathsWithRequestsRefs.get(spreadsheetModel.getPathInfo().getOriginalPath()))));
        List<SpreadsheetModel> sprModelsWithRC = sprModelsDivided.get(Boolean.TRUE);

        // remove defaultRuntimeContext from dts - it will be generated automatically in the interface
        dts.removeIf(dt -> dt.getName().equals(OpenAPITypeUtils.DEFAULT_RUNTIME_CONTEXT));

        removeContextFromParams(sprModelsWithRC);
        return new ProjectModel(projectName,
            isRuntimeContextProvided,
            areVariationsProvided,
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
                    PathInfo pathInfo = sprModel.getPathInfo();
                    TypeInfo pathReturnType = pathInfo.getReturnType();
                    int dimension = pathReturnType.getDimension();
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

    private void createLostSpreadsheets(JXPathContext jxPathContext,
            OpenAPI openAPI,
            List<SpreadsheetParserModel> spreadsheetParserModels,
            Set<String> refSpreadsheets,
            List<String> notUsedDataTypeWithRefToSpreadsheet,
            Map<String, Map<String, Integer>> pathsWithRequestsRefs,
            boolean isRuntimeContextProvided) {
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
                        .map(x -> createStep(jxPathContext, spreadsheetParserModels, refSpreadsheets, modelName, x))
                        .collect(Collectors.toList());
                }
            }
            model.setSteps(steps);
            String originalPath = "/" + modelName;
            model.setPathInfo(new PathInfo(originalPath,
                modelName,
                PathInfo.Operation.POST,
                new TypeInfo(SPREADSHEET_RESULT_CLASS_NAME, SPREADSHEET_RESULT, TypeInfo.Type.SPREADSHEET)));
            lostModel.setModel(model);
            spreadsheetParserModels.add(lostModel);
            if (isRuntimeContextProvided) {
                if (!pathsWithRequestsRefs.containsKey(originalPath)) {
                    Map<String, Integer> mapWithRC = new HashMap<>();
                    mapWithRC.put(LINK_TO_DEFAULT_RUNTIME_CONTEXT, 1);
                    pathsWithRequestsRefs.put(originalPath, mapWithRC);
                }
            }
        }
    }

    private StepModel createStep(JXPathContext jxPathContext,
            List<SpreadsheetParserModel> spreadsheetParserModels,
            Set<String> refSpreadsheets,
            String modelName,
            Map.Entry<String, Schema> x) {
        StepModel step = extractStep(jxPathContext, x);
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(jxPathContext, x.getValue(), false);
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
            Optional<SpreadsheetParserModel> optionalModel = Optional.empty();
            for (SpreadsheetParserModel parserModel : spreadsheetParserModels) {
                int dimension = parserModel.getModel().getPathInfo().getReturnType().getDimension();
                String returnRef = parserModel.getReturnRef();
                if (returnRef != null && returnRef.equals(SCHEMAS_LINK + type) && dimension == 0) {
                    optionalModel = Optional.of(parserModel);
                    break;
                }
            }
            if (optionalModel.isPresent()) {
                SpreadsheetParserModel spreadsheetParserModel = optionalModel.get();
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
        String call = makeCall(modelToCall, value);
        if (stepType.endsWith("[]")) {
            step.setValue(makeArrayCall(stepType, modelToCall, call));
        } else {
            step.setValue("= " + call);
        }
        return step;
    }

    private List<DataModel> extractDataModels(List<SpreadsheetParserModel> spreadsheetModels,
            JXPathContext jxPathContext,
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
            String operationMethod = potentialDataTablePathInfo.getOperation().name();
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
                    isSimpleType ? createSimpleModel(
                        type) : createModelForDataTable(jxPathContext, openAPI, type, getSchemas(openAPI).get(type)));

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
            Set<String> dataModelRefs,
            Set<String> lostDt) {
        Set<String> calledRefs = new HashSet<>();
        final Set<String> fixedDataTypes = Stream.concat(dataModelRefs.stream(), lostDt.stream())
            .collect(Collectors.toSet());
        // return type + spreadsheet name
        Set<Pair<String, String>> sprResultNames = new HashSet<>();
        for (SpreadsheetParserModel model : models) {
            String returnRef = model.getReturnRef();
            if (returnRef != null && model.isRefIsDataType() && models.stream()
                .anyMatch(x -> returnRef.equals(x.getReturnRef()) && !x.isRefIsDataType()) && !fixedDataTypes
                    .contains(returnRef)) {
                datatypeRefs.remove(returnRef);
            }
        }
        final Set<String> datatypeNames = Stream.concat(datatypeRefs.stream(), fixedDataTypes.stream())
            .collect(Collectors.toSet())
            .stream()
            .map(ref -> OpenAPITypeUtils.getSimpleName(ref).toLowerCase())
            .collect(Collectors.toSet());

        Set<String> reservedWords = new HashSet<>(datatypeNames);
        Map<String, Set<String>> spreadsheetWithParameterNames = new HashMap<>();

        for (SpreadsheetParserModel model : models) {
            SpreadsheetModel spreadsheetModel = model.getModel();
            Set<String> parameterNames = spreadsheetModel.getParameters()
                .stream()
                .map(InputParameter::getFormattedName)
                .collect(Collectors.toSet());
            String spreadsheetType = spreadsheetModel.getType();
            String returnRef = model.getReturnRef();
            final String spreadsheetName = spreadsheetModel.getName();
            PathInfo pathInfo = spreadsheetModel.getPathInfo();
            final String lowerCasedSpreadsheetName = spreadsheetName.toLowerCase();
            boolean spreadsheetWithSameNameAndParametersExists = spreadsheetWithParameterNames
                .containsKey(lowerCasedSpreadsheetName) && spreadsheetWithParameterNames.get(lowerCasedSpreadsheetName)
                    .equals(parameterNames);
            if (spreadsheetWithSameNameAndParametersExists && returnRef == null) {
                String name = makeName(spreadsheetModel.getName(), reservedWords);
                spreadsheetModel.setName(name);
                pathInfo.setFormattedPath(name);
            } else if (returnRef != null && (SPREADSHEET_RESULT.equals(spreadsheetType) || !datatypeRefs
                .contains(SCHEMAS_LINK + OpenAPITypeUtils.removeArrayBrackets(spreadsheetType)))) {
                TypeInfo returnType = pathInfo.getReturnType();
                if (returnType.getDimension() == 0 && (datatypeNames
                    .contains(lowerCasedSpreadsheetName) || spreadsheetWithSameNameAndParametersExists)) {
                    String modifiedName = findSpreadsheetResultName(reservedWords, returnRef);
                    spreadsheetModel.setName(modifiedName);
                    returnType.setJavaName(OpenAPITypeUtils.getSpreadsheetArrayClassName(returnType.getDimension()));
                    pathInfo.setFormattedPath(modifiedName);
                }
                sprResultNames.add(Pair.of(returnType.getSimpleName(), spreadsheetModel.getName()));
            }
            spreadsheetWithParameterNames.put(spreadsheetModel.getName().toLowerCase(), parameterNames);
            reservedWords.add(spreadsheetModel.getName().toLowerCase());
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
                int dimension = existingPathInfo.getReturnType().getDimension();
                if (dimension > 0) {
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
                if (sprResultNames.stream().anyMatch(x -> x.getKey().equals(type))) {
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
                        foundSpr = models.stream().filter(sprModel -> {
                            boolean typesAreTheSame = sprModel.getReturnRef() != null && type
                                .equals(OpenAPITypeUtils.getSimpleName(sprModel.getReturnRef()));
                            boolean notItSelf = !sprModel.getModel().getName().equals(spreadsheetModel.getName());
                            boolean isSpreadsheetResult = sprModel.getModel().getType().equals(SPREADSHEET_RESULT);
                            return typesAreTheSame && notItSelf && isSpreadsheetResult;
                        }).findAny();
                    }
                    // the called spreadsheet was found
                    if (foundSpr.isPresent()) {
                        SpreadsheetParserModel calledSpr = foundSpr.get();
                        String calledRef = calledSpr.getReturnRef();
                        calledRefs.add(calledRef);

                        SpreadsheetModel calledModel = calledSpr.getModel();
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

    private String findSpreadsheetResultName(Set<String> reservedWords, String returnRef) {
        return findSpreadsheetName(returnRef, reservedWords);
    }

    private String findSpreadsheetName(final String returnRef, final Set<String> reservedNames) {
        String nameCandidate = OpenAPITypeUtils.getSimpleName(returnRef);
        return makeName(nameCandidate, reservedNames);
    }

    private String makeName(String candidate, final Set<String> reservedWords) {
        if (CollectionUtils.isNotEmpty(reservedWords) && reservedWords.contains(candidate.toLowerCase())) {
            candidate = candidate + "1";
            return makeName(candidate, reservedWords);
        }
        return candidate;
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

    public boolean containsRuntimeContext(final Map<String, Integer> inputParametersEntry) {
        return inputParametersEntry != null && inputParametersEntry.containsKey(LINK_TO_DEFAULT_RUNTIME_CONTEXT);
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
            Set<String> childSet) {
        List<SpreadsheetParserModel> spreadSheetModels = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            extractSpreadsheets(openAPI,
                jxPathContext,
                pathWithPotentialSprResult,
                refsToExpand,
                spreadSheetModels,
                paths,
                PathType.SPREADSHEET_RESULT_PATH,
                childSet);
            extractSpreadsheets(openAPI,
                jxPathContext,
                pathsWithPrimitiveReturns,
                refsToExpand,
                spreadSheetModels,
                paths,
                PathType.SIMPLE_RETURN_PATH,
                childSet);
            extractSpreadsheets(openAPI,
                jxPathContext,
                pathsWithSpreadsheets,
                refsToExpand,
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
            Set<String> childSet) {
        SpreadsheetParserModel spreadsheetParserModel = new SpreadsheetParserModel();
        SpreadsheetModel spr = new SpreadsheetModel();
        spreadsheetParserModel.setModel(spr);
        PathInfo pathInfo = generatePathInfo(path, pathItem);
        spr.setPathInfo(pathInfo);
        Schema<?> responseSchema = OpenLOpenAPIUtils.getUsedSchemaInResponse(jxPathContext, pathItem);
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(jxPathContext, responseSchema, false);
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
            .extractParameters(jxPathContext, openAPI, refsToExpand, pathItem);
        String normalizedPath = replaceBrackets(path);
        String formattedName = normalizeName(normalizedPath);
        spr.setName(formattedName);
        spr.setParameters(parameters);
        pathInfo.setFormattedPath(formattedName);
        List<StepModel> stepModels = getStepModels(openAPI,
            jxPathContext,
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
        pathInfo.setOperation(Optional.ofNullable(operation.getMethod())
            .map(String::toUpperCase)
            .map(PathInfo.Operation::valueOf)
            .orElseThrow(() -> new IllegalArgumentException("Invalid method operation")));
        pathInfo.setConsumes(operation.getConsumes());
        pathInfo.setProduces(operation.getProduces());
        return pathInfo;
    }

    private List<StepModel> getStepModels(OpenAPI openAPI,
            JXPathContext jxPathContext,
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
                            .map(p -> extractStep(jxPathContext, p))
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

    private List<DatatypeModel> extractDataTypeModels(JXPathContext jxPathContext,
            OpenAPI openAPI,
            Set<String> allTheRefsWhichAreDataTypes) {
        List<DatatypeModel> result = new ArrayList<>();
        for (String datatypeRef : allTheRefsWhichAreDataTypes) {
            Schema<?> schema = (Schema<?>) OpenLOpenAPIUtils.resolveByRef(jxPathContext, datatypeRef);
            if (schema != null && OpenAPITypeUtils.isComplexSchema(jxPathContext, schema)) {
                DatatypeModel dm = createModel(jxPathContext,
                    openAPI,
                    OpenAPITypeUtils.getSimpleName(datatypeRef),
                    schema);
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

    private DatatypeModel createModel(JXPathContext jxPathContext,
            OpenAPI openAPI,
            String schemaName,
            Schema<?> schema) {
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
            fields = properties.entrySet().stream().filter(property -> {
                boolean isIgnoredField = IGNORED_FIELDS.contains(property.getKey());
                String ref = property.getValue().get$ref();
                boolean isRuntimeContext = ref != null && ref.equals(LINK_TO_DEFAULT_RUNTIME_CONTEXT);
                return !(isIgnoredField || isRuntimeContext);
            }).map(p -> extractField(jxPathContext, p)).collect(Collectors.toList());
        }
        dm.setFields(fields);
        return dm;
    }

    private DatatypeModel createModelForDataTable(JXPathContext jxPathContext,
            OpenAPI openAPI,
            String schemaName,
            Schema<?> schema) {
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
                .map(p -> extractField(jxPathContext, p))
                .collect(Collectors.toList());
        }
        dm.setFields(fields);
        return dm;
    }

    private FieldModel extractField(JXPathContext jxPathContext, Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();

        TypeInfo typeInfo = OpenAPITypeUtils.extractType(jxPathContext, valueSchema, false);
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

    private StepModel extractStep(JXPathContext jxPathContext, Map.Entry<String, Schema> property) {
        String propertyName = property.getKey();
        Schema<?> valueSchema = property.getValue();
        TypeInfo typeInfo = OpenAPITypeUtils.extractType(jxPathContext, valueSchema, false);
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
