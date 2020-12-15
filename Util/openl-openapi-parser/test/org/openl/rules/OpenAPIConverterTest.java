package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

public class OpenAPIConverterTest {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";

    @Test
    public void testAutoPolicyJson() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        String JSON_FILE_NAME = "test.converter/Example3-AutoPolicyCalculationOpenAPI.json";
        ProjectModel projectModel = converter.extractProjectModel(JSON_FILE_NAME);
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<DataModel> dataModels = projectModel.getDataModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(14, dataModels.size());
        assertEquals(25, spreadsheetModels.size());
    }

    @Test
    public void testBankRating() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        String BANK_RATING = "test.converter/BankRating.json";
        ProjectModel projectModel = converter.extractProjectModel(BANK_RATING);
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<DataModel> dataModels = projectModel.getDataModels();
        assertEquals(6, datatypeModels.size());
        assertEquals(4, dataModels.size());
        assertEquals(33, spreadsheetModels.size());
        assertTrue(projectModel.isRuntimeContextProvided());
    }

    @Test
    public void testFolderWithJsonFiles() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        String EXTERNAL_LINKS_JSON_TEST = "test.converter/external_links/Driver.json";
        ProjectModel projectModel = converter.extractProjectModel(EXTERNAL_LINKS_JSON_TEST);
        assertNotNull(projectModel.getName());
        assertEquals("Example, Multiple Files", projectModel.getName());

        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(7, datatypeModels.size());

        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetModels.size());
    }

    @Test
    public void testReusableBodyJsonWhichWillBeExpanded() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/request/reusable_request_body_once.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel sprResult = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = sprResult.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        assertEquals("RequestModel", param.getType());
    }

    @Test
    public void testReusableBodyWhichWillBeDataType() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/request/reusable_request_body_twice.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());
        SpreadsheetModel sprResult = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = sprResult.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        assertEquals("RequestModel", param.getType());
    }

    @Test
    public void testReusableResponse() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/response/reusable_response.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
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
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel oneOf = converter.extractProjectModel("test.converter/project/oneOfAndAnyOf/oneOfInRequest.json");
        List<SpreadsheetModel> spreadsheetModels = oneOf.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel spr = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = spr.getParameters();
        assertEquals(1, parameters.size());

        ProjectModel anyOf = converter.extractProjectModel("test.converter/project/oneOfAndAnyOf/anyOfInRequest.json");
        List<SpreadsheetModel> anyOfModels = anyOf.getSpreadsheetResultModels();
        assertEquals(1, anyOfModels.size());
        SpreadsheetModel anyOfSpr = anyOfModels.iterator().next();
        List<InputParameter> anyOfParams = anyOfSpr.getParameters();
        assertEquals(1, anyOfParams.size());
    }

    @Test
    public void testAllOfRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/allOf/allOfInRequest.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
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
        assertEquals("Body", ip.getType());
    }

    @Test
    public void testOneOfWithAllOf() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/oneOfWithAllOfInRequest.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());
        SpreadsheetModel sprModel = spreadsheetModels.iterator().next();
        List<InputParameter> parameters = sprModel.getParameters();
        assertEquals(1, parameters.size());
    }

    @Test
    public void testAllOfInResponse() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        // cat is used in expanding
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/allOf/allOfInResponse.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, spreadsheetModels.size());
        assertEquals(3, datatypeModels.size());
        SpreadsheetModel spreadsheetModel = spreadsheetModels.iterator().next();
        String type = spreadsheetModel.getType();
        assertEquals(SPREADSHEET_RESULT, type);
    }

    @Test
    public void testOneOfInResponse() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/oneOfAndAnyOf/oneOfInResponse.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(7, datatypeModels.size());
        assertEquals(1, spreadsheetModels.size());
    }

    @Test
    public void testExpandablePropertyInsideNonExpandableScheme() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/expand/expand_test.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());
    }

    @Test
    public void testExpandableExceedingLimit() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/expand/expand_exceeds_limit.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(5, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());
    }

    @Test
    public void largeFileTest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/twitter.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(104, datatypeModels.size());
        assertEquals(9, spreadsheetModels.size());
    }

    @Test
    public void testSimpleTypes() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter.extractProjectModel("test.converter/problems/simpleTypes.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();

        Optional<SpreadsheetModel> requestBodyApplicationJSON = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTestWithIntegerRBAJ"))
            .findFirst();
        assertTrue(requestBodyApplicationJSON.isPresent());
        SpreadsheetModel simpleModel = requestBodyApplicationJSON.get();
        assertEquals("SpreadsheetResult", simpleModel.getType());
        List<InputParameter> parameters = simpleModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter integerParam = parameters.iterator().next();
        assertEquals("integer", integerParam.getName());
        assertEquals("Integer", integerParam.getType());

        Optional<SpreadsheetModel> requestBodyTextPlain = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTestWithDoubleRBTP"))
            .findFirst();
        assertTrue(requestBodyTextPlain.isPresent());
        SpreadsheetModel textPlainWithPrimitiveDoubleParam = requestBodyTextPlain.get();
        assertEquals("SpreadsheetResult", textPlainWithPrimitiveDoubleParam.getType());
        List<InputParameter> paramsPrimitive = textPlainWithPrimitiveDoubleParam.getParameters();
        assertEquals(1, paramsPrimitive.size());
        InputParameter doubleParam = paramsPrimitive.iterator().next();
        assertEquals("double", doubleParam.getName());
        assertEquals("Double", doubleParam.getType());

        Optional<SpreadsheetModel> myTst = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTst"))
            .findFirst();
        assertTrue(myTst.isPresent());
        SpreadsheetModel myTstModel = myTst.get();
        assertEquals("boolean", myTstModel.getType());
        List<InputParameter> myTstParams = myTstModel.getParameters();
        assertEquals(2, myTstParams.size());

        Optional<SpreadsheetModel> myTestWithArrayDouble = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTestWithArrayDoubleRBTP"))
            .findFirst();
        assertTrue(myTestWithArrayDouble.isPresent());
        SpreadsheetModel doubleArrayParamModel = myTestWithArrayDouble.get();
        assertEquals("Boolean[]", doubleArrayParamModel.getType());
        List<InputParameter> doubleArrayParam = doubleArrayParamModel.getParameters();
        InputParameter param = doubleArrayParam.iterator().next();
        assertEquals("Double[]", param.getType());
        assertEquals("double", param.getName());

        Optional<SpreadsheetModel> myTestWithLong = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTestWithLongRBTP"))
            .findFirst();
        assertTrue(myTestWithLong.isPresent());
        SpreadsheetModel longModel = myTestWithLong.get();
        assertEquals("long", longModel.getType());
        List<InputParameter> longParams = longModel.getParameters();
        InputParameter longParam = longParams.iterator().next();
        assertEquals("Long", longParam.getType());
        assertEquals("long", longParam.getName());

        Optional<SpreadsheetModel> myTestWithParams = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTestWithParams"))
            .findFirst();
        assertTrue(myTestWithParams.isPresent());
        SpreadsheetModel testWithParams = myTestWithParams.get();
        List<InputParameter> parametersList = testWithParams.getParameters();
        assertEquals(1, parametersList.size());
        InputParameter oneParam = parametersList.iterator().next();
        assertEquals("long", oneParam.getType());
        assertEquals("simpleId", oneParam.getName());

        Optional<SpreadsheetModel> myTestWithPathParams = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("myTestWithParams2"))
            .findFirst();
        assertTrue(myTestWithPathParams.isPresent());
        SpreadsheetModel model = myTestWithPathParams.get();
        List<InputParameter> withPathParams = model.getParameters();
        assertEquals(2, withPathParams.size());
        Optional<InputParameter> pidId = withPathParams.stream().filter(x -> x.getName().equals("pidId")).findFirst();
        assertTrue(pidId.isPresent());
        assertEquals("double", pidId.get().getType());

        Optional<InputParameter> sum = withPathParams.stream().filter(x -> x.getName().equals("sum")).findFirst();
        assertTrue(sum.isPresent());
        assertEquals("float", sum.get().getType());
    }

    @Test
    public void testSettingReturnType() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter.extractProjectModel("test.converter/problems/EPBDS-10841-npe.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();

        Optional<SpreadsheetModel> midStepSome1 = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("MidStepSome1"))
            .findFirst();
        assertTrue(midStepSome1.isPresent());
        SpreadsheetModel midStepSome1Model = midStepSome1.get();
        List<InputParameter> midStepSome1Params = midStepSome1Model.getParameters();
        assertEquals(2, midStepSome1Params.size());
        List<StepModel> midStepSome1ModelSteps = midStepSome1Model.getSteps();
        assertEquals(1, midStepSome1ModelSteps.size());
        StepModel step = midStepSome1ModelSteps.iterator().next();
        assertEquals("Result", step.getName());
        assertEquals("=MidStepSome(null,null)", step.getValue());

        Optional<SpreadsheetModel> middleStepSome = spreadsheetResultModels.stream()
            .filter(model -> model.getName().equals("MiddleStepSome"))
            .findFirst();
        assertTrue(middleStepSome.isPresent());
        SpreadsheetModel middleStepSomeModel = middleStepSome.get();
        assertEquals(3, middleStepSomeModel.getParameters().size());
        assertEquals(2, middleStepSomeModel.getSteps().size());

        Optional<SpreadsheetModel> setStepsSome = spreadsheetResultModels.stream()
            .filter(model -> model.getName().equals("SetStepSome"))
            .findFirst();
        assertTrue(setStepsSome.isPresent());
        SpreadsheetModel setStepsSomeModel = setStepsSome.get();
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
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10838_spreadsheets_filtering.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> mySecondSpr = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("MySecondSpr"))
            .findFirst();
        assertTrue(mySecondSpr.isPresent());
        SpreadsheetModel mySecondSprModel = mySecondSpr.get();
        assertEquals("SpreadsheetResultMyFirsSpr[]", mySecondSprModel.getType());

        assertFalse(pm.getDatatypeModels().stream().anyMatch(x -> x.getName().equals("MyFirsSpr")));
    }

    @Test
    public void testFilteringIfArrayReturns() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10838_spreadsheets_filtering_second_case.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> myFirsSpr = spreadsheetResultModels.stream()
            .filter(spr -> spr.getName().equals("myFirsSpr"))
            .findFirst();
        assertTrue(myFirsSpr.isPresent());
        SpreadsheetModel firsSprModel = myFirsSpr.get();
        assertEquals("MyFirsSpr", firsSprModel.getType());
        List<StepModel> steps = firsSprModel.getSteps();
        assertEquals(1, steps.size());
        StepModel resultStep = steps.iterator().next();
        assertEquals("Result", resultStep.getName());
        assertEquals("MyFirsSpr", resultStep.getType());
        assertEquals("=new MyFirsSpr()", resultStep.getValue());

        Optional<SpreadsheetModel> mySecondSpr = spreadsheetResultModels.stream()
            .filter(spr -> spr.getName().equals("MySecondSpr"))
            .findFirst();
        assertTrue(mySecondSpr.isPresent());
        SpreadsheetModel secondModel = mySecondSpr.get();
        assertEquals("MyFirsSpr[]", secondModel.getType());
        List<StepModel> secondModelSteps = secondModel.getSteps();
        assertEquals(1, secondModelSteps.size());
        StepModel secondModelResultStep = secondModelSteps.iterator().next();
        assertEquals("Result", secondModelResultStep.getName());
        assertEquals("MyFirsSpr[]", secondModelResultStep.getType());
        assertEquals("=new MyFirsSpr[]{}", secondModelResultStep.getValue());
        assertTrue(pm.getDatatypeModels().stream().anyMatch(x -> x.getName().equals("MyFirsSpr")));

    }

}
