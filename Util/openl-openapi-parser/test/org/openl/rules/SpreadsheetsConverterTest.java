package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

/**
 * Tests which are related to spreadsheets model generation.
 */
public class SpreadsheetsConverterTest {

    private OpenAPIModelConverter converter;

    @Before
    public void setUp() {
        converter = new OpenAPIScaffoldingConverter();
    }

    @Test
    public void testBraces() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/path_with_braces/braced_with_text.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        Optional<SpreadsheetModel> bracedSprName = spreadsheetModels.stream().findFirst();
        String name = bracedSprName.get().getName();
        assertEquals("myRulexyz", name);

        ProjectModel pM = converter.extractProjectModel("test.converter/path_with_braces/braced_simple.json");
        List<SpreadsheetModel> sprModels = pM.getSpreadsheetResultModels();
        assertFalse(sprModels.isEmpty());
        Optional<SpreadsheetModel> model = sprModels.stream().findFirst();
        String formattedName = model.get().getName();
        assertEquals("myRule", formattedName);

        PathInfo pathInfo = projectModel.getSpreadsheetResultModels().iterator().next().getPathInfo();
        assertNotNull(pathInfo);
        assertEquals("/myRule/{bla}/xyz", pathInfo.getOriginalPath());
        assertEquals("myRulexyz", pathInfo.getFormattedPath());

    }

    @Test
    public void testArrayInSpr() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_array_instance.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        SpreadsheetModel spreadsheetModel = findSpreadsheet(spreadsheetModels, "HelloKitty");
        assertEquals(1, spreadsheetModel.getSteps().size());
        Optional<StepModel> first = spreadsheetModel.getSteps().stream().findFirst();
        assertTrue(first.isPresent());
        StepModel stepModel = first.get();
        validateGeneratedModel("Double[]",
            stepModel.getType(),
            "Result",
            stepModel.getName(),
            "= new Double[]{}",
            stepModel.getValue());

        SpreadsheetModel blaArrayModel = findSpreadsheet(spreadsheetModels, "BlaArray");
        List<StepModel> blaSteps = blaArrayModel.getSteps();
        assertEquals(1, blaSteps.size());
        assertEquals("= new SpreadsheetResultBla[][][][]{{{{Bla(null)}}}}", blaSteps.iterator().next().getValue());

        SpreadsheetModel helloWorldModel = findSpreadsheet(spreadsheetModels, "HelloWorld");
        assertEquals("Double[][][][]", helloWorldModel.getType());
        List<StepModel> steps = helloWorldModel.getSteps();
        assertEquals(1, steps.size());
        StepModel step = steps.iterator().next();
        assertEquals("Double[][][][]", step.getType());
        assertEquals("= new Double[][][][]{}", step.getValue());
    }

    @Test
    public void testNamesInSpreadsheets() throws IOException {
        List<String> expectedStepsForBla = Arrays.asList("NumAccidents",
            "FIeLd",
            "f$$ieLD",
            "$afzZF",
            "numAccidentsOne",
            "numAccRidentsTwo",
            "numAccidentsThree");
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_cases_and_symbols.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        SpreadsheetModel firstModel = findSpreadsheet(spreadsheetModels, "bla");
        List<StepModel> steps = firstModel.getSteps();
        List<String> stepsNames = steps.stream().map(StepModel::getName).collect(Collectors.toList());
        assertEquals(expectedStepsForBla, stepsNames);

        SpreadsheetModel helloKittyModel = findSpreadsheet(spreadsheetModels, "$H1ello$Kitty");
        List<InputParameter> parameters = helloKittyModel.getParameters();
        assertFalse(parameters.isEmpty());
        InputParameter next = parameters.iterator().next();
        assertEquals("Integer", next.getType().getSimpleName());
        assertEquals("integer", next.getFormattedName());
        assertFalse(helloKittyModel.getSteps().isEmpty());
        StepModel kittyStep = helloKittyModel.getSteps().iterator().next();
        assertEquals("Result", kittyStep.getName());

        SpreadsheetModel bla112Model = findSpreadsheet(spreadsheetModels, "Bla112");
        List<InputParameter> bla112Params = bla112Model.getParameters();
        assertFalse(bla112Params.isEmpty());
        assertEquals(2, bla112Params.size());
        Optional<InputParameter> firstParam = bla112Params.stream()
            .filter(x -> x.getFormattedName().equals("__32$12HI"))
            .findFirst();
        assertTrue(firstParam.isPresent());

        Optional<InputParameter> secondParam = bla112Params.stream()
            .filter(x -> x.getFormattedName().equals("byeBye"))
            .findFirst();
        assertTrue(secondParam.isPresent());
    }

    @Test
    public void testSprDefaultDateTimeValueInSpr() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/default_values_check.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> apiBla = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("apiBla"))
            .findFirst();
        assertTrue(apiBla.isPresent());
        List<StepModel> steps = apiBla.get().getSteps();
        assertEquals(11, steps.size());

        StepModel boolStep = findStep(steps, "numAccidents");
        assertEquals("Boolean", boolStep.getType());
        assertEquals("= false", boolStep.getValue());

        StepModel dateStep = findStep(steps, "numAccidentsOne");
        assertEquals("Date", dateStep.getType());
        assertEquals("= new Date()", dateStep.getValue());

        StepModel dateTimeStep = findStep(steps, "numAccidentsTwo");
        assertEquals("Date", dateTimeStep.getType());
        assertEquals("= new Date()", dateTimeStep.getValue());

        StepModel floatStep = findStep(steps, "numAccidentsThree");
        assertEquals("Float", floatStep.getType());
        assertEquals("= 0.0f", floatStep.getValue());

        StepModel integerStep = findStep(steps, "numAccidentsFour");
        assertEquals("BigInteger", integerStep.getType());
        assertEquals("= java.math.BigInteger.ZERO", integerStep.getValue());

        StepModel objectStep = findStep(steps, "numAccidentsFive");
        assertEquals("Object", objectStep.getType());
        assertEquals("= new Object()", objectStep.getValue());

        StepModel typedStep = findStep(steps, "numAccidentsSix");
        assertEquals("XItem", typedStep.getType());
        assertEquals("= new XItem()", typedStep.getValue());

        StepModel doubleStep = findStep(steps, "numAccidentsSeven");
        assertEquals("Double", doubleStep.getType());
        assertEquals("= 0.0", doubleStep.getValue());

        StepModel longStep = findStep(steps, "numAccidentsEight");
        assertEquals("Long", longStep.getType());
        assertEquals("= 0L", longStep.getValue());

        StepModel arrStep = findStep(steps, "numAccidentsNine");
        assertEquals("Boolean[]", arrStep.getType());
        assertEquals("= new Boolean[]{}", arrStep.getValue());

        StepModel nArrStep = findStep(steps, "numAccidentsTen");
        assertEquals("Integer[][][][][]", nArrStep.getType());
        assertEquals("= new Integer[][][][][]{}", nArrStep.getValue());

    }

    @Test
    public void inputParamsAreObjects() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EBPDS-10283_object_datatypes_without_fields.yaml");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        SpreadsheetModel mySprModel = findSpreadsheet(spreadsheetModels, "mySpr");
        assertEquals("Double", mySprModel.getType());
        List<InputParameter> parameters = mySprModel.getParameters();
        assertEquals(4, parameters.size());

        InputParameter objParameter = findInputParameter(parameters, "objField");
        validateGeneratedModel("Object",
            objParameter.getType().getSimpleName(),
            "java.lang.Object",
            objParameter.getType().getJavaName(),
            "objField",
            objParameter.getFormattedName());

        InputParameter mapParameter = findInputParameter(parameters, "mapField");
        validateGeneratedModel("Object",
            mapParameter.getType().getSimpleName(),
            "java.lang.Object",
            mapParameter.getType().getJavaName(),
            "mapField",
            mapParameter.getFormattedName());

        InputParameter listParameter = findInputParameter(parameters, "listField");
        validateGeneratedModel("Object[]",
            listParameter.getType().getSimpleName(),
            "[Ljava.lang.Object;",
            listParameter.getType().getJavaName(),
            "listField",
            listParameter.getFormattedName());

        InputParameter doubleParameter = findInputParameter(parameters, "doubleField");
        validateGeneratedModel("Double",
            doubleParameter.getType().getSimpleName(),
            "java.lang.Double",
            doubleParameter.getType().getJavaName(),
            "doubleField",
            doubleParameter.getFormattedName());

        SpreadsheetModel mySpr2Model = findSpreadsheet(spreadsheetModels, "mySpr2");
        assertEquals("Object", mySpr2Model.getType());
        List<InputParameter> spr2Parameters = mySpr2Model.getParameters();
        assertEquals(1, spr2Parameters.size());
        InputParameter objectParam = spr2Parameters.iterator().next();
        assertEquals("Object", objectParam.getType().getSimpleName());
        assertEquals("object", objectParam.getFormattedName());

        List<StepModel> objSteps = mySpr2Model.getSteps();
        assertEquals(1, objSteps.size());
        StepModel objStep = objSteps.iterator().next();
        assertEquals("Object", objStep.getType());
        assertEquals("Result", objStep.getName());
    }

    @Test
    public void testMissedSpreadsheet() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EBPDS-10228_spreadsheet_was_missed.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetModels.size());
        SpreadsheetModel apiBlaModel = findSpreadsheet(spreadsheetModels, "apiBla");
        List<StepModel> steps = apiBlaModel.getSteps();
        StepModel step = steps.iterator().next();
        validateGeneratedModel("Result",
            step.getName(),
            "DriverRisk",
            step.getType(),
            "= new DriverRisk()",
            step.getValue());
    }

    @Test
    public void testProvidedContext() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/rd/EPBDS-10306_Runtime_context.json");
        assertFalse(projectModel.isRuntimeContextProvided());
        List<SpreadsheetModel> sprModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, sprModels.size());
        assertEquals(0, projectModel.getNotOpenLModels().size());

        ProjectModel projectModelWithDRC = converter
            .extractProjectModel("test.converter/rd/EPBDS-10306_Runtime_context_provided.json");
        assertTrue(projectModelWithDRC.isRuntimeContextProvided());
        assertEquals(2, projectModelWithDRC.getSpreadsheetResultModels().size());
        List<InputParameter> params = projectModelWithDRC.getSpreadsheetResultModels()
            .stream()
            .flatMap(x -> x.getParameters().stream())
            .collect(Collectors.toList());
        assertEquals(0, params.size());

        ProjectModel projectModelWithNotAllDRC = converter
            .extractProjectModel("test.converter/rd/EPBDS-10306_Runtime_context_provided_partially.json");
        List<SpreadsheetModel> modelsToClass = projectModelWithNotAllDRC.getNotOpenLModels();
        List<SpreadsheetModel> spreadsheetResultModels = projectModelWithNotAllDRC.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetResultModels.size());
        assertEquals(1, modelsToClass.size());
        SpreadsheetModel apiBlaModel = modelsToClass.iterator().next();
        List<InputParameter> parameters = apiBlaModel.getParameters();
        assertEquals(4, parameters.size());
        Set<String> parameterNames = parameters.stream()
            .map(InputParameter::getFormattedName)
            .collect(Collectors.toSet());
        assertTrue(parameterNames.contains("id"));
        assertTrue(parameterNames.contains("name"));
        assertTrue(parameterNames.contains("isCompleted"));
        assertTrue(parameterNames.contains("someStep"));
    }

    @Test
    public void testArrayBrackets() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/spreadsheets/arrray_brackets.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        SpreadsheetModel spreadsheetModel = findSpreadsheet(spreadsheetResultModels, "HelloKitty");
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        assertEquals("Double[]", param.getType().getSimpleName());
        assertEquals("double", param.getFormattedName());
    }

    @Test
    public void testSprResultSignatureForArray() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_return_array_of_type.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        SpreadsheetModel hk = findSpreadsheet(spreadsheetResultModels, "HelloKitty");
        assertEquals("SpreadsheetResultBla[]", hk.getType());
        List<InputParameter> hkParameters = hk.getParameters();
        assertEquals(1, hkParameters.size());
        InputParameter decimalParam = hkParameters.iterator().next();
        assertEquals("[Ljava.math.BigDecimal;", decimalParam.getType().getJavaName());
        assertEquals("BigDecimal[]", decimalParam.getType().getSimpleName());

        SpreadsheetModel hp = findSpreadsheet(spreadsheetResultModels, "HelloPesi");
        assertEquals("SpreadsheetResultBla[][][]", hp.getType());

    }

    @Test
    public void testSprInputDateTimeType() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10392_date_time_input.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        SpreadsheetModel model = findSpreadsheet(spreadsheetResultModels, "helloKitty");
        assertEquals("Date[]", model.getType());
        List<InputParameter> parameters = model.getParameters();
        assertEquals(4, parameters.size());
        InputParameter inputParameter = findInputParameter(parameters, "someField");
        assertEquals("Date", inputParameter.getType().getSimpleName());
        assertEquals("java.util.Date", inputParameter.getType().getJavaName());

        InputParameter oneMoreFieldParam = findInputParameter(parameters, "oneMoreField");
        assertEquals("[[[Ljava.util.Date;", oneMoreFieldParam.getType().getJavaName());
        assertEquals("Date[][][]", oneMoreFieldParam.getType().getSimpleName());
    }

    @Test
    public void testMissedDataType() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10386_datatype_was_missed.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(3, datatypeModels.size());
        Optional<DatatypeModel> anotherDatatype = datatypeModels.stream()
            .filter(x -> x.getName().equals("AnotherDatatype"))
            .findFirst();
        assertTrue(anotherDatatype.isPresent());
    }

    @Test
    public void testExtraDataType() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10387_extra_datatype.yaml");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(4, datatypeModels.size());
        Optional<SpreadsheetModel> corporateRatingCalculation = projectModel.getSpreadsheetResultModels()
            .stream()
            .filter(x -> x.getName().equals("CorporateRatingCalculation"))
            .findAny();
        assertTrue(corporateRatingCalculation.isPresent());
        SpreadsheetModel spreadsheetModel = corporateRatingCalculation.get();
        List<StepModel> steps = spreadsheetModel.getSteps();
        Optional<StepModel> financialRatingCalculation = steps.stream()
            .filter(x -> x.getName().equals("Value_FinancialRatingCalculation"))
            .findAny();
        assertTrue(financialRatingCalculation.isPresent());
        StepModel stepModel = financialRatingCalculation.get();
        assertEquals("= FinancialRatingCalculation(null, null)", stepModel.getValue());
    }

    @Test
    public void testArraySprSteps() throws IOException {
        ProjectModel oneDimArray = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10412_array_type_steps.json");
        Optional<DatatypeModel> pokemonOptional = oneDimArray.getDatatypeModels()
            .stream()
            .filter(x -> x.getName().equals("Pokemon"))
            .findFirst();
        assertTrue(pokemonOptional.isPresent());
        DatatypeModel pokemon = pokemonOptional.get();
        assertEquals(4, pokemon.getFields().size());
        List<SpreadsheetModel> spreadsheetResultModels = oneDimArray.getSpreadsheetResultModels();
        SpreadsheetModel helloKittyArray = findSpreadsheet(spreadsheetResultModels, "helloKitty");
        assertEquals(1, helloKittyArray.getSteps().size());
        StepModel step = helloKittyArray.getSteps().iterator().next();
        assertEquals("= new Pokemon[]{}", step.getValue());

        ProjectModel nThArray = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10412_multi_array_type_steps.json");
        List<SpreadsheetModel> multiArrayModels = nThArray.getSpreadsheetResultModels();
        Optional<DatatypeModel> pokemonArrOptional = nThArray.getDatatypeModels()
            .stream()
            .filter(x -> x.getName().equals("Pokemon"))
            .findFirst();
        assertTrue(pokemonArrOptional.isPresent());
        DatatypeModel pokemonArr = pokemonArrOptional.get();
        assertEquals(4, pokemonArr.getFields().size());
        SpreadsheetModel arrModel = findSpreadsheet(multiArrayModels, "helloKitty");
        List<StepModel> arrSteps = arrModel.getSteps();
        assertEquals(1, arrSteps.size());
        StepModel arrStep = arrSteps.iterator().next();
        assertEquals("= new Pokemon[][][][][]{}", arrStep.getValue());
    }

    @Test
    public void testSprChild() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10432_child_spr.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> spr = spreadsheetResultModels.stream().findAny();
        assertTrue(spr.isPresent());
        SpreadsheetModel spreadsheetModel = spr.get();
        assertEquals("Pet", spreadsheetModel.getType());
        assertEquals(1, spreadsheetModel.getSteps().size());
        StepModel step = spreadsheetModel.getSteps().iterator().next();
        validateGeneratedModel("Pet", step.getType(), "Result", step.getName(), "= new Pet()", step.getValue());
    }

    @Test
    public void testTypeGeneration() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10433_type_not_generated.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, datatypeModels.size());

        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        SpreadsheetModel spreadsheetModel = findSpreadsheet(spreadsheetResultModels, "helloKitty");
        assertEquals("SpreadsheetResult", spreadsheetModel.getType());
        List<StepModel> steps = spreadsheetModel.getSteps();
        assertEquals(3, steps.size());

        ProjectModel projectModelArray = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10433_type_not_generated_arr.json");
        Set<DatatypeModel> dts = projectModelArray.getDatatypeModels();
        assertEquals(1, dts.size());

    }

    @Test
    public void testOverloaded() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10439_overloaded_spreadsheet.yaml");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Set<SpreadsheetModel> storeOrderModels = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("storeorder"))
            .collect(Collectors.toSet());
        assertEquals(2, storeOrderModels.size());
    }

    @Test
    public void testDiscriminatorField() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10422_discriminator_field.yaml");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<FieldModel> fields = datatypeModels.stream()
            .flatMap(x -> x.getFields().stream())
            .collect(Collectors.toList());
        Optional<FieldModel> anyClassField = fields.stream().filter(x -> x.getName().equals("@class")).findAny();
        assertFalse(anyClassField.isPresent());

        SpreadsheetModel spreadsheetModel = findSpreadsheet(projectModel.getSpreadsheetResultModels(), "method2");
        assertEquals(1, spreadsheetModel.getSteps().size());
        StepModel colorStep = spreadsheetModel.getSteps().iterator().next();
        assertEquals("color", colorStep.getName());
    }

    @Test
    public void testIncorrectCall() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10417_table_name_equals_data_type_name.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> calculateCoverageRate = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("CalculateCoverageRate"))
            .findAny();
        assertTrue(calculateCoverageRate.isPresent());
        SpreadsheetModel spreadsheetModel = calculateCoverageRate.get();
        List<StepModel> steps = spreadsheetModel.getSteps();
        Optional<StepModel> tierRates = steps.stream().filter(x -> x.getName().equals("tierRates")).findAny();
        assertTrue(tierRates.isPresent());
        assertEquals("= new TierRate()", tierRates.get().getValue());
    }

    @Test
    public void testIncorrectSpreadsheetArray() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10465-incorrect_spreadsheet_array.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> petsA = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("petsA"))
            .findAny();
        assertTrue(petsA.isPresent());
        SpreadsheetModel spreadsheetModel = petsA.get();
        List<StepModel> steps = spreadsheetModel.getSteps();
        assertEquals(2, steps.size());
        StepModel stepModel = findStep(steps, "PetArray");
        assertEquals("= new SpreadsheetResultNewPet[]{NewPet(null, null)}", stepModel.getValue());

        ProjectModel nThDimensionalArray = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10465-incorrect_spreadsheet_n_array.json");

        Optional<SpreadsheetModel> petsN = nThDimensionalArray.getSpreadsheetResultModels()
            .stream()
            .filter(x -> x.getName().equals("petsN"))
            .findAny();
        assertTrue(petsN.isPresent());
        SpreadsheetModel model = petsN.get();
        List<StepModel> stepModels = model.getSteps();
        assertEquals(2, stepModels.size());
        StepModel step = findStep(stepModels, "PetArray");
        assertEquals("= new SpreadsheetResultNewPet[][][][]{{{{NewPet(null, null)}}}}", step.getValue());
    }

    @Test
    public void testIncorrectWrapperCase() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10481-wrong_request_body.yaml");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetResultModels.size());
        SpreadsheetModel spreadsheetModel = spreadsheetResultModels.iterator().next();
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter next = parameters.iterator().next();
        assertEquals("WrapperObject", next.getType().getSimpleName());
        assertEquals("wrapperObject", next.getFormattedName());
    }

    @Test
    public void modifiedPathTest() throws IOException {
        ProjectModel pathProject = converter.extractProjectModel("test.converter/paths/slashProblem.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        SpreadsheetModel apiBlaModel = findSpreadsheet(spreadsheetResultModels, "apiBla");
        PathInfo apiBlaModelPathInfo = apiBlaModel.getPathInfo();
        validateGeneratedModel("/api/Bla",
            apiBlaModelPathInfo.getOriginalPath(),
            "apiBla",
            apiBlaModelPathInfo.getFormattedPath(),
            "application/json",
            apiBlaModelPathInfo.getConsumes());
        validateGeneratedModel("text/plain",
            apiBlaModelPathInfo.getProduces(),
            PathInfo.Operation.POST,
            apiBlaModelPathInfo.getOperation(),
            "AnotherDatatype",
            apiBlaModelPathInfo.getReturnType().getSimpleName());
        assertEquals("org.openl.rules.calc.SpreadsheetResult", apiBlaModelPathInfo.getReturnType().getJavaName());

        SpreadsheetModel apiTodoModel = findSpreadsheet(spreadsheetResultModels, "apiTodo");
        PathInfo apiTodoModelPathInfo = apiTodoModel.getPathInfo();
        validateGeneratedModel("/api/Todo",
            apiTodoModelPathInfo.getOriginalPath(),
            "apiTodo",
            apiTodoModelPathInfo.getFormattedPath(),
            "text/csv",
            apiTodoModelPathInfo.getConsumes());
        validateGeneratedModel("text/html",
            apiTodoModelPathInfo.getProduces(),
            PathInfo.Operation.POST,
            apiTodoModelPathInfo.getOperation(),
            "Integer",
            apiTodoModelPathInfo.getReturnType().getSimpleName());
        assertEquals("java.lang.Integer", apiTodoModelPathInfo.getReturnType().getJavaName());

    }

    @Test
    public void testSpreadsheetWithManyParamsCreation() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10969_many_params_with_runtime.yaml");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        SpreadsheetModel mySprModel = findSpreadsheet(spreadsheetModels, "CoverageFactors");
        assertEquals("SpreadsheetResult", mySprModel.getType());
        List<InputParameter> parameters = mySprModel.getParameters();
        assertEquals(10, parameters.size());

        List<String> expectedStepsForBla = Arrays.asList("HospitalConfinementWaiverRate",
            "PortabilityFactor",
            "DisabilityDefinitionFactor",
            "FICAMatchingFactor",
            "BenefitPercentFactor",
            "ReturnToWorkFactor",
            "CoverageFactor",
            "ProgressiveIllnessProtection",
            "PreExistingFactor",
            "WorkIncentiveFactor",
            "NetClaimCostAggregatedFactor");
        List<StepModel> steps = mySprModel.getSteps();
        List<String> stepsNames = steps.stream().map(StepModel::getName).collect(Collectors.toList());
        assertEquals(expectedStepsForBla, stepsNames);

        ProjectModel projectModel2 = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10969_many_params.yaml");
        List<SpreadsheetModel> spreadsheetModels2 = projectModel2.getSpreadsheetResultModels();
        SpreadsheetModel mySprModel2 = findSpreadsheet(spreadsheetModels2, "CoverageFactors");
        assertEquals("SpreadsheetResult", mySprModel2.getType());
        List<InputParameter> parameters2 = mySprModel2.getParameters();
        assertEquals(10, parameters2.size());
    }

    @Test
    public void testSpreadsheetCreation() throws IOException {
        ProjectModel pathProject = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10799_spreadsheets_creation.json");

        Set<DatatypeModel> datatypeModels = pathProject.getDatatypeModels();
        assertEquals(1, datatypeModels.size());
        DatatypeModel dm = datatypeModels.iterator().next();
        assertEquals("JAXRSErrorResponse", dm.getName());
        assertEquals(3, dm.getFields().size());

        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetResultModels.size());
        SpreadsheetModel fm = findSpreadsheet(spreadsheetResultModels, "newSpr");
        assertEquals(2, fm.getSteps().size());

        SpreadsheetModel mySpr = findSpreadsheet(spreadsheetResultModels, "mySpr");
        List<StepModel> steps = mySpr.getSteps();
        assertEquals(3, steps.size());
        StepModel callStep = findStep(steps, "Step3");
        String type = callStep.getType();
        assertEquals("NewSpr[]", type);
        String value = callStep.getValue();
        assertEquals("= new SpreadsheetResultnewSpr[]{newSpr(null, null)}", value);
    }

    @Test
    public void testLostSpreadsheet() throws IOException {
        ProjectModel pathProject = converter
            .extractProjectModel("test.converter/spreadsheets/lostSpreadsheetExamples.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        SpreadsheetModel newSprModel = findSpreadsheet(spreadsheetResultModels, "newSpr");
        List<StepModel> steps = newSprModel.getSteps();
        assertEquals(2, steps.size());
        assertTrue(steps.stream().anyMatch(x -> x.getName().equals("calc1")));
        assertTrue(steps.stream().anyMatch(x -> x.getName().equals("calc2")));

        SpreadsheetModel mySprModel = findSpreadsheet(spreadsheetResultModels, "mySpr");
        List<StepModel> mySprSteps = mySprModel.getSteps();
        assertTrue(mySprSteps.stream().anyMatch(step -> step.getName().equals("Step1")));
        assertTrue(mySprSteps.stream().anyMatch(step -> step.getName().equals("Step2")));
        StepModel stepModel = findStep(mySprSteps, "Step3");
        assertEquals("= new SpreadsheetResultnewSpr[]{newSpr(null, null)}", stepModel.getValue());

        SpreadsheetModel withoutSelfRef = findSpreadsheet(spreadsheetResultModels, "LostSpreadsheetWithoutSelfRefs");
        List<StepModel> withoutSelfRefSteps = withoutSelfRef.getSteps();
        assertTrue(withoutSelfRefSteps.stream().anyMatch(step -> step.getName().equals("abba")));
        StepModel callModel = findStep(withoutSelfRefSteps, "callOfSpr");
        assertEquals("= mySpr(null, null)", callModel.getValue());

        SpreadsheetModel withSelfRefModel = findSpreadsheet(spreadsheetResultModels,
            "LostSpreadsheetWithSelfReferences");
        List<StepModel> selfRefSteps = withSelfRefModel.getSteps();
        assertTrue(selfRefSteps.stream().anyMatch(step -> step.getName().equals("abba")));

        StepModel interestingStep = findStep(selfRefSteps, "interesting");
        assertEquals("= LostSpreadsheetWithSelfReferences()", interestingStep.getValue());

        Optional<StepModel> optionalInterestingArray = selfRefSteps.stream()
            .filter(step -> step.getName().equals("interestingArray"))
            .findFirst();
        StepModel interestingArrayStep = optionalInterestingArray.get();
        assertEquals("= new SpreadsheetResultLostSpreadsheetWithSelfReferences[][]{{}}",
            interestingArrayStep.getValue());

        StepModel callInArrStep = findStep(selfRefSteps, "callOfSpr");
        assertEquals("= mySpr(null, null)", callInArrStep.getValue());
    }

    @Test
    public void testFilteringWithAnySpreadsheetResult() throws IOException {
        ProjectModel pathProject = converter
            .extractProjectModel("test.converter/spreadsheets/smallExampleWithAny.json");
        Set<DatatypeModel> datatypeModels = pathProject.getDatatypeModels();

        assertEquals(1, datatypeModels.size());
        assertEquals("JAXRSErrorResponse", datatypeModels.iterator().next().getName());

        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();
        assertEquals(3, spreadsheetResultModels.size());

        List<StepModel> mySpr1Steps = findSpreadsheet(spreadsheetResultModels, "mySpr1").getSteps();
        assertEquals(3, mySpr1Steps.size());
        assertTrue(mySpr1Steps.stream().anyMatch(step -> step.getName().equals("Step1")));
        assertTrue(mySpr1Steps.stream().anyMatch(step -> step.getName().equals("Step2")));
        assertTrue(mySpr1Steps.stream().anyMatch(step -> step.getName().equals("Step3")));

        List<StepModel> mySmartSteps = findSpreadsheet(spreadsheetResultModels, "mySmart").getSteps();
        assertTrue(mySmartSteps.stream().anyMatch(step -> step.getName().equals("Step1")));
        assertTrue(mySmartSteps.stream().anyMatch(step -> step.getName().equals("Step2")));
        assertTrue(mySmartSteps.stream().anyMatch(step -> step.getName().equals("Step3")));

        List<StepModel> mySpr2Steps = findSpreadsheet(spreadsheetResultModels, "mySpr2").getSteps();
        StepModel step = mySpr2Steps.iterator().next();
        validateGeneratedModel("Step1",
            step.getName(),
            "AnySpreadsheetResult",
            step.getType(),
            "= mySmart(null)",
            step.getValue());

        ProjectModel pathProjectWithLostAny = converter
            .extractProjectModel("test.converter/spreadsheets/smallExampleWithAnyAsLost.json");
    }

    @Test
    public void testSpreadsheetResultInDataTypes() throws IOException {
        ProjectModel pathProject = converter
            .extractProjectModel("test.converter/datatype/spreadsheetResultDataType.json");
        Set<DatatypeModel> datatypeModels = pathProject.getDatatypeModels();
        assertFalse(datatypeModels.stream().anyMatch(dm -> dm.getName().equals("SpreadsheetResult")));

    }

    /**
     * Case when spreadsheet call another one instead of having the same steps
     */
    @Test
    public void testWrongCall() throws IOException {
        ProjectModel pathProject = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10848_wrong_call.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        SpreadsheetModel midStepSome1 = findSpreadsheet(spreadsheetResultModels, "MidStepSome1");
        List<StepModel> midStepSome1Steps = midStepSome1.getSteps();
        StepModel ageBandSome1 = findStep(midStepSome1Steps, "AgeBand");
        validateGeneratedModel("String",
            ageBandSome1.getType(),
            "AgeBand",
            ageBandSome1.getName(),
            "= \"\"",
            ageBandSome1.getValue());
        StepModel ageBandInfoSome1 = findStep(midStepSome1Steps, "AgeBandInfo");
        validateGeneratedModel("StepSome[]",
            ageBandInfoSome1.getType(),
            "AgeBandInfo",
            ageBandInfoSome1.getName(),
            "= new StepSome[]{}",
            ageBandInfoSome1.getValue());
        StepModel someFromAllMyPerAgeBandSome1 = findStep(midStepSome1Steps, "SomeFromAllMyPerAgeBand");
        validateGeneratedModel("Double",
            someFromAllMyPerAgeBandSome1.getType(),
            "SomeFromAllMyPerAgeBand",
            someFromAllMyPerAgeBandSome1.getName(),
            "= 0.0",
            someFromAllMyPerAgeBandSome1.getValue());
        StepModel cpFromAllMyPerAgeBandSome1 = findStep(midStepSome1Steps, "CPFromAllMyPerAgeBand");
        validateGeneratedModel("Double",
            cpFromAllMyPerAgeBandSome1.getType(),
            "CPFromAllMyPerAgeBand",
            cpFromAllMyPerAgeBandSome1.getName(),
            "= 0.0",
            someFromAllMyPerAgeBandSome1.getValue());
        StepModel someMultiplyCPSome1 = findStep(midStepSome1Steps, "SomeMultiplyCP");
        validateGeneratedModel("Double",
            someMultiplyCPSome1.getType(),
            "SomeMultiplyCP",
            someMultiplyCPSome1.getName(),
            "= 0.0",
            someMultiplyCPSome1.getValue());
        StepModel blendedSome1 = findStep(midStepSome1Steps, "BlendedSome");
        validateGeneratedModel("MiddleStepSome[]",
            blendedSome1.getType(),
            "BlendedSome",
            blendedSome1.getName(),
            "= new SpreadsheetResultMiddleStepSome[]{MiddleStepSome(null, null, null)}",
            blendedSome1.getValue());

        SpreadsheetModel middleStepSome = findSpreadsheet(spreadsheetResultModels, "MiddleStepSome");
        assertEquals(2, middleStepSome.getSteps().size());

        SpreadsheetModel midStepSome = findSpreadsheet(spreadsheetResultModels, "MidStepSome");
        List<StepModel> midStepSomeSteps = midStepSome.getSteps();
        StepModel ageBandSome = findStep(midStepSomeSteps, "AgeBand");
        assertEquals(ageBandSome1, ageBandSome);
        StepModel ageBandInfoSome = findStep(midStepSomeSteps, "AgeBandInfo");
        assertEquals(ageBandInfoSome1, ageBandInfoSome);
        StepModel someFromAllMyPerAgeBandSome = findStep(midStepSomeSteps, "SomeFromAllMyPerAgeBand");
        assertEquals(someFromAllMyPerAgeBandSome1, someFromAllMyPerAgeBandSome);
        StepModel cpFromAllMyPerAgeBandSome = findStep(midStepSomeSteps, "CPFromAllMyPerAgeBand");
        assertEquals(cpFromAllMyPerAgeBandSome1, cpFromAllMyPerAgeBandSome);
        StepModel someMultiplyCPSome = findStep(midStepSomeSteps, "SomeMultiplyCP");
        assertEquals(someMultiplyCPSome1, someMultiplyCPSome);
        StepModel blendedSome = findStep(midStepSomeSteps, "BlendedSome");
        assertEquals(blendedSome1, blendedSome);

        SpreadsheetModel setStepSome = findSpreadsheet(spreadsheetResultModels, "SetStepSome");
        assertEquals(4, setStepSome.getSteps().size());
        StepModel midStepSomePerAgeBand = findStep(setStepSome.getSteps(), "MidStepSomePerAgeBand");
        validateGeneratedModel("MidStepSome",
            midStepSomePerAgeBand.getType(),
            "MidStepSomePerAgeBand",
            midStepSomePerAgeBand.getName(),
            "= MidStepSome1(null, null)",
            midStepSomePerAgeBand.getValue());

    }

    @Test
    public void testEPBDS_10979() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10979_sprGeneration.json");
        assertEquals(3, projectModel.getDatatypeModels().size());
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "MyDatatype".equals(dt.getName())));
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "Spr5".equals(dt.getName())));
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "CensusSummary".equals(dt.getName())));
        SpreadsheetModel sp1 = findSpreadsheet(projectModel.getSpreadsheetResultModels(), "Spr1");
        assertEquals("CensusSummary", sp1.getType());
        assertEquals(1, sp1.getSteps().size());
        assertEquals("CensusSummary", sp1.getSteps().get(0).getType());
        assertEquals("= new CensusSummary()", sp1.getSteps().get(0).getValue());

        SpreadsheetModel sp4 = findSpreadsheet(projectModel.getSpreadsheetResultModels(), "Spr4");
        assertEquals("SpreadsheetResult", sp4.getType());
        assertEquals(2, sp4.getSteps().size());
        assertEquals("CensusSummary[]", sp4.getSteps().get(0).getType());
        assertEquals("= new CensusSummary[]{}", sp4.getSteps().get(0).getValue());
    }

    @Test
    public void testVariations() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/tutorial7.json");
        assertTrue(projectModel.areVariationsProvided());
    }

    private SpreadsheetModel findSpreadsheet(final List<SpreadsheetModel> spreadsheetModels, final String sprName) {
        Optional<SpreadsheetModel> spreadsheet = spreadsheetModels.stream()
            .filter(x -> x.getName().equals(sprName))
            .findFirst();
        assertTrue(spreadsheet.isPresent());
        return spreadsheet.get();
    }

    private StepModel findStep(final List<StepModel> steps, final String stepName) {
        Optional<StepModel> step = steps.stream().filter(x -> x.getName().equals(stepName)).findFirst();
        assertTrue(step.isPresent());
        return step.get();
    }

    private InputParameter findInputParameter(final List<InputParameter> parameters, final String paramName) {
        Optional<InputParameter> param = parameters.stream()
            .filter(x -> x.getFormattedName().equals(paramName))
            .findFirst();
        assertTrue(param.isPresent());
        return param.get();
    }

    private <T, R, G> void validateGeneratedModel(final T expectedType,
            final T type,
            final R expectedName,
            final R name,
            final G expectedValue,
            final G value) {
        assertEquals(expectedType, type);
        assertEquals(expectedName, name);
        assertEquals(expectedValue, value);
    }
}
