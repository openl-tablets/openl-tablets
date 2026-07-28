package org.openl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

/**
 * Tests which are related to spreadsheets model generation.
 */
class SpreadsheetsConverterTest {

    private OpenAPIModelConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OpenAPIScaffoldingConverter();
    }

    @Test
    void testBraces() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/path_with_braces/braced_with_text.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        Optional<SpreadsheetModel> bracedSprName = spreadsheetModels.stream().findFirst();
        var name = bracedSprName.get().getName();
        assertEquals("myRulexyz", name);

        var pM = converter.extractProjectModel("test.converter/path_with_braces/braced_simple.json");
        List<SpreadsheetModel> sprModels = pM.getSpreadsheetResultModels();
        assertFalse(sprModels.isEmpty());
        Optional<SpreadsheetModel> model = sprModels.stream().findFirst();
        var formattedName = model.get().getName();
        assertEquals("myRule", formattedName);

        var pathInfo = projectModel.getSpreadsheetResultModels().getFirst().getPathInfo();
        assertNotNull(pathInfo);
        assertEquals("/myRule/{bla}/xyz", pathInfo.getOriginalPath());
        assertEquals("myRulexyz", pathInfo.getFormattedPath());

    }

    @Test
    void testArrayInSpr() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/spr_array_instance.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        var spreadsheetModel = findSpreadsheet(spreadsheetModels, "HelloKitty");
        assertEquals(1, spreadsheetModel.getSteps().size());
        Optional<StepModel> first = spreadsheetModel.getSteps().stream().findFirst();
        assertTrue(first.isPresent());
        var stepModel = first.get();
        validateGeneratedModel("Double[]",
                stepModel.getType(),
                "Result",
                stepModel.getName(),
                "= new Double[]{}",
                stepModel.getValue());

        var blaArrayModel = findSpreadsheet(spreadsheetModels, "BlaArray");
        List<StepModel> blaSteps = blaArrayModel.getSteps();
        assertEquals(1, blaSteps.size());
        assertEquals("= new SpreadsheetResultBla[][][][]{{{{Bla(null)}}}}", blaSteps.getFirst().getValue());

        var helloWorldModel = findSpreadsheet(spreadsheetModels, "HelloWorld");
        assertEquals("Double[][][][]", helloWorldModel.getType());
        List<StepModel> steps = helloWorldModel.getSteps();
        assertEquals(1, steps.size());
        var step = steps.getFirst();
        assertEquals("Double[][][][]", step.getType());
        assertEquals("= new Double[][][][]{}", step.getValue());
    }

    @Test
    void testNamesInSpreadsheets() throws IOException {
        var expectedStepsForBla = Arrays.asList("NumAccidents",
                "FIeLd",
                "f$$ieLD",
                "$afzZF",
                "numAccidentsOne",
                "numAccRidentsTwo",
                "numAccidentsThree");
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/spr_cases_and_symbols.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        var firstModel = findSpreadsheet(spreadsheetModels, "bla");
        List<StepModel> steps = firstModel.getSteps();
        var stepsNames = steps.stream().map(StepModel::getName).collect(Collectors.toList());
        assertEquals(expectedStepsForBla, stepsNames);

        var helloKittyModel = findSpreadsheet(spreadsheetModels, "$H1ello$Kitty");
        List<InputParameter> parameters = helloKittyModel.getParameters();
        assertFalse(parameters.isEmpty());
        var next = parameters.getFirst();
        assertEquals("Integer", next.getType().getSimpleName());
        assertEquals("integer", next.getFormattedName());
        assertFalse(helloKittyModel.getSteps().isEmpty());
        var kittyStep = helloKittyModel.getSteps().getFirst();
        assertEquals("Result", kittyStep.getName());

        var bla112Model = findSpreadsheet(spreadsheetModels, "Bla112");
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
    void testSprDefaultDateTimeValueInSpr() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/default_values_check.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> apiBla = spreadsheetModels.stream()
                .filter(x -> x.getName().equals("apiBla"))
                .findFirst();
        assertTrue(apiBla.isPresent());
        List<StepModel> steps = apiBla.get().getSteps();
        assertEquals(11, steps.size());

        var boolStep = findStep(steps, "numAccidents");
        assertEquals("Boolean", boolStep.getType());
        assertEquals("= false", boolStep.getValue());

        var dateStep = findStep(steps, "numAccidentsOne");
        assertEquals("Date", dateStep.getType());
        assertEquals("= new Date()", dateStep.getValue());

        var dateTimeStep = findStep(steps, "numAccidentsTwo");
        assertEquals("Date", dateTimeStep.getType());
        assertEquals("= new Date()", dateTimeStep.getValue());

        var floatStep = findStep(steps, "numAccidentsThree");
        assertEquals("Float", floatStep.getType());
        assertEquals("= 0.0f", floatStep.getValue());

        var integerStep = findStep(steps, "numAccidentsFour");
        assertEquals("Integer", integerStep.getType());
        assertEquals("= 0", integerStep.getValue());

        var objectStep = findStep(steps, "numAccidentsFive");
        assertEquals("Object", objectStep.getType());
        assertEquals("= new Object()", objectStep.getValue());

        var typedStep = findStep(steps, "numAccidentsSix");
        assertEquals("XItem", typedStep.getType());
        assertEquals("= new XItem()", typedStep.getValue());

        var doubleStep = findStep(steps, "numAccidentsSeven");
        assertEquals("Double", doubleStep.getType());
        assertEquals("= 0.0", doubleStep.getValue());

        var longStep = findStep(steps, "numAccidentsEight");
        assertEquals("Long", longStep.getType());
        assertEquals("= 0L", longStep.getValue());

        var arrStep = findStep(steps, "numAccidentsNine");
        assertEquals("Boolean[]", arrStep.getType());
        assertEquals("= new Boolean[]{}", arrStep.getValue());

        var nArrStep = findStep(steps, "numAccidentsTen");
        assertEquals("Integer[][][][][]", nArrStep.getType());
        assertEquals("= new Integer[][][][][]{}", nArrStep.getValue());

    }

    @Test
    void inputParamsAreObjects() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EBPDS-10283_object_datatypes_without_fields.yaml");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        var mySprModel = findSpreadsheet(spreadsheetModels, "mySpr");
        assertEquals("Double", mySprModel.getType());
        List<InputParameter> parameters = mySprModel.getParameters();
        assertEquals(4, parameters.size());

        var objParameter = findInputParameter(parameters, "objField");
        validateGeneratedModel("Object",
                objParameter.getType().getSimpleName(),
                "java.lang.Object",
                objParameter.getType().getJavaName(),
                "objField",
                objParameter.getFormattedName());

        var mapParameter = findInputParameter(parameters, "mapField");
        validateGeneratedModel("Object",
                mapParameter.getType().getSimpleName(),
                "java.lang.Object",
                mapParameter.getType().getJavaName(),
                "mapField",
                mapParameter.getFormattedName());

        var listParameter = findInputParameter(parameters, "listField");
        validateGeneratedModel("Object[]",
                listParameter.getType().getSimpleName(),
                "[Ljava.lang.Object;",
                listParameter.getType().getJavaName(),
                "listField",
                listParameter.getFormattedName());

        var doubleParameter = findInputParameter(parameters, "doubleField");
        validateGeneratedModel("Double",
                doubleParameter.getType().getSimpleName(),
                "java.lang.Double",
                doubleParameter.getType().getJavaName(),
                "doubleField",
                doubleParameter.getFormattedName());

        var mySpr2Model = findSpreadsheet(spreadsheetModels, "mySpr2");
        assertEquals("Object", mySpr2Model.getType());
        List<InputParameter> spr2Parameters = mySpr2Model.getParameters();
        assertEquals(1, spr2Parameters.size());
        var objectParam = spr2Parameters.getFirst();
        assertEquals("Object", objectParam.getType().getSimpleName());
        assertEquals("object", objectParam.getFormattedName());

        List<StepModel> objSteps = mySpr2Model.getSteps();
        assertEquals(1, objSteps.size());
        var objStep = objSteps.getFirst();
        assertEquals("Object", objStep.getType());
        assertEquals("Result", objStep.getName());
    }

    @Test
    void testMissedSpreadsheet() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EBPDS-10228_spreadsheet_was_missed.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetModels.size());
        var apiBlaModel = findSpreadsheet(spreadsheetModels, "apiBla");
        List<StepModel> steps = apiBlaModel.getSteps();
        var step = steps.getFirst();
        validateGeneratedModel("Result",
                step.getName(),
                "DriverRisk",
                step.getType(),
                "= new DriverRisk()",
                step.getValue());
    }

    @Test
    void testProvidedContext() throws IOException {
        var projectModel = converter.extractProjectModel("test.converter/rd/EPBDS-10306_Runtime_context.json");
        assertFalse(projectModel.isRuntimeContextProvided());
        List<SpreadsheetModel> sprModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, sprModels.size());
        assertEquals(0, projectModel.getNotOpenLModels().size());

        var projectModelWithDRC = converter
                .extractProjectModel("test.converter/rd/EPBDS-10306_Runtime_context_provided.json");
        assertTrue(projectModelWithDRC.isRuntimeContextProvided());
        assertEquals(2, projectModelWithDRC.getSpreadsheetResultModels().size());
        var params = projectModelWithDRC.getSpreadsheetResultModels()
                .stream()
                .flatMap(x -> x.getParameters().stream())
                .collect(Collectors.toList());
        assertEquals(0, params.size());

        var projectModelWithNotAllDRC = converter
                .extractProjectModel("test.converter/rd/EPBDS-10306_Runtime_context_provided_partially.json");
        List<SpreadsheetModel> modelsToClass = projectModelWithNotAllDRC.getNotOpenLModels();
        List<SpreadsheetModel> spreadsheetResultModels = projectModelWithNotAllDRC.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetResultModels.size());
        assertEquals(1, modelsToClass.size());
        var apiBlaModel = modelsToClass.getFirst();
        List<InputParameter> parameters = apiBlaModel.getParameters();
        assertEquals(4, parameters.size());
        var parameterNames = parameters.stream()
                .map(InputParameter::getFormattedName)
                .collect(Collectors.toSet());
        assertTrue(parameterNames.contains("id"));
        assertTrue(parameterNames.contains("name"));
        assertTrue(parameterNames.contains("isCompleted"));
        assertTrue(parameterNames.contains("someStep"));
    }

    @Test
    void testArrayBrackets() throws IOException {
        var projectModel = converter.extractProjectModel("test.converter/spreadsheets/arrray_brackets.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        var spreadsheetModel = findSpreadsheet(spreadsheetResultModels, "HelloKitty");
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(1, parameters.size());
        var param = parameters.getFirst();
        assertEquals("Double[]", param.getType().getSimpleName());
        assertEquals("double", param.getFormattedName());
    }

    @Test
    void testSprResultSignatureForArray() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/spr_return_array_of_type.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        var hk = findSpreadsheet(spreadsheetResultModels, "HelloKitty");
        assertEquals("SpreadsheetResultBla[]", hk.getType());
        List<InputParameter> hkParameters = hk.getParameters();
        assertEquals(1, hkParameters.size());
        var decimalParam = hkParameters.getFirst();
        assertEquals("[Ljava.lang.Double;", decimalParam.getType().getJavaName());
        assertEquals("Double[]", decimalParam.getType().getSimpleName());

        var hp = findSpreadsheet(spreadsheetResultModels, "HelloPesi");
        assertEquals("SpreadsheetResultBla[][][]", hp.getType());

    }

    @Test
    void testSprInputDateTimeType() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10392_date_time_input.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        var model = findSpreadsheet(spreadsheetResultModels, "helloKitty");
        assertEquals("Date[]", model.getType());
        List<InputParameter> parameters = model.getParameters();
        assertEquals(4, parameters.size());
        var inputParameter = findInputParameter(parameters, "someField");
        assertEquals("Date", inputParameter.getType().getSimpleName());
        assertEquals("java.util.Date", inputParameter.getType().getJavaName());

        var oneMoreFieldParam = findInputParameter(parameters, "oneMoreField");
        assertEquals("[[[Ljava.util.Date;", oneMoreFieldParam.getType().getJavaName());
        assertEquals("Date[][][]", oneMoreFieldParam.getType().getSimpleName());
    }

    @Test
    void testMissedDataType() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10386_datatype_was_missed.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(3, datatypeModels.size());
        Optional<DatatypeModel> anotherDatatype = datatypeModels.stream()
                .filter(x -> x.getName().equals("AnotherDatatype"))
                .findFirst();
        assertTrue(anotherDatatype.isPresent());
    }

    @Test
    void testExtraDataType() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10387_extra_datatype.yaml");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(4, datatypeModels.size());
        Optional<SpreadsheetModel> corporateRatingCalculation = projectModel.getSpreadsheetResultModels()
                .stream()
                .filter(x -> x.getName().equals("CorporateRatingCalculation"))
                .findAny();
        assertTrue(corporateRatingCalculation.isPresent());
        var spreadsheetModel = corporateRatingCalculation.get();
        List<StepModel> steps = spreadsheetModel.getSteps();
        Optional<StepModel> financialRatingCalculation = steps.stream()
                .filter(x -> x.getName().equals("Value_FinancialRatingCalculation"))
                .findAny();
        assertTrue(financialRatingCalculation.isPresent());
        var stepModel = financialRatingCalculation.get();
        assertEquals("= FinancialRatingCalculation(null, null)", stepModel.getValue());
    }

    @Test
    void testArraySprSteps() throws IOException {
        var oneDimArray = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10412_array_type_steps.json");
        Optional<DatatypeModel> pokemonOptional = oneDimArray.getDatatypeModels()
                .stream()
                .filter(x -> x.getName().equals("Pokemon"))
                .findFirst();
        assertTrue(pokemonOptional.isPresent());
        var pokemon = pokemonOptional.get();
        assertEquals(4, pokemon.getFields().size());
        List<SpreadsheetModel> spreadsheetResultModels = oneDimArray.getSpreadsheetResultModels();
        var helloKittyArray = findSpreadsheet(spreadsheetResultModels, "helloKitty");
        assertEquals(1, helloKittyArray.getSteps().size());
        var step = helloKittyArray.getSteps().getFirst();
        assertEquals("= new Pokemon[]{}", step.getValue());

        var nThArray = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10412_multi_array_type_steps.json");
        List<SpreadsheetModel> multiArrayModels = nThArray.getSpreadsheetResultModels();
        Optional<DatatypeModel> pokemonArrOptional = nThArray.getDatatypeModels()
                .stream()
                .filter(x -> x.getName().equals("Pokemon"))
                .findFirst();
        assertTrue(pokemonArrOptional.isPresent());
        var pokemonArr = pokemonArrOptional.get();
        assertEquals(4, pokemonArr.getFields().size());
        var arrModel = findSpreadsheet(multiArrayModels, "helloKitty");
        List<StepModel> arrSteps = arrModel.getSteps();
        assertEquals(1, arrSteps.size());
        var arrStep = arrSteps.getFirst();
        assertEquals("= new Pokemon[][][][][]{}", arrStep.getValue());
    }

    @Test
    void testSprChild() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10432_child_spr.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> spr = spreadsheetResultModels.stream()
                .filter(spreadsheet -> spreadsheet.getName().equals("petsAGET"))
                .findAny();
        assertTrue(spr.isPresent());
        var spreadsheetModel = spr.get();
        assertEquals("Pet", spreadsheetModel.getType());
        assertEquals(1, spreadsheetModel.getSteps().size());
        var step = spreadsheetModel.getSteps().getFirst();
        validateGeneratedModel("Pet", step.getType(), "Result", step.getName(), "= new Pet()", step.getValue());
    }

    @Test
    void testTypeGeneration() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10433_type_not_generated.json");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, datatypeModels.size());

        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        var spreadsheetModel = findSpreadsheet(spreadsheetResultModels, "helloKitty");
        assertEquals("SpreadsheetResult", spreadsheetModel.getType());
        List<StepModel> steps = spreadsheetModel.getSteps();
        assertEquals(3, steps.size());

        var projectModelArray = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10433_type_not_generated_arr.json");
        Set<DatatypeModel> dts = projectModelArray.getDatatypeModels();
        assertEquals(1, dts.size());

    }

    @Test
    void testOverloaded() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10439_overloaded_spreadsheet.yaml");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        var storeOrderModels = spreadsheetResultModels.stream()
                .filter(x -> x.getName().contains("storeorder"))
                .collect(Collectors.toSet());
        assertEquals(2, storeOrderModels.size());
    }

    @Test
    void testDiscriminatorField() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10422_discriminator_field.yaml");
        Set<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        var fields = datatypeModels.stream()
                .flatMap(x -> x.getFields().stream())
                .collect(Collectors.toList());
        Optional<FieldModel> anyClassField = fields.stream().filter(x -> x.getName().equals("@class")).findAny();
        assertFalse(anyClassField.isPresent());

        var spreadsheetModel = findSpreadsheet(projectModel.getSpreadsheetResultModels(), "method2");
        assertEquals(1, spreadsheetModel.getSteps().size());
        var colorStep = spreadsheetModel.getSteps().getFirst();
        assertEquals("color", colorStep.getName());
    }

    @Test
    void testIncorrectCall() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10417_table_name_equals_data_type_name.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> calculateCoverageRate = spreadsheetResultModels.stream()
                .filter(x -> x.getName().equals("CalculateCoverageRate"))
                .findAny();
        assertTrue(calculateCoverageRate.isPresent());
        var spreadsheetModel = calculateCoverageRate.get();
        List<StepModel> steps = spreadsheetModel.getSteps();
        Optional<StepModel> tierRates = steps.stream().filter(x -> x.getName().equals("tierRates")).findAny();
        assertTrue(tierRates.isPresent());
        assertEquals("= new TierRate()", tierRates.get().getValue());
    }

    @Test
    void testIncorrectSpreadsheetArray() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10465-incorrect_spreadsheet_array.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> petsA = spreadsheetResultModels.stream()
                .filter(x -> x.getName().equals("petsA"))
                .findAny();
        assertTrue(petsA.isPresent());
        var spreadsheetModel = petsA.get();
        List<StepModel> steps = spreadsheetModel.getSteps();
        assertEquals(2, steps.size());
        var stepModel = findStep(steps, "PetArray");
        assertEquals("= new SpreadsheetResultNewPet[]{NewPet(null, null)}", stepModel.getValue());

        var nThDimensionalArray = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10465-incorrect_spreadsheet_n_array.json");

        Optional<SpreadsheetModel> petsN = nThDimensionalArray.getSpreadsheetResultModels()
                .stream()
                .filter(x -> x.getName().equals("petsN"))
                .findAny();
        assertTrue(petsN.isPresent());
        var model = petsN.get();
        List<StepModel> stepModels = model.getSteps();
        assertEquals(2, stepModels.size());
        var step = findStep(stepModels, "PetArray");
        assertEquals("= new SpreadsheetResultNewPet[][][][]{{{{NewPet(null, null)}}}}", step.getValue());
    }

    @Test
    void testIncorrectWrapperCase() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10481-wrong_request_body.yaml");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetResultModels.size());
        var spreadsheetModel = spreadsheetResultModels.getFirst();
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(1, parameters.size());
        var next = parameters.getFirst();
        assertEquals("WrapperObject", next.getType().getSimpleName());
        assertEquals("wrapperObject", next.getFormattedName());
    }

    @Test
    void modifiedPathTest() throws IOException {
        var pathProject = converter.extractProjectModel("test.converter/paths/slashProblem.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        var apiBlaModel = findSpreadsheet(spreadsheetResultModels, "apiBla");
        var apiBlaModelPathInfo = apiBlaModel.getPathInfo();
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

        var apiTodoModel = findSpreadsheet(spreadsheetResultModels, "apiTodo");
        var apiTodoModelPathInfo = apiTodoModel.getPathInfo();
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
    void testSpreadsheetWithManyParamsCreation() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10969_many_params_with_runtime.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        var mySprModel = findSpreadsheet(spreadsheetModels, "CoverageFactors");
        assertEquals("SpreadsheetResult", mySprModel.getType());
        List<InputParameter> parameters = mySprModel.getParameters();
        assertEquals(10, parameters.size());

        var expectedStepsForBla = Arrays.asList("HospitalConfinementWaiverRate",
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
        var stepsNames = steps.stream().map(StepModel::getName).collect(Collectors.toList());
        assertEquals(expectedStepsForBla, stepsNames);

        var projectModel2 = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10969_many_params.json");
        List<SpreadsheetModel> spreadsheetModels2 = projectModel2.getSpreadsheetResultModels();
        var mySprModel2 = findSpreadsheet(spreadsheetModels2, "CoverageFactors");
        assertEquals("SpreadsheetResult", mySprModel2.getType());
        List<InputParameter> parameters2 = mySprModel2.getParameters();
        assertEquals(10, parameters2.size());
    }

    @Test
    void testSpreadsheetCreation() throws IOException {
        var pathProject = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10799_spreadsheets_creation.json");

        Set<DatatypeModel> datatypeModels = pathProject.getDatatypeModels();
        assertEquals(1, datatypeModels.size());
        var dm = datatypeModels.iterator().next();
        assertEquals("JAXRSErrorResponse", dm.getName());
        assertEquals(3, dm.getFields().size());

        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetResultModels.size());
        var fm = findSpreadsheet(spreadsheetResultModels, "newSpr");
        assertEquals(2, fm.getSteps().size());

        var mySpr = findSpreadsheet(spreadsheetResultModels, "mySpr");
        List<StepModel> steps = mySpr.getSteps();
        assertEquals(3, steps.size());
        var callStep = findStep(steps, "Step3");
        var type = callStep.getType();
        assertEquals("NewSpr[]", type);
        var value = callStep.getValue();
        assertEquals("= new SpreadsheetResultnewSpr[]{newSpr(null, null)}", value);
    }

    @Test
    void testLostSpreadsheet() throws IOException {
        var pathProject = converter
                .extractProjectModel("test.converter/spreadsheets/lostSpreadsheetExamples.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        var newSprModel = findSpreadsheet(spreadsheetResultModels, "newSpr");
        List<StepModel> steps = newSprModel.getSteps();
        assertEquals(2, steps.size());
        assertTrue(steps.stream().anyMatch(x -> x.getName().equals("calc1")));
        assertTrue(steps.stream().anyMatch(x -> x.getName().equals("calc2")));

        var mySprModel = findSpreadsheet(spreadsheetResultModels, "mySpr");
        List<StepModel> mySprSteps = mySprModel.getSteps();
        assertTrue(mySprSteps.stream().anyMatch(step -> step.getName().equals("Step1")));
        assertTrue(mySprSteps.stream().anyMatch(step -> step.getName().equals("Step2")));
        var stepModel = findStep(mySprSteps, "Step3");
        assertEquals("= new SpreadsheetResultnewSpr[]{newSpr(null, null)}", stepModel.getValue());

        var withoutSelfRef = findSpreadsheet(spreadsheetResultModels, "LostSpreadsheetWithoutSelfRefs");
        List<StepModel> withoutSelfRefSteps = withoutSelfRef.getSteps();
        assertTrue(withoutSelfRefSteps.stream().anyMatch(step -> step.getName().equals("abba")));
        var callModel = findStep(withoutSelfRefSteps, "callOfSpr");
        assertEquals("= mySpr(null, null)", callModel.getValue());

        var withSelfRefModel = findSpreadsheet(spreadsheetResultModels,
                "LostSpreadsheetWithSelfReferences");
        List<StepModel> selfRefSteps = withSelfRefModel.getSteps();
        assertTrue(selfRefSteps.stream().anyMatch(step -> step.getName().equals("abba")));

        var interestingStep = findStep(selfRefSteps, "interesting");
        assertEquals("= LostSpreadsheetWithSelfReferences()", interestingStep.getValue());

        Optional<StepModel> optionalInterestingArray = selfRefSteps.stream()
                .filter(step -> step.getName().equals("interestingArray"))
                .findFirst();
        var interestingArrayStep = optionalInterestingArray.get();
        assertEquals("= new SpreadsheetResultLostSpreadsheetWithSelfReferences[][]{{}}",
                interestingArrayStep.getValue());

        var callInArrStep = findStep(selfRefSteps, "callOfSpr");
        assertEquals("= mySpr(null, null)", callInArrStep.getValue());
    }

    @Test
    void testFilteringWithAnySpreadsheetResult() throws IOException {
        var pathProject = converter
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
        var step = mySpr2Steps.getFirst();
        validateGeneratedModel("Step1",
                step.getName(),
                "AnySpreadsheetResult",
                step.getType(),
                "= mySmart(null)",
                step.getValue());

        converter.extractProjectModel("test.converter/spreadsheets/smallExampleWithAnyAsLost.json");
    }

    @Test
    void testSpreadsheetResultInDataTypes() throws IOException {
        var pathProject = converter
                .extractProjectModel("test.converter/datatype/spreadsheetResultDataType.json");
        Set<DatatypeModel> datatypeModels = pathProject.getDatatypeModels();
        assertFalse(datatypeModels.stream().anyMatch(dm -> dm.getName().equals("SpreadsheetResult")));

    }

    /**
     * Case when spreadsheet call another one instead of having the same steps
     */
    @Test
    void testWrongCall() throws IOException {
        var pathProject = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10848_wrong_call.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        var midStepSome1 = findSpreadsheet(spreadsheetResultModels, "MidStepSome1");
        List<StepModel> midStepSome1Steps = midStepSome1.getSteps();
        var ageBandSome1 = findStep(midStepSome1Steps, "AgeBand");
        validateGeneratedModel("String",
                ageBandSome1.getType(),
                "AgeBand",
                ageBandSome1.getName(),
                "= \"\"",
                ageBandSome1.getValue());
        var ageBandInfoSome1 = findStep(midStepSome1Steps, "AgeBandInfo");
        validateGeneratedModel("StepSome[]",
                ageBandInfoSome1.getType(),
                "AgeBandInfo",
                ageBandInfoSome1.getName(),
                "= new StepSome[]{}",
                ageBandInfoSome1.getValue());
        var someFromAllMyPerAgeBandSome1 = findStep(midStepSome1Steps, "SomeFromAllMyPerAgeBand");
        validateGeneratedModel("Double",
                someFromAllMyPerAgeBandSome1.getType(),
                "SomeFromAllMyPerAgeBand",
                someFromAllMyPerAgeBandSome1.getName(),
                "= 0.0",
                someFromAllMyPerAgeBandSome1.getValue());
        var cpFromAllMyPerAgeBandSome1 = findStep(midStepSome1Steps, "CPFromAllMyPerAgeBand");
        validateGeneratedModel("Double",
                cpFromAllMyPerAgeBandSome1.getType(),
                "CPFromAllMyPerAgeBand",
                cpFromAllMyPerAgeBandSome1.getName(),
                "= 0.0",
                someFromAllMyPerAgeBandSome1.getValue());
        var someMultiplyCPSome1 = findStep(midStepSome1Steps, "SomeMultiplyCP");
        validateGeneratedModel("Double",
                someMultiplyCPSome1.getType(),
                "SomeMultiplyCP",
                someMultiplyCPSome1.getName(),
                "= 0.0",
                someMultiplyCPSome1.getValue());
        var blendedSome1 = findStep(midStepSome1Steps, "BlendedSome");
        validateGeneratedModel("MiddleStepSome[]",
                blendedSome1.getType(),
                "BlendedSome",
                blendedSome1.getName(),
                "= new SpreadsheetResultMiddleStepSome[]{MiddleStepSome(null, null, null)}",
                blendedSome1.getValue());

        var middleStepSome = findSpreadsheet(spreadsheetResultModels, "MiddleStepSome");
        assertEquals(2, middleStepSome.getSteps().size());

        var midStepSome = findSpreadsheet(spreadsheetResultModels, "MidStepSome");
        List<StepModel> midStepSomeSteps = midStepSome.getSteps();
        var ageBandSome = findStep(midStepSomeSteps, "AgeBand");
        assertEquals(ageBandSome1, ageBandSome);
        var ageBandInfoSome = findStep(midStepSomeSteps, "AgeBandInfo");
        assertEquals(ageBandInfoSome1, ageBandInfoSome);
        var someFromAllMyPerAgeBandSome = findStep(midStepSomeSteps, "SomeFromAllMyPerAgeBand");
        assertEquals(someFromAllMyPerAgeBandSome1, someFromAllMyPerAgeBandSome);
        var cpFromAllMyPerAgeBandSome = findStep(midStepSomeSteps, "CPFromAllMyPerAgeBand");
        assertEquals(cpFromAllMyPerAgeBandSome1, cpFromAllMyPerAgeBandSome);
        var someMultiplyCPSome = findStep(midStepSomeSteps, "SomeMultiplyCP");
        assertEquals(someMultiplyCPSome1, someMultiplyCPSome);
        var blendedSome = findStep(midStepSomeSteps, "BlendedSome");
        assertEquals(blendedSome1, blendedSome);

        var setStepSome = findSpreadsheet(spreadsheetResultModels, "SetStepSome");
        assertEquals(4, setStepSome.getSteps().size());
        var midStepSomePerAgeBand = findStep(setStepSome.getSteps(), "MidStepSomePerAgeBand");
        validateGeneratedModel("MidStepSome",
                midStepSomePerAgeBand.getType(),
                "MidStepSomePerAgeBand",
                midStepSomePerAgeBand.getName(),
                "= MidStepSome1(null, null)",
                midStepSomePerAgeBand.getValue());

    }

    @Test
    void testEPBDS_10979() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/spreadsheets/EPBDS-10979_sprGeneration.json");
        assertEquals(3, projectModel.getDatatypeModels().size());
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "MyDatatype".equals(dt.getName())));
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "Spr5".equals(dt.getName())));
        assertTrue(projectModel.getDatatypeModels().stream().anyMatch(dt -> "CensusSummary".equals(dt.getName())));
        var sp1 = findSpreadsheet(projectModel.getSpreadsheetResultModels(), "Spr1");
        assertEquals("CensusSummary", sp1.getType());
        assertEquals(1, sp1.getSteps().size());
        assertEquals("CensusSummary", sp1.getSteps().getFirst().getType());
        assertEquals("= new CensusSummary()", sp1.getSteps().getFirst().getValue());

        var sp4 = findSpreadsheet(projectModel.getSpreadsheetResultModels(), "Spr4");
        assertEquals("SpreadsheetResult", sp4.getType());
        assertEquals(2, sp4.getSteps().size());
        assertEquals("CensusSummary[]", sp4.getSteps().getFirst().getType());
        assertEquals("= new CensusSummary[]{}", sp4.getSteps().getFirst().getValue());
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
