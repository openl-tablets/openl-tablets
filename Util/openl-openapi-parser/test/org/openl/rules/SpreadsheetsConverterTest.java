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

    @Test
    public void testBraces() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
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
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_array_instance.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        Optional<SpreadsheetModel> helloKitty = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("HelloKitty"))
            .findFirst();
        assertTrue(helloKitty.isPresent());
        SpreadsheetModel spreadsheetModel = helloKitty.get();
        assertEquals(1, spreadsheetModel.getSteps().size());
        Optional<StepModel> first = spreadsheetModel.getSteps().stream().findFirst();
        assertTrue(first.isPresent());
        StepModel stepModel = first.get();
        assertEquals("Double[]", stepModel.getType());
        assertEquals("HelloKitty", stepModel.getName());
        assertEquals("=new Double[]{}", stepModel.getValue());

        Optional<SpreadsheetModel> secondModel = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("BlaArray"))
            .findFirst();
        assertTrue(secondModel.isPresent());
        SpreadsheetModel blaArrayModel = secondModel.get();
        List<StepModel> blaSteps = blaArrayModel.getSteps();
        assertEquals(1, blaSteps.size());
        assertEquals("=new AnotherDatatype[][][][]{}", blaSteps.iterator().next().getValue());

        Optional<SpreadsheetModel> thirdModel = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("HelloWorld"))
            .findFirst();
        assertTrue(thirdModel.isPresent());
        SpreadsheetModel helloWorldModel = thirdModel.get();
        assertEquals("Double[][][][]", helloWorldModel.getType());
        List<StepModel> steps = helloWorldModel.getSteps();
        assertEquals(1, steps.size());
        StepModel step = steps.iterator().next();
        assertEquals("Double[][][][]", step.getType());
        assertEquals("=new Double[][][][]{}", step.getValue());
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
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_cases_and_symbols.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertFalse(spreadsheetModels.isEmpty());
        Optional<SpreadsheetModel> bla = spreadsheetModels.stream().filter(x -> x.getName().equals("bla")).findFirst();
        assertTrue(bla.isPresent());
        SpreadsheetModel firstModel = bla.get();
        List<StepModel> steps = firstModel.getSteps();
        List<String> stepsNames = steps.stream().map(StepModel::getName).collect(Collectors.toList());
        assertEquals(expectedStepsForBla, stepsNames);

        Optional<SpreadsheetModel> helloKitty = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("$H1ello$Kitty"))
            .findFirst();
        assertTrue(helloKitty.isPresent());
        SpreadsheetModel helloKittyModel = helloKitty.get();
        List<InputParameter> parameters = helloKittyModel.getParameters();
        assertFalse(parameters.isEmpty());
        InputParameter next = parameters.iterator().next();
        assertEquals("Integer", next.getType());
        assertEquals("integer", next.getName());
        assertFalse(helloKittyModel.getSteps().isEmpty());
        StepModel kittyStep = helloKittyModel.getSteps().iterator().next();
        assertEquals("$H1ello$Kitty", kittyStep.getName());

        Optional<SpreadsheetModel> bla112 = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("Bla112"))
            .findFirst();
        assertTrue(bla112.isPresent());
        SpreadsheetModel bla112Model = bla112.get();
        List<InputParameter> bla112Params = bla112Model.getParameters();
        assertFalse(bla112Params.isEmpty());
        assertEquals(2, bla112Params.size());
        Optional<InputParameter> firstParam = bla112Params.stream()
            .filter(x -> x.getName().equals("__32$12HI"))
            .findFirst();
        assertTrue(firstParam.isPresent());

        Optional<InputParameter> secondParam = bla112Params.stream()
            .filter(x -> x.getName().equals("byeparam"))
            .findFirst();
        assertTrue(secondParam.isPresent());
    }

    @Test
    public void testSprDefaultDateTimeValueInSpr() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/default_values_check.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> apiBla = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("apiBla"))
            .findFirst();
        assertTrue(apiBla.isPresent());
        List<StepModel> steps = apiBla.get().getSteps();
        assertEquals(11, steps.size());

        Optional<StepModel> numAccidentsBoolean = steps.stream()
            .filter(x -> x.getName().equals("numAccidents"))
            .findFirst();
        assertTrue(numAccidentsBoolean.isPresent());
        StepModel boolStep = numAccidentsBoolean.get();
        assertEquals("Boolean", boolStep.getType());
        assertEquals("=false", boolStep.getValue());

        Optional<StepModel> numAccidentsDate = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsOne"))
            .findFirst();
        assertTrue(numAccidentsDate.isPresent());
        StepModel dateStep = numAccidentsDate.get();
        assertEquals("Date", dateStep.getType());
        assertEquals("=new Date()", dateStep.getValue());

        Optional<StepModel> numAccidentsTwo = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsTwo"))
            .findFirst();
        assertTrue(numAccidentsTwo.isPresent());
        StepModel dateTimeStep = numAccidentsTwo.get();
        assertEquals("Date", dateTimeStep.getType());
        assertEquals("=new Date()", dateTimeStep.getValue());

        Optional<StepModel> numAccidentsFloat = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsThree"))
            .findFirst();
        assertTrue(numAccidentsFloat.isPresent());
        StepModel floatStep = numAccidentsFloat.get();
        assertEquals("Float", floatStep.getType());
        assertEquals("=0.0f", floatStep.getValue());

        Optional<StepModel> numAccidentsInt = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsFour"))
            .findFirst();
        assertTrue(numAccidentsInt.isPresent());
        StepModel integerStep = numAccidentsInt.get();
        assertEquals("BigInteger", integerStep.getType());
        assertEquals("=java.math.BigInteger.ZERO", integerStep.getValue());

        Optional<StepModel> numAccidentsObject = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsFive"))
            .findFirst();
        assertTrue(numAccidentsObject.isPresent());
        StepModel objectStep = numAccidentsObject.get();
        assertEquals("Object", objectStep.getType());
        assertEquals("=new Object()", objectStep.getValue());

        Optional<StepModel> numAccidentsTyped = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsSix"))
            .findFirst();
        assertTrue(numAccidentsTyped.isPresent());
        StepModel typedStep = numAccidentsTyped.get();
        assertEquals("XItem", typedStep.getType());
        assertEquals("=new XItem()", typedStep.getValue());

        Optional<StepModel> numAccidentsDouble = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsSeven"))
            .findFirst();
        assertTrue(numAccidentsDouble.isPresent());
        StepModel doubleStep = numAccidentsDouble.get();
        assertEquals("Double", doubleStep.getType());
        assertEquals("=0.0", doubleStep.getValue());

        Optional<StepModel> numAccidentsLong = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsEight"))
            .findFirst();
        assertTrue(numAccidentsLong.isPresent());
        StepModel longStep = numAccidentsLong.get();
        assertEquals("Long", longStep.getType());
        assertEquals("=0L", longStep.getValue());

        Optional<StepModel> numAccidentsArray = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsNine"))
            .findFirst();
        assertTrue(numAccidentsArray.isPresent());
        StepModel arrStep = numAccidentsArray.get();
        assertEquals("Boolean[]", arrStep.getType());
        assertEquals("=new Boolean[]{}", arrStep.getValue());

        Optional<StepModel> numAccidentsNArray = steps.stream()
            .filter(x -> x.getName().equals("numAccidentsTen"))
            .findFirst();
        assertTrue(numAccidentsNArray.isPresent());
        StepModel nArrStep = numAccidentsNArray.get();
        assertEquals("Integer[][][][][]", nArrStep.getType());
        assertEquals("=new Integer[][][][][]{}", nArrStep.getValue());

    }

    @Test
    public void inputParamsAreObjects() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EBPDS-10283_object_datatypes_without_fields.yaml");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> mySprOptional = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("mySpr"))
            .findFirst();
        assertTrue(mySprOptional.isPresent());
        SpreadsheetModel mySprModel = mySprOptional.get();
        assertEquals("double", mySprModel.getType());
        List<InputParameter> parameters = mySprModel.getParameters();
        assertEquals(4, parameters.size());

        Optional<InputParameter> objFieldOptional = parameters.stream()
            .filter(x -> x.getName().equals("objField"))
            .findFirst();
        assertTrue(objFieldOptional.isPresent());
        InputParameter objParameter = objFieldOptional.get();
        assertEquals("Object", objParameter.getType());
        assertEquals("objField", objParameter.getName());

        Optional<InputParameter> mapFieldOptional = parameters.stream()
            .filter(x -> x.getName().equals("mapField"))
            .findFirst();
        assertTrue(mapFieldOptional.isPresent());
        InputParameter mapParameter = mapFieldOptional.get();
        assertEquals("Object", mapParameter.getType());
        assertEquals("mapField", mapParameter.getName());

        Optional<InputParameter> listFieldOptional = parameters.stream()
            .filter(x -> x.getName().equals("listField"))
            .findFirst();
        assertTrue(listFieldOptional.isPresent());
        InputParameter listParameter = listFieldOptional.get();
        assertEquals("Object[]", listParameter.getType());
        assertEquals("listField", listParameter.getName());

        Optional<InputParameter> doubleFieldOptional = parameters.stream()
            .filter(x -> x.getName().equals("doubleField"))
            .findFirst();
        assertTrue(doubleFieldOptional.isPresent());
        InputParameter doubleParameter = doubleFieldOptional.get();
        assertEquals("Double", doubleParameter.getType());
        assertEquals("doubleField", doubleParameter.getName());

        Optional<SpreadsheetModel> mySpr2Optional = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("mySpr2"))
            .findFirst();
        assertTrue(mySpr2Optional.isPresent());
        SpreadsheetModel mySpr2Model = mySpr2Optional.get();
        assertEquals("Object", mySpr2Model.getType());
        List<InputParameter> spr2Parameters = mySpr2Model.getParameters();
        assertEquals(1, spr2Parameters.size());
        InputParameter objectParam = spr2Parameters.iterator().next();
        assertEquals("Object", objectParam.getType());
        assertEquals("object", objectParam.getName());

        List<StepModel> objSteps = mySpr2Model.getSteps();
        assertEquals(1, objSteps.size());
        StepModel objStep = objSteps.iterator().next();
        assertEquals("Object", objStep.getType());
        assertEquals("mySpr2", objStep.getName());
    }

    @Test
    public void testMissedSpreadsheet() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EBPDS-10228_spreadsheet_was_missed.json");
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, spreadsheetModels.size());
        Optional<SpreadsheetModel> apiBlaOptional = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("apiBla"))
            .findFirst();
        assertTrue(apiBlaOptional.isPresent());
        SpreadsheetModel apiBlaModel = apiBlaOptional.get();
        List<StepModel> steps = apiBlaModel.getSteps();
        StepModel step = steps.iterator().next();
        assertEquals("Result", step.getName());
        assertEquals("DriverRisk", step.getType());
        assertEquals("=new DriverRisk()", step.getValue());
    }

    @Test
    public void testProvidedContext() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
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
    }

    @Test
    public void testArrayBrackets() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/spreadsheets/arrray_brackets.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> modelOptional = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("HelloKitty"))
            .findFirst();
        assertTrue(modelOptional.isPresent());
        SpreadsheetModel spreadsheetModel = modelOptional.get();
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter param = parameters.iterator().next();
        assertEquals("Double[]", param.getType());
        assertEquals("double", param.getName());
    }

    @Test
    public void testSprResultSignatureForArray() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/spr_return_array_of_type.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> hko = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("HelloKitty"))
            .findFirst();
        assertTrue(hko.isPresent());
        SpreadsheetModel hk = hko.get();
        assertEquals("AnotherDatatype[]", hk.getType());

        Optional<SpreadsheetModel> hpo = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("HelloPesi"))
            .findFirst();
        assertTrue(hpo.isPresent());
        SpreadsheetModel hp = hpo.get();
        assertEquals("AnotherDatatype[][][]", hp.getType());

    }

    @Test
    public void testSprInputDateTimeType() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10392_date_time_input.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> helloKitty = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("helloKitty"))
            .findFirst();
        assertTrue(helloKitty.isPresent());
        SpreadsheetModel model = helloKitty.get();
        assertEquals("Date[]", model.getType());
        List<InputParameter> parameters = model.getParameters();
        assertEquals(4, parameters.size());
        Optional<InputParameter> someField = parameters.stream()
            .filter(x -> x.getName().equals("someField"))
            .findFirst();
        assertTrue(someField.isPresent());
        InputParameter inputParameter = someField.get();
        assertEquals("Date", inputParameter.getType());

        Optional<InputParameter> oneMoreField = parameters.stream()
            .filter(x -> x.getName().equals("oneMoreField"))
            .findFirst();
        assertTrue(oneMoreField.isPresent());
        InputParameter oneMoreFieldParam = oneMoreField.get();
        assertEquals("Date[][][]", oneMoreFieldParam.getType());
    }

    @Test
    public void testMissedDataType() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10386_datatype_was_missed.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(3, datatypeModels.size());
        Optional<DatatypeModel> anotherDatatype = datatypeModels.stream()
            .filter(x -> x.getName().equals("AnotherDatatype"))
            .findFirst();
        assertTrue(anotherDatatype.isPresent());
    }

    @Test
    public void testExtraDataType() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10387_extra_datatype.yaml");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(5, datatypeModels.size());
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
        assertEquals("=FinancialRatingCalculation(null,null)", stepModel.getValue());
    }

    @Test
    public void testArraySprSteps() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
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
        Optional<SpreadsheetModel> helloKitty = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("helloKitty"))
            .findFirst();
        assertTrue(helloKitty.isPresent());
        SpreadsheetModel helloKittyArray = helloKitty.get();
        assertEquals(1, helloKittyArray.getSteps().size());
        StepModel step = helloKittyArray.getSteps().iterator().next();
        assertEquals("=new Pokemon[]{}", step.getValue());

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
        Optional<SpreadsheetModel> arrHello = multiArrayModels.stream()
            .filter(x -> x.getName().equals("helloKitty"))
            .findFirst();
        assertTrue(arrHello.isPresent());
        SpreadsheetModel arrModel = arrHello.get();
        List<StepModel> arrSteps = arrModel.getSteps();
        assertEquals(1, arrSteps.size());
        StepModel arrStep = arrSteps.iterator().next();
        assertEquals("=new Pokemon[][][][][]{}", arrStep.getValue());
    }

    @Test
    public void testSprChild() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10432_child_spr.json");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> spr = spreadsheetResultModels.stream().findAny();
        assertTrue(spr.isPresent());
        SpreadsheetModel spreadsheetModel = spr.get();
        assertEquals("Pet", spreadsheetModel.getType());
        assertEquals(1, spreadsheetModel.getSteps().size());
        StepModel step = spreadsheetModel.getSteps().iterator().next();
        assertEquals("Pet", step.getType());
        assertEquals("Result", step.getName());
        assertEquals("=new Pet()", step.getValue());
    }

    @Test
    public void testTypeGeneration() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10433_type_not_generated.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(2, datatypeModels.size());
        Optional<DatatypeModel> myType = datatypeModels.stream().filter(x -> x.getName().equals("MyType")).findAny();
        assertTrue(myType.isPresent());

        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> helloKitty = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("helloKitty"))
            .findFirst();
        assertTrue(helloKitty.isPresent());
        SpreadsheetModel spreadsheetModel = helloKitty.get();
        assertEquals("MyType", spreadsheetModel.getType());
        List<StepModel> steps = spreadsheetModel.getSteps();
        assertEquals(1, steps.size());
        StepModel step = steps.iterator().next();
        assertEquals("MyType", step.getType());
        assertEquals("Result", step.getName());
        assertEquals("=new MyType()", step.getValue());

        ProjectModel projectModelArray = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10433_type_not_generated_arr.json");
        List<DatatypeModel> dts = projectModelArray.getDatatypeModels();
        assertEquals(2, dts.size());

    }

    @Test
    public void testOverloaded() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
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
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10422_discriminator_field.yaml");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<FieldModel> fields = datatypeModels.stream()
            .flatMap(x -> x.getFields().stream())
            .collect(Collectors.toList());
        Optional<FieldModel> anyClassField = fields.stream().filter(x -> x.getName().equals("@class")).findAny();
        assertFalse(anyClassField.isPresent());

        Optional<SpreadsheetModel> method2 = projectModel.getSpreadsheetResultModels()
            .stream()
            .filter(x -> x.getName().equals("method2"))
            .findFirst();
        assertTrue(method2.isPresent());
        SpreadsheetModel spreadsheetModel = method2.get();
        assertEquals(1, spreadsheetModel.getSteps().size());
        StepModel colorStep = spreadsheetModel.getSteps().iterator().next();
        assertEquals("color", colorStep.getName());
    }

    @Test
    public void testIncorrectCall() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
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
        assertEquals("=new TierRate()", tierRates.get().getValue());
    }

    @Test
    public void testIncorrectSpreadsheetArray() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
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
        Optional<StepModel> petArray = steps.stream().filter(x -> x.getName().equals("PetArray")).findFirst();
        assertTrue(petArray.isPresent());
        StepModel stepModel = petArray.get();
        assertEquals("=new SpreadsheetResultNewPet[]{NewPet(null,null)}", stepModel.getValue());

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
        Optional<StepModel> petNArray = stepModels.stream().filter(x -> x.getName().equals("PetArray")).findFirst();
        assertTrue(petNArray.isPresent());
        StepModel step = petNArray.get();
        assertEquals("=new SpreadsheetResultNewPet[][][][]{{{{NewPet(null,null)}}}}", step.getValue());
    }

    @Test
    public void testIncorrectWrapperCase() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/spreadsheets/EPBDS-10481-wrong_request_body.yaml");
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        assertEquals(1, spreadsheetResultModels.size());
        SpreadsheetModel spreadsheetModel = spreadsheetResultModels.iterator().next();
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(1, parameters.size());
        InputParameter next = parameters.iterator().next();
        assertEquals("WrapperObject", next.getType());
        assertEquals("wrapperObject", next.getName());
    }

    @Test
    public void modifiedPathTest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pathProject = converter.extractProjectModel("test.converter/paths/slashProblem.json");
        List<SpreadsheetModel> spreadsheetResultModels = pathProject.getSpreadsheetResultModels();

        Optional<SpreadsheetModel> apiBlaOptional = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("apiBla"))
            .findFirst();
        assertTrue(apiBlaOptional.isPresent());
        SpreadsheetModel apiBlaModel = apiBlaOptional.get();
        PathInfo apiBlaModelPathInfo = apiBlaModel.getPathInfo();
        assertEquals("/api/Bla", apiBlaModelPathInfo.getOriginalPath());
        assertEquals("apiBla", apiBlaModelPathInfo.getFormattedPath());
        assertEquals("application/json", apiBlaModelPathInfo.getConsumes());
        assertEquals("text/plain", apiBlaModelPathInfo.getProduces());
        assertEquals("POST", apiBlaModelPathInfo.getOperation());
        assertEquals("Object", apiBlaModelPathInfo.getReturnType());

        Optional<SpreadsheetModel> apiTodoOptional = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("apiTodo"))
            .findFirst();
        assertTrue(apiTodoOptional.isPresent());
        SpreadsheetModel apiTodoModel = apiTodoOptional.get();
        PathInfo apiTodoModelPathInfo = apiTodoModel.getPathInfo();
        assertEquals("/api/Todo", apiTodoModelPathInfo.getOriginalPath());
        assertEquals("apiTodo", apiTodoModelPathInfo.getFormattedPath());
        assertEquals("text/csv", apiTodoModelPathInfo.getConsumes());
        assertEquals("text/html", apiTodoModelPathInfo.getProduces());
        assertEquals("POST", apiTodoModelPathInfo.getOperation());
        assertEquals("Integer", apiTodoModelPathInfo.getReturnType());

    }

}
