package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;

/**
 * Tests which are related to data type models generation.
 */
public class DataTypeConverterTest {

    private OpenAPIModelConverter converter;

    @Before
    public void setUp() {
        converter = new OpenAPIScaffoldingConverter();
    }

    @Test
    public void testMissedDataTypes() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/datatype/EPBDS-10229_missed_types.json");
        assertEquals(3, projectModel.getDatatypeModels().size());
        Optional<DatatypeModel> driverRisk = projectModel.getDatatypeModels()
            .stream()
            .filter(x -> x.getName().equals("DriverRisk"))
            .findFirst();
        assertTrue(driverRisk.isPresent());

        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> apiBla = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("apiBla"))
            .findFirst();
        assertTrue(apiBla.isPresent());
        SpreadsheetModel model = apiBla.get();
        List<StepModel> steps = model.getSteps();
        assertEquals(1, steps.size());
        StepModel resultStep = steps.iterator().next();
        assertEquals("Result", resultStep.getName());
        assertEquals("=new DriverRisk()", resultStep.getValue());
    }

    @Test
    public void testNestingProblem() throws IOException {
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
    public void testSimpleDatatype() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/datatype/datatype_simple.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(1, datatypeModels.size());
    }

    @Test
    public void testDataTypeNesting() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/datatype/datatype_with_parent.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(2, datatypeModels.size());
        List<FieldModel> fields = datatypeModels.stream()
            .flatMap(x -> x.getFields().stream())
            .collect(Collectors.toList());
        assertFalse(fields.isEmpty());
        assertEquals(4, fields.size());
        Optional<DatatypeModel> animal = datatypeModels.stream().filter(x -> x.getName().equals("Animal")).findFirst();
        assertTrue(animal.isPresent());
        DatatypeModel datatypeModel = animal.get();
        assertEquals(2, datatypeModel.getFields().size());
    }

    @Test
    public void testMultipleDataTypeNesting() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/datatype/datatypes_multiple_nesting.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertEquals(6, datatypeModels.size());
        List<FieldModel> fieldModels = datatypeModels.stream()
            .flatMap(x -> x.getFields().stream())
            .collect(Collectors.toList());
        assertFalse(fieldModels.isEmpty());
        assertEquals(8, fieldModels.size());
        Optional<FieldModel> birthDate = fieldModels.stream().filter(x -> x.getName().equals("birthDate")).findFirst();
        assertTrue(birthDate.isPresent());
        assertEquals("Date", birthDate.get().getType());
        Optional<FieldModel> birthDateTime = fieldModels.stream()
            .filter(x -> x.getName().equals("birthTime"))
            .findFirst();
        assertTrue(birthDateTime.isPresent());
        FieldModel birthTimeField = birthDateTime.get();
        assertEquals("Date", birthTimeField.getType());
        assertTrue(birthTimeField.getDefaultValue() instanceof OffsetDateTime);
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
    public void dataTypeInExpandableRequestBody() throws IOException {
        // project model with expandable request
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/datatype/EPBDS-10285_datatype_in_request_body.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(2, datatypeModels.size());
        assertEquals(2, spreadsheetModels.size());
        Optional<SpreadsheetModel> helloKittyOptional = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("HelloKitty"))
            .findFirst();
        assertTrue(helloKittyOptional.isPresent());
        SpreadsheetModel spreadsheetModel = helloKittyOptional.get();
        List<InputParameter> parameters = spreadsheetModel.getParameters();
        assertEquals(4, parameters.size());

        Optional<InputParameter> numDui = parameters.stream().filter(x -> x.getName().equals("numDUI")).findFirst();
        assertTrue(numDui.isPresent());

        Optional<InputParameter> numAccidents = parameters.stream()
            .filter(x -> x.getName().equals("numAccidents"))
            .findFirst();
        assertTrue(numAccidents.isPresent());

        Optional<InputParameter> category = parameters.stream().filter(x -> x.getName().equals("category")).findFirst();
        assertTrue(category.isPresent());

        Optional<InputParameter> numMovingViolations = parameters.stream()
            .filter(x -> x.getName().equals("numMovingViolations"))
            .findFirst();
        assertTrue(numMovingViolations.isPresent());

        // project model with expandable request, but one more datatype has a link to this datatype
        ProjectModel pm = converter
            .extractProjectModel("test.converter/datatype/EPBDS-10285_datatype_in_request_body_in_field.json");
        assertEquals(4, pm.getDatatypeModels().size());
    }

    @Test
    public void dataTypeWithMoreThanLimitFields() throws IOException {
        ProjectModel projectModel = converter.extractProjectModel(
            "test.converter/datatype/EPBDS-10285_datatype_with_exceeding_limit_fields_number.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        List<SpreadsheetModel> spreadsheetModels = projectModel.getSpreadsheetResultModels();
        assertEquals(3, datatypeModels.size());
        Optional<SpreadsheetModel> apiTodo = spreadsheetModels.stream()
            .filter(x -> x.getName().equals("apiTodo"))
            .findFirst();
        assertTrue(apiTodo.isPresent());
        InputParameter inputParameter = apiTodo.get().getParameters().iterator().next();
        assertEquals("AnotherDatatype", inputParameter.getType().getSimpleName());
        assertEquals("anotherDatatype", inputParameter.getName());
    }

    @Test
    public void dataTypeNumberValuesTest() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/datatype/EPBDS-10415-types_values.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        Optional<DatatypeModel> dynamo = datatypeModels.stream().filter(x -> x.getName().equals("Dynamo")).findFirst();
        assertTrue(dynamo.isPresent());
        DatatypeModel datatypeModel = dynamo.get();
        List<FieldModel> fields = datatypeModel.getFields();
        Optional<FieldModel> a = fields.stream().filter(x -> x.getName().equals("A")).findFirst();
        assertTrue(a.isPresent());
        FieldModel aField = a.get();
        assertEquals("A", aField.getName());
        assertEquals("BigDecimal", aField.getType());
        assertEquals("0", aField.getDefaultValue());
        Optional<FieldModel> b = fields.stream().filter(x -> x.getName().equals("B")).findFirst();
        assertTrue(b.isPresent());
        FieldModel bField = b.get();
        assertEquals("B", bField.getName());
        assertEquals("BigInteger", bField.getType());
        assertEquals(0, bField.getDefaultValue());
        Optional<FieldModel> c = fields.stream().filter(x -> x.getName().equals("C")).findFirst();
        assertTrue(c.isPresent());
        FieldModel cField = c.get();
        assertEquals("C", cField.getName());
        assertEquals("BigInteger", cField.getType());
        assertEquals(BigInteger.ZERO, cField.getDefaultValue());
        Optional<FieldModel> d = fields.stream().filter(x -> x.getName().equals("D")).findFirst();
        assertTrue(d.isPresent());
        FieldModel dField = d.get();
        assertEquals("D", dField.getName());
        assertEquals("BigDecimal", dField.getType());
        assertEquals("2975671681509007947508815", dField.getDefaultValue());
        Optional<FieldModel> e = fields.stream().filter(x -> x.getName().equals("E")).findFirst();
        assertTrue(e.isPresent());
        FieldModel eField = e.get();
        assertEquals("E", eField.getName());
        assertEquals("BigInteger", eField.getType());
        assertEquals(2147483647, eField.getDefaultValue());
        Optional<FieldModel> f = fields.stream().filter(x -> x.getName().equals("F")).findFirst();
        assertTrue(f.isPresent());
        FieldModel fField = f.get();
        assertEquals("F", fField.getName());
        assertEquals("BigInteger", fField.getType());
        assertEquals(BigInteger.ZERO, fField.getDefaultValue());

        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> apiTodo = spreadsheetResultModels.stream()
            .filter(x -> x.getName().equals("apiTodo"))
            .findFirst();
        assertTrue(apiTodo.isPresent());
        SpreadsheetModel sm = apiTodo.get();
        Optional<StepModel> cdTcodeValidationResultStep = sm.getSteps()
            .stream()
            .filter(x -> x.getName().equals("CDTcodeValidationResult"))
            .findFirst();
        assertTrue(cdTcodeValidationResultStep.isPresent());
        StepModel stepModel = cdTcodeValidationResultStep.get();
        assertEquals("BigInteger", stepModel.getType());
        assertEquals("=java.math.BigInteger.ZERO", stepModel.getValue());

        Optional<StepModel> cdTCodeToBeProcessedStep = sm.getSteps()
            .stream()
            .filter(x -> x.getName().equals("CDTCodeToBeProcessed"))
            .findFirst();
        assertTrue(cdTCodeToBeProcessedStep.isPresent());
        StepModel cdTCodeToBeProcessedStepModel = cdTCodeToBeProcessedStep.get();
        assertEquals("BigDecimal", cdTCodeToBeProcessedStepModel.getType());
        assertEquals("=java.math.BigDecimal.ZERO", cdTCodeToBeProcessedStepModel.getValue());
    }

    @Test
    public void testLostDatatype() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/datatype/EPBDS-10843_lost_datatype.json");
        List<DatatypeModel> datatypeModels = projectModel.getDatatypeModels();
        assertTrue(datatypeModels.stream().anyMatch(model -> model.getName().equals("NewNewDatatype")));
    }
}
