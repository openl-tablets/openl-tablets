package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

public class OpenAPIToExcelConverterTest {

    public static final String SPREADSHEET_RESULT = "SpreadsheetResult";

    @Test
    public void testAutoPolicyJson() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        String JSON_FILE_NAME = "test.converter/Example3-AutoPolicyCalculationOpenAPI.json";
        ProjectModel projectModel = converter.extractProjectModel(JSON_FILE_NAME);
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(3, datatypeModels.size());
        assertEquals(25, spreadsheetResultModels.size());
    }

    @Test
    public void testBankRating() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        String BANK_RATING = "test.converter/BankRating.json";
        ProjectModel projectModel = converter.extractProjectModel(BANK_RATING);
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        assertEquals(35, spreadsheetResultModels.size());
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

        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetResultModels.size());
    }

    @Test
    public void testSimpleDatatype() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/datatype/datatype_simple.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, datatypeModels.size());
    }

    @Test
    public void testDataTypeNesting() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/datatype/datatype_with_parent.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(2, datatypeModels.size());
    }

    @Test
    public void testMultipleDataTypeNesting() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/datatype/datatypes_multiple_nesting.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        Optional<DatatypeModel> crucian = datatypeModels.stream()
            .filter(x -> x.getName().equals("Crucian"))
            .findFirst();
        boolean crucianModelPresented = crucian.isPresent();
        assertTrue(crucianModelPresented);

        DatatypeModel crucianModel = crucian.get();
        assertNotNull(crucianModel);
        assertEquals("Crucian", crucianModel.getName());
        assertEquals("Fish", crucianModel.getParent());
        assertEquals(1, crucianModel.getFields().size());

        Optional<DatatypeModel> fish = datatypeModels.stream().filter(x -> x.getName().equals("Fish")).findFirst();
        boolean fishModelPresented = fish.isPresent();
        assertTrue(fishModelPresented);

        DatatypeModel fishModel = fish.get();
        assertNotNull(fishModel);
        assertEquals("Fish", fishModel.getName());
        assertEquals("Animal", fishModel.getParent());
        assertEquals(1, fishModel.getFields().size());

        Optional<DatatypeModel> animal = datatypeModels.stream().filter(x -> x.getName().equals("Animal")).findFirst();
        boolean animalModelPresented = animal.isPresent();
        assertTrue(animalModelPresented);
        DatatypeModel animalModel = animal.get();
        assertEquals("Animal", animalModel.getName());
        assertNull(animalModel.getParent());
    }

    @Test
    public void testReusableBodyJsonWhichWillBeExpanded() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/request/reusable_request_body_once.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(1, datatypeModels.size());
        assertEquals(1, spreadsheetResultModels.size());
        SpreadsheetResultModel sprResult = spreadsheetResultModels.iterator().next();
        List<InputParameter> parameters = sprResult.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        assertEquals("Long", param.getType());
    }

    @Test
    public void testReusableBodyWhichWillBeDataType() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/reusable/request/reusable_request_body_twice.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, datatypeModels.size());
        assertEquals(2, spreadsheetResultModels.size());
        SpreadsheetResultModel sprResult = spreadsheetResultModels.iterator().next();
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
        assertEquals(1, datatypeModels.size());
        DatatypeModel datatypeModel = datatypeModels.iterator().next();
        assertEquals("MyModel", datatypeModel.getName());
        assertEquals(3, datatypeModel.getFields().size());
    }

    @Test
    public void testOneAndAnyOfRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel oneOf = converter.extractProjectModel("test.converter/project/oneOfAndAnyOf/oneOfInRequest.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = oneOf.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetResultModels.size());
        SpreadsheetResultModel spr = spreadsheetResultModels.iterator().next();
        List<InputParameter> parameters = spr.getParameters();
        assertEquals(0, parameters.size());

        ProjectModel anyOf = converter.extractProjectModel("test.converter/project/oneOfAndAnyOf/anyOfInRequest.json");
        List<SpreadsheetResultModel> anyOfModels = anyOf.getSpreadsheetResultModels();
        assertEquals(1, anyOfModels.size());
        SpreadsheetResultModel anyOfSpr = anyOfModels.iterator().next();
        List<InputParameter> anyOfParams = anyOfSpr.getParameters();
        assertEquals(0, anyOfParams.size());
    }

    @Test
    public void testAllOfRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/allOf/allOfInRequest.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(3, datatypeModels.size());
        assertEquals(1, spreadsheetResultModels.size());
        Optional<DatatypeModel> body = datatypeModels.stream().filter(x -> x.getName().equals("Body")).findFirst();
        boolean isBodyPresented = body.isPresent();
        assertFalse(isBodyPresented);
        SpreadsheetResultModel sprModel = spreadsheetResultModels.iterator().next();
        List<InputParameter> parameters = sprModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter ip = parameters.iterator().next();
        assertEquals("String", ip.getType());
    }

    @Test
    public void testOneOfWithAllOf() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/oneOfWithAllOfInRequest.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        assertEquals(1, spreadsheetResultModels.size());
        SpreadsheetResultModel sprModel = spreadsheetResultModels.iterator().next();
        List<InputParameter> parameters = sprModel.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testAllOfInResponse() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        // cat is used in expanding
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/allOf/allOfInResponse.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, spreadsheetResultModels.size());
        assertEquals(3, datatypeModels.size());
        SpreadsheetResultModel spreadsheetResultModel = spreadsheetResultModels.iterator().next();
        String type = spreadsheetResultModel.getType();
        assertEquals(SPREADSHEET_RESULT, type);
    }

    @Test
    public void testOneOfInResponse() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/oneOfAndAnyOf/oneOfInResponse.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        assertEquals(1, spreadsheetResultModels.size());
    }

    @Test
    public void testExpandablePropertyInsideNonExpandableScheme() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/project/expand/expand_test.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(4, datatypeModels.size());
        assertEquals(2, spreadsheetResultModels.size());
    }

    @Test
    public void testExpandableExceedingLimit() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/project/expand/expand_exceeds_limit.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(5, datatypeModels.size());
        assertEquals(2, spreadsheetResultModels.size());
    }

    @Test
    public void largeFileTest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/twitter.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(104, datatypeModels.size());
        assertEquals(9, spreadsheetResultModels.size());
    }

    @Test
    public void testSprDefaultDateTimeValueInSpr() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/default_values_check.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetResultModel> apiBla = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("apiBla"))
            .findFirst();
        assertTrue(apiBla.isPresent());
        List<StepModel> steps = apiBla.get().getSteps();
        assertEquals(4, steps.size());
        Optional<StepModel> numAccidentsTwo = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsTwo"))
            .findFirst();
        assertTrue(numAccidentsTwo.isPresent());
        assertEquals("OffsetDateTime", numAccidentsTwo.get().getType());
    }

    @Test
    public void testBraces() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/path_with_braces/braced_with_text.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetResultModels.isEmpty());
        Optional<SpreadsheetResultModel> bracedSprName = spreadsheetResultModels.stream().findFirst();
        String name = bracedSprName.get().getName();
        assertEquals("myRulexyz", name);

        ProjectModel pM = converter.extractProjectModel("test.converter/path_with_braces/braced_simple.json");
        List<SpreadsheetResultModel> sprModels = pM.getSpreadsheetResultModels();
        assertFalse(sprModels.isEmpty());
        Optional<SpreadsheetResultModel> model = sprModels.stream().findFirst();
        String formattedName = model.get().getName();
        assertEquals("myRule", formattedName);
    }

    @Test
    public void testNestingProblem() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/nesting.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertFalse(datatypeModels.isEmpty());
        Optional<DatatypeModel> anotherDatatype = datatypeModels.stream()
            .filter(x -> x.getName().equals("AnotherDatatype"))
            .findFirst();
        assertTrue(anotherDatatype.isPresent());
        DatatypeModel datatypeModel = anotherDatatype.get();
        assertEquals("DriverRisk", datatypeModel.getParent());
        List<FieldModel> fields = datatypeModel.getFields();
        assertFalse(fields.isEmpty());
        FieldModel f = fields.stream().findFirst().get();
        assertEquals("category", f.getName());
    }

    @Test
    public void testArrayInSpr() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_array_instance.json");
        List<SpreadsheetResultModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetResultModels.isEmpty());
        Optional<SpreadsheetResultModel> helloKitty = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("HelloKitty"))
            .findFirst();
        assertTrue(helloKitty.isPresent());
        SpreadsheetResultModel spreadsheetResultModel = helloKitty.get();
        assertEquals(1, spreadsheetResultModel.getSteps().size());
        Optional<StepModel> first = spreadsheetResultModel.getSteps().stream().findFirst();
        assertTrue(first.isPresent());
        StepModel stepModel = first.get();
        assertEquals("Double[]", stepModel.getType());
        assertEquals("HelloKitty", stepModel.getName());
    }
}
