package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

public class OpenAPIConverterTest {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";

    private OpenAPIModelConverter converter;

    @Before
    public void setUp() {
        converter = new OpenAPIScaffoldingConverter();
    }

    @Test
    public void testAutoPolicyJson() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/Example3-AutoPolicyCalculationOpenAPI.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<DataModel> dataModels = projectModel.getDataModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(14, dataModels.size());
        assertEquals(25, spreadsheetModels.size());
    }

    @Test
    public void testBankRating() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/BankRating.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<DataModel> dataModels = projectModel.getDataModels();
        assertEquals(5, datatypeModels.size());
        assertEquals(4, dataModels.size());
        assertEquals(33, spreadsheetModels.size());
        assertTrue(projectModel.isRuntimeContextProvided());
    }

    @Test
    public void testFolderWithJsonFiles() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/external_links/Driver.json");
        assertNotNull(projectModel.getName());
        assertEquals("Example, Multiple Files", projectModel.getName());

        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(7, datatypeModels.size());

        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetModels.size());

        SpreadsheetModel dsModel = findSpreadsheetByName(spreadsheetModels, "drivers");
        List<InputParameter> dsParameters = dsModel.getParameters();
        assertEquals(1, dsParameters.size());
        InputParameter dsParam = dsParameters.iterator().next();
        assertEquals("someValue", dsParam.getFormattedName());
        TypeInfo dsType = dsParam.getType();
        validateTypeInfo("SomeValue", dsType.getSimpleName(), "SomeValue", dsType.getJavaName());
        assertEquals(TypeInfo.Type.DATATYPE, dsType.getType());
    }

    @Test
    public void testReusableBodyJsonWhichWillBeExpanded() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/request/reusable_request_body_once.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel sprResult = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = sprResult.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        TypeInfo type = param.getType();
        validateTypeInfo("RequestModel", type.getSimpleName(), "RequestModel", type.getJavaName());
        assertEquals(TypeInfo.Type.DATATYPE, type.getType());
        assertNull(param.getIn());
    }

    @Test
    public void testReusableBodyWhichWillBeDataType() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/request/reusable_request_body_twice.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());
        SpreadsheetModel oneMoreTestSpreadsheet = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = oneMoreTestSpreadsheet.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        assertEquals("requestModel", param.getFormattedName());
        TypeInfo type = param.getType();
        assertEquals("RequestModel", type.getSimpleName());
        assertNull(param.getIn());
        assertEquals(TypeInfo.Type.DATATYPE, type.getType());
    }

    @Test
    public void testReusableResponse() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/response/reusable_response.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(2, datatypeModels.size());
        Optional<DatatypeModel> optionalDatatypeModel = datatypeModels.stream()
            .filter(x -> x.getName().equals("MyModel"))
            .findAny();
        assertTrue(optionalDatatypeModel.isPresent());
        DatatypeModel datatypeModel = optionalDatatypeModel.get();
        assertEquals("MyModel", datatypeModel.getName());
        assertEquals(3, datatypeModel.getFields().size());

        Optional<DatatypeModel> optionalRequestModel = datatypeModels.stream()
            .filter(x -> x.getName().equals("RequestModel"))
            .findAny();
        assertTrue(optionalRequestModel.isPresent());
        DatatypeModel requestModel = optionalRequestModel.get();
        assertEquals(1, requestModel.getFields().size());
    }

    @Test
    public void testOneAndAnyOfRequest() throws IOException {
        ProjectModel oneOf = converter.extractProjectModel("test.converter/project/oneOfAndAnyOf/oneOfInRequest.json");
        List<SpreadsheetModel> spreadsheetModels = oneOf.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel testSpreadsheet = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = testSpreadsheet.getParameters();
        assertEquals(1, parameters.size());
        InputParameter inputParam = parameters.iterator().next();
        assertEquals("object", inputParam.getFormattedName());
        TypeInfo inputParamType = inputParam.getType();
        validateTypeInfo("java.lang.Object", inputParamType.getJavaName(), "Object", inputParamType.getSimpleName());
        assertEquals(TypeInfo.Type.OBJECT, inputParamType.getType());
        assertNull(inputParam.getIn());
        PathInfo testSpreadsheetPathInfo = testSpreadsheet.getPathInfo();
        TypeInfo returnType = testSpreadsheetPathInfo.getReturnType();
        validateTypeInfo("java.lang.Double", returnType.getJavaName(), "Double", returnType.getSimpleName());
        assertEquals(TypeInfo.Type.OBJECT, returnType.getType());

        ProjectModel anyOf = converter.extractProjectModel("test.converter/project/oneOfAndAnyOf/anyOfInRequest.json");
        List<SpreadsheetModel> anyOfModels = anyOf.getSpreadsheetResultModels();
        assertEquals(1, anyOfModels.size());
        SpreadsheetModel anyOfSpr = anyOfModels.iterator().next();
        List<InputParameter> anyOfParams = anyOfSpr.getParameters();
        assertEquals(1, anyOfParams.size());
    }

    @Test
    public void testAllOfRequest() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/allOf/allOfInRequest.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());

        Optional<DatatypeModel> body = datatypeModels.stream().filter(x -> x.getName().equals("Body")).findFirst();
        boolean isBodyPresented = body.isPresent();
        assertFalse(isBodyPresented);
        SpreadsheetModel sprModel = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = sprModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter ip = parameters.iterator().next();
        TypeInfo type = ip.getType();
        assertEquals("body", type.getSimpleName());
        assertEquals(TypeInfo.Type.DATATYPE, type.getType());
        assertTrue(datatypeModels.stream().anyMatch(model -> model.getName().equals(type.getSimpleName())));
        assertNull(ip.getIn());
    }

    @Test
    public void testOneOfWithAllOf() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/oneOfWithAllOfInRequest.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel testSpreadsheet = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = testSpreadsheet.getParameters();
        assertEquals(1, parameters.size());
        InputParameter inputParameter = parameters.iterator().next();
        assertEquals("object", inputParameter.getFormattedName());
        assertNull(inputParameter.getIn());
        TypeInfo type = inputParameter.getType();
        validateTypeInfo("java.lang.Object", type.getJavaName(), "Object", type.getSimpleName());
    }

    @Test
    public void testAllOfInResponse() throws IOException {
        // cat is used in expanding
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/allOf/allOfInResponse.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, spreadsheetModels.size());
        assertEquals(3, datatypeModels.size());
        SpreadsheetModel spreadsheetModel = spreadsheetModels.iterator().next();

        PathInfo pathInfo = spreadsheetModel.getPathInfo();
        TypeInfo returnType = pathInfo.getReturnType();
        validateTypeInfo("org.openl.rules.calc.SpreadsheetResult",
            returnType.getJavaName(),
            "inline_response_200",
            returnType.getSimpleName());
        assertEquals(TypeInfo.Type.SPREADSHEET, returnType.getType());

        String spreadsheetType = spreadsheetModel.getType();
        assertEquals(SPREADSHEET_RESULT, spreadsheetType);

        List<InputParameter> parameters = spreadsheetModel.getParameters();
        InputParameter catParam = findInputParameter(parameters, "cat");
        TypeInfo catType = catParam.getType();
        assertEquals(TypeInfo.Type.DATATYPE, catType.getType());
        assertTrue(datatypeModels.stream().anyMatch(dm -> dm.getName().equals(catType.getSimpleName())));

        validateParameter(parameters, "voice", "java.lang.String", "String");
    }

    @Test
    public void testOneOfInResponse() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/oneOfAndAnyOf/oneOfInResponse.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(7, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());
    }

    @Test
    public void testExpandablePropertyInsideNonExpandableScheme() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/expand/expand_test.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());

        SpreadsheetModel testSpreadsheet = findSpreadsheetByName(spreadsheetModels, "test");
        List<InputParameter> testParameters = testSpreadsheet.getParameters();
        assertEquals(3, testParameters.size());

        InputParameter countParam = findInputParameter(testParameters, "count");
        assertEquals("java.math.BigInteger", countParam.getType().getJavaName());

        InputParameter requestParam = findInputParameter(testParameters, "requestTest");
        assertEquals("java.lang.String", requestParam.getType().getJavaName());

        InputParameter catParamInTest = findInputParameter(testParameters, "cat");
        assertEquals("Cat", catParamInTest.getType().getJavaName());

        SpreadsheetModel oneMorePath = findSpreadsheetByName(spreadsheetModels, "oneMorePath");
        List<InputParameter> parameters = oneMorePath.getParameters();
        assertEquals(1, parameters.size());
        InputParameter catParam = parameters.iterator().next();
        TypeInfo catType = catParam.getType();
        assertEquals("Cat", catType.getSimpleName());
        assertEquals(TypeInfo.Type.DATATYPE, catType.getType());
        assertTrue(datatypeModels.stream().anyMatch(dt -> dt.getName().equals(catType.getSimpleName())));
    }

    @Test
    public void testExpandableExceedingLimit() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/expand/expand_exceeds_limit.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());
    }

    @Test
    public void largeFileTest() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/twitter.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(85, datatypeModels.size());
        assertEquals(9, spreadsheetModels.size());
    }

    @Test
    public void testSimpleTypes() throws IOException {
        ProjectModel pm = converter.extractProjectModel("test.converter/problems/simpleTypes.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();

        SpreadsheetModel simpleModel = findSpreadsheetByName(spreadsheetResultModels, "myTestWithIntegerRBAJ");
        assertEquals("SpreadsheetResult", simpleModel.getType());
        List<InputParameter> parameters = simpleModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter integerParam = parameters.iterator().next();
        validateTypeInfo("integer", integerParam.getFormattedName(), "Integer", integerParam.getType().getSimpleName());
        assertEquals("java.lang.Integer", integerParam.getType().getJavaName());

        SpreadsheetModel textPlainWithPrimitiveDoubleParam = findSpreadsheetByName(spreadsheetResultModels,
            "myTestWithDoubleRBTP");
        assertEquals("SpreadsheetResult", textPlainWithPrimitiveDoubleParam.getType());
        List<InputParameter> paramsPrimitive = textPlainWithPrimitiveDoubleParam.getParameters();
        assertEquals(1, paramsPrimitive.size());
        InputParameter doubleParam = paramsPrimitive.iterator().next();
        validateTypeInfo("double", doubleParam.getFormattedName(), "Double", doubleParam.getType().getSimpleName());
        assertEquals("java.lang.Double", doubleParam.getType().getJavaName());

        SpreadsheetModel myTstModel = findSpreadsheetByName(spreadsheetResultModels, "myTst");
        assertEquals("Boolean", myTstModel.getType());
        List<InputParameter> myTstParams = myTstModel.getParameters();
        assertEquals(2, myTstParams.size());

        SpreadsheetModel doubleArrayParamModel = findSpreadsheetByName(spreadsheetResultModels,
            "myTestWithArrayDoubleRBTP");
        validateTypeInfo("Boolean[]",
            doubleArrayParamModel.getType(),
            "[Ljava.lang.Boolean;",
            doubleArrayParamModel.getPathInfo().getReturnType().getJavaName());
        List<InputParameter> doubleArrayParam = doubleArrayParamModel.getParameters();
        InputParameter param = doubleArrayParam.iterator().next();
        validateTypeInfo("Double[]",
            param.getType().getSimpleName(),
            "[Ljava.lang.Double;",
            param.getType().getJavaName());
        assertEquals("double", param.getFormattedName());

        SpreadsheetModel longModel = findSpreadsheetByName(spreadsheetResultModels, "myTestWithLongRBTP");
        assertEquals("Long", longModel.getType());
        List<InputParameter> longParams = longModel.getParameters();
        InputParameter longParam = longParams.iterator().next();
        validateTypeInfo("Long",
            longParam.getType().getSimpleName(),
            "java.lang.Long",
            longParam.getType().getJavaName());
        assertEquals("long", longParam.getFormattedName());

        SpreadsheetModel testWithParams = findSpreadsheetByName(spreadsheetResultModels, "myTestWithParams");
        List<InputParameter> parametersList = testWithParams.getParameters();
        assertEquals(1, parametersList.size());
        InputParameter oneParam = parametersList.iterator().next();
        validateTypeInfo("long", oneParam.getType().getSimpleName(), "simpleId", oneParam.getFormattedName());

        SpreadsheetModel model = findSpreadsheetByName(spreadsheetResultModels, "myTestWithParams2");
        List<InputParameter> withPathParams = model.getParameters();
        assertEquals(2, withPathParams.size());
        Optional<InputParameter> pidId = withPathParams.stream()
            .filter(x -> x.getFormattedName().equals("pidId"))
            .findFirst();
        assertTrue(pidId.isPresent());
        assertEquals("double", pidId.get().getType().getSimpleName());

        InputParameter sumParam = findInputParameter(withPathParams, "sum");
        validateTypeInfo("float", sumParam.getType().getSimpleName(), "float", sumParam.getType().getJavaName());
    }

    @Test
    public void testSettingReturnType() throws IOException {
        ProjectModel pm = converter.extractProjectModel("test.converter/problems/EPBDS-10841-npe.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();

        SpreadsheetModel midStepSome1Model = findSpreadsheetByName(spreadsheetResultModels, "MidStepSome1");
        List<InputParameter> midStepSome1Params = midStepSome1Model.getParameters();
        assertEquals(2, midStepSome1Params.size());
        List<StepModel> midStepSome1ModelSteps = midStepSome1Model.getSteps();
        assertEquals(6, midStepSome1ModelSteps.size());

        SpreadsheetModel middleStepSomeModel = findSpreadsheetByName(spreadsheetResultModels, "MiddleStepSome");
        assertEquals(3, middleStepSomeModel.getParameters().size());
        assertEquals(2, middleStepSomeModel.getSteps().size());

        SpreadsheetModel setStepsSomeModel = findSpreadsheetByName(spreadsheetResultModels, "SetStepSome");
        assertEquals(3, setStepsSomeModel.getParameters().size());
        assertEquals(4, setStepsSomeModel.getSteps().size());

        Optional<SpreadsheetModel> midStepSomeWithTwoParams = spreadsheetResultModels.stream()
            .filter(model -> model.getName().equals("MidStepSome") && model.getParameters().size() == 2)
            .findFirst();
        assertTrue(midStepSomeWithTwoParams.isPresent());
        SpreadsheetModel withoutParams = midStepSomeWithTwoParams.get();
        assertEquals(6, withoutParams.getSteps().size());

    }

    @Test
    public void testFiltering() throws IOException {
        ProjectModel pm = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10838_spreadsheets_filtering.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        SpreadsheetModel mySecondSprModel = findSpreadsheetByName(spreadsheetResultModels, "MySecondSpr");
        assertEquals("SpreadsheetResultMyFirsSpr[]", mySecondSprModel.getType());

        assertFalse(pm.getDatatypeModels().stream().anyMatch(x -> x.getName().equals("MyFirsSpr")));
    }

    @Test
    public void testFilteringIfArrayReturns() throws IOException {
        ProjectModel pm = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10838_spreadsheets_filtering_second_case.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        SpreadsheetModel firsSprModel = findSpreadsheetByName(spreadsheetResultModels, "myFirsSpr");
        assertEquals("SpreadsheetResult", firsSprModel.getType());
        List<StepModel> steps = firsSprModel.getSteps();
        assertEquals(2, steps.size());
        assertTrue(steps.stream().anyMatch(step -> step.getName().equals("Step1")));
        assertTrue(steps.stream().anyMatch(step -> step.getName().equals("Step2")));

        SpreadsheetModel secondModel = findSpreadsheetByName(spreadsheetResultModels, "MySecondSpr");
        assertEquals("SpreadsheetResultmyFirsSpr[]", secondModel.getType());
        List<StepModel> secondModelSteps = secondModel.getSteps();
        assertEquals(1, secondModelSteps.size());
        StepModel secondModelResultStep = secondModelSteps.iterator().next();
        validateTypeInfo("Result", secondModelResultStep.getName(), "MyFirsSpr[]", secondModelResultStep.getType());
        assertEquals("= new SpreadsheetResultmyFirsSpr[]{myFirsSpr(null)}", secondModelResultStep.getValue());
        assertFalse(pm.getDatatypeModels().stream().anyMatch(x -> x.getName().equals("MyFirsSpr")));

    }

    @Test
    public void testGeneratedInputTypes() throws IOException {
        ProjectModel pm = converter.extractProjectModel("test.converter/spreadsheets/EPBDS-10939-parameters.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();

        SpreadsheetModel caseWithPrimitiveParams = findSpreadsheetByName(spreadsheetResultModels, "Case13");
        assertEquals("Integer", caseWithPrimitiveParams.getType());
        PathInfo caseWithPrimitiveParamsPathInfo = caseWithPrimitiveParams.getPathInfo();
        TypeInfo caseWithPrimitiveParamsReturnType = caseWithPrimitiveParamsPathInfo.getReturnType();
        validateTypeInfo("java.lang.Integer",
            caseWithPrimitiveParamsReturnType.getJavaName(),
            "Integer",
            caseWithPrimitiveParamsReturnType.getSimpleName());
        List<InputParameter> primitiveParams = caseWithPrimitiveParams.getParameters();
        validateParameter(primitiveParams, "a", "int", "int");
        validateParameter(primitiveParams, "b", "double", "double");
        validateParameter(primitiveParams, "c", "boolean", "boolean");

        SpreadsheetModel caseWithPrimitiveParamsAndWrappedResult = findSpreadsheetByName(spreadsheetResultModels,
            "Case14");
        assertEquals("BigDecimal", caseWithPrimitiveParamsAndWrappedResult.getType());
        PathInfo wrappedReturnPathInfo = caseWithPrimitiveParamsAndWrappedResult.getPathInfo();
        TypeInfo wrappedReturnType = wrappedReturnPathInfo.getReturnType();
        validateTypeInfo("java.math.BigDecimal",
            wrappedReturnType.getJavaName(),
            "BigDecimal",
            wrappedReturnType.getSimpleName());
        List<InputParameter> primitiveParamsForWrappedReturn = caseWithPrimitiveParamsAndWrappedResult.getParameters();
        validateParameter(primitiveParamsForWrappedReturn, "a", "int", "int");
        validateParameter(primitiveParamsForWrappedReturn, "b", "double", "double");
        validateParameter(primitiveParamsForWrappedReturn, "c", "boolean", "boolean");

        SpreadsheetModel spreadsheetWithExpandedRequest = findSpreadsheetByName(spreadsheetResultModels, "Case23");
        assertEquals("Integer", spreadsheetWithExpandedRequest.getType());
        PathInfo sprWithExpandedRequestPathInfo = spreadsheetWithExpandedRequest.getPathInfo();
        TypeInfo spreadsheetWithExpandedRequestReturnType = sprWithExpandedRequestPathInfo.getReturnType();
        assertEquals("java.lang.Integer", spreadsheetWithExpandedRequestReturnType.getJavaName());
        assertEquals("Integer", spreadsheetWithExpandedRequestReturnType.getSimpleName());
        List<InputParameter> sprWithExpandedRequestParams = spreadsheetWithExpandedRequest.getParameters();
        validateParameter(sprWithExpandedRequestParams, "a", "java.lang.Integer", "Integer");
        validateParameter(sprWithExpandedRequestParams, "b", "java.lang.Double", "Double");
        validateParameter(sprWithExpandedRequestParams, "c", "java.lang.Boolean", "Boolean");
        validateParameter(sprWithExpandedRequestParams, "d", "java.lang.Float", "Float");

        SpreadsheetModel spreadsheetWithExpandedRequestAndWrappedReturnType = findSpreadsheetByName(
            spreadsheetResultModels,
            "Case24");
        assertEquals("BigDecimal", spreadsheetWithExpandedRequestAndWrappedReturnType.getType());
        PathInfo pathInfo = spreadsheetWithExpandedRequestAndWrappedReturnType.getPathInfo();
        TypeInfo returnType = pathInfo.getReturnType();
        assertEquals("java.math.BigDecimal", returnType.getJavaName());
        assertEquals("BigDecimal", returnType.getSimpleName());
        List<InputParameter> parameters = spreadsheetWithExpandedRequestAndWrappedReturnType.getParameters();
        validateParameter(parameters, "a", "java.lang.Integer", "Integer");
        validateParameter(parameters, "b", "java.lang.Double", "Double");
        validateParameter(parameters, "c", "java.lang.Boolean", "Boolean");
        validateParameter(parameters, "d", "java.lang.Float", "Float");

        SpreadsheetModel sprWithDataTypeAndPrimitiveRequest = findSpreadsheetByName(spreadsheetResultModels, "Case51");
        assertEquals("String", sprWithDataTypeAndPrimitiveRequest.getType());
        PathInfo sprWithDataTypeAndPrimitiveRequestPathInfo = sprWithDataTypeAndPrimitiveRequest.getPathInfo();
        TypeInfo infoReturnType = sprWithDataTypeAndPrimitiveRequestPathInfo.getReturnType();
        assertEquals("java.lang.String", infoReturnType.getJavaName());
        assertEquals("String", infoReturnType.getSimpleName());
        List<InputParameter> dataTypeWithPrimitiveParams = sprWithDataTypeAndPrimitiveRequest.getParameters();
        validateParameter(dataTypeWithPrimitiveParams, "a", "java.lang.Integer", "Integer");
        validateParameter(dataTypeWithPrimitiveParams, "param", "MyTestDatatype", "MyTestDatatype");

        SpreadsheetModel sprWithDataTypeAndPrimitiveRequestOrder = findSpreadsheetByName(spreadsheetResultModels,
            "Case52");
        assertEquals("String", sprWithDataTypeAndPrimitiveRequestOrder.getType());
        PathInfo primitiveRequestOrderPathInfo = sprWithDataTypeAndPrimitiveRequestOrder.getPathInfo();
        TypeInfo pathInfoReturnType = primitiveRequestOrderPathInfo.getReturnType();
        assertEquals("java.lang.String", pathInfoReturnType.getJavaName());
        assertEquals("String", pathInfoReturnType.getSimpleName());
        List<InputParameter> requestOrderParameters = sprWithDataTypeAndPrimitiveRequestOrder.getParameters();
        validateParameter(requestOrderParameters, "param", "MyTestDatatype", "MyTestDatatype");
        validateParameter(requestOrderParameters, "a", "java.lang.Integer", "Integer");

        ProjectModel pmWithRuntimeContext = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10939-with-runtime-context.json");
        List<SpreadsheetModel> spreadsheetsWithRuntimeContext = pmWithRuntimeContext.getSpreadsheetResultModels();

        SpreadsheetModel caseWithPrimitivesAndRC = findSpreadsheetByName(spreadsheetsWithRuntimeContext, "Case13");
        assertEquals("Integer", caseWithPrimitivesAndRC.getType());
        PathInfo caseWithPrimitivesAndRCPathInfo = caseWithPrimitivesAndRC.getPathInfo();
        TypeInfo rcPrimitivesReturnType = caseWithPrimitivesAndRCPathInfo.getReturnType();
        validateTypeInfo("java.lang.Integer",
            rcPrimitivesReturnType.getJavaName(),
            "Integer",
            rcPrimitivesReturnType.getSimpleName());
        List<InputParameter> primitiveParametersWithRC = caseWithPrimitivesAndRC.getParameters();
        validateParameter(primitiveParametersWithRC, "a", "java.lang.Integer", "Integer");
        validateParameter(primitiveParametersWithRC, "b", "java.lang.Double", "Double");
        validateParameter(primitiveParametersWithRC, "c", "java.lang.Boolean", "Boolean");

        SpreadsheetModel wrappedWithRC = findSpreadsheetByName(spreadsheetsWithRuntimeContext, "Case14");
        assertEquals("BigDecimal", wrappedWithRC.getType());
        PathInfo wrappedWithRCPathInfo = wrappedWithRC.getPathInfo();
        TypeInfo wrappedRCReturnType = wrappedWithRCPathInfo.getReturnType();
        validateTypeInfo("java.math.BigDecimal",
            wrappedRCReturnType.getJavaName(),
            "BigDecimal",
            wrappedRCReturnType.getSimpleName());
        List<InputParameter> wrappedWithRCParameters = wrappedWithRC.getParameters();
        validateParameter(wrappedWithRCParameters, "a", "java.lang.Integer", "Integer");
        validateParameter(wrappedWithRCParameters, "b", "java.lang.Double", "Double");
        validateParameter(wrappedWithRCParameters, "c", "java.lang.Boolean", "Boolean");

        SpreadsheetModel caseWithExpandedRequestAndRC = findSpreadsheetByName(spreadsheetsWithRuntimeContext, "Case23");
        List<InputParameter> paramsOfExpandedRequestWithRC = caseWithExpandedRequestAndRC.getParameters();
        validateParameter(paramsOfExpandedRequestWithRC, "a", "java.lang.Integer", "Integer");
        validateParameter(paramsOfExpandedRequestWithRC, "b", "java.lang.Double", "Double");
        validateParameter(paramsOfExpandedRequestWithRC, "c", "java.lang.Boolean", "Boolean");
        validateParameter(paramsOfExpandedRequestWithRC, "d", "java.lang.Float", "Float");

        SpreadsheetModel caseWithDTAndPrimitiveWithRC = findSpreadsheetByName(spreadsheetsWithRuntimeContext, "Case51");
        List<InputParameter> rcDatatypeAndPrimitiveParam = caseWithDTAndPrimitiveWithRC.getParameters();
        validateParameter(rcDatatypeAndPrimitiveParam, "a", "java.lang.Integer", "Integer");
        validateParameter(rcDatatypeAndPrimitiveParam, "param", "MyTestDatatype", "MyTestDatatype");
        SpreadsheetModel caseWithDTAndPrimitiveWithRCOrder = findSpreadsheetByName(spreadsheetsWithRuntimeContext,
            "Case52");
        List<InputParameter> rcDatatypeAndPrimitiveParamOrder = caseWithDTAndPrimitiveWithRCOrder.getParameters();
        validateParameter(rcDatatypeAndPrimitiveParamOrder, "a", "java.lang.Integer", "Integer");
        validateParameter(rcDatatypeAndPrimitiveParamOrder, "param", "MyTestDatatype", "MyTestDatatype");

        ProjectModel pmWithDoubleResult = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10939_doubleResult.json");
        List<SpreadsheetModel> spreadsheetModels = pmWithDoubleResult.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel doubleResultSprModel = spreadsheetModels.iterator().next();
        assertEquals("Double", doubleResultSprModel.getType());
    }

    @Test
    public void test_EPBDS_10988() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/EPBDS-10988_OpenAPI.json");
        assertNotNull(projectModel);

        assertTrue(projectModel.isRuntimeContextProvided());

        assertEquals(1, projectModel.getSpreadsheetResultModels().size());
        SpreadsheetModel sprModel = projectModel.getSpreadsheetResultModels().get(0);

        assertEquals(1, sprModel.getParameters().size());
        InputParameter param1 = sprModel.getParameters().get(0);
        assertEquals("param1", param1.getFormattedName());
        assertEquals("java.lang.String", param1.getType().getJavaName());
        assertEquals("String", param1.getType().getSimpleName());
        assertEquals(TypeInfo.Type.OBJECT, param1.getType().getType());

        InputParameter param0 = sprModel.getPathInfo().getRuntimeContextParameter();
        assertNotNull(param0);
        assertEquals("param0", param0.getFormattedName());
        assertEquals("org.openl.rules.context.IRulesRuntimeContext", param0.getType().getJavaName());
        assertEquals("IRulesRuntimeContext", param0.getType().getSimpleName());
        assertEquals(TypeInfo.Type.RUNTIMECONTEXT, param0.getType().getType());
    }

    @Test
    public void test_EPBDS_10993() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/openapi_EPBDS-10993.json");
        assertNotNull(projectModel);
        assertEquals(2, projectModel.getDatatypeModels().size());
        DatatypeModel datatypeModel = projectModel.getDatatypeModels()
            .stream()
            .filter(x -> "MyDatatype".equals(x.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(datatypeModel);
        assertEquals(1, projectModel.getDataModels().size());
        DataModel dataModel = projectModel.getDataModels().get(0);
        assertEquals(datatypeModel, dataModel.getDatatypeModel());
    }

    @Test
    public void test_EPBDS_10999() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/openapi_EPBDS-10999.json");
        assertEquals(4, projectModel.getDatatypeModels().size());
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "DogDatatype".equals(dt.getName())));
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "CatDatatype".equals(dt.getName())));
        assertTrue(
            projectModel.getDatatypeModels().stream().anyMatch(dt -> "AnySpreadsheetResult".equals(dt.getName())));
        assertNotNull(projectModel);
    }

    private void validateParameter(final List<InputParameter> parameters,
            final String paramName,
            final String expectedJavaName,
            final String expectedSimpleName) {
        InputParameter inputParameter = findInputParameter(parameters, paramName);
        TypeInfo aType = inputParameter.getType();
        validateTypeInfo(expectedJavaName, aType.getJavaName(), expectedSimpleName, aType.getSimpleName());
    }

    private SpreadsheetModel findSpreadsheetByName(final List<SpreadsheetModel> spreadsheetResultModels,
            final String name) {
        Optional<SpreadsheetModel> model = spreadsheetResultModels.stream()
            .filter(sprModel -> sprModel.getName().equals(name))
            .findAny();
        assertTrue(model.isPresent());
        return model.get();
    }

    private InputParameter findInputParameter(final List<InputParameter> parameters, final String cat) {
        Optional<InputParameter> catOptional = parameters.stream()
            .filter(x -> x.getFormattedName().equals(cat))
            .findFirst();
        assertTrue(catOptional.isPresent());
        return catOptional.get();
    }

    private void validateTypeInfo(final String expectedClassname,
            final String javaName,
            final String expectedName,
            final String simpleName) {
        assertEquals(expectedClassname, javaName);
        assertEquals(expectedName, simpleName);
    }
}
