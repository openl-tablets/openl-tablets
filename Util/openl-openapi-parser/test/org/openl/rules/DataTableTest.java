package org.openl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.util.CollectionUtils;

class DataTableTest {

    private OpenAPIModelConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OpenAPIScaffoldingConverter();
    }

    @Test
    void testDataTableGenerationEmptyRequest() throws IOException {
        var projectModel = converter
                .extractProjectModel("test.converter/data_tables/EPBDS-10250_data_tables.json");
        List<DataModel> dataModels = projectModel.getDataModels();
        assertFalse(dataModels.isEmpty());
        var petsB = findDataModel(dataModels, "PetsB");
        assertEquals("PetsB", petsB.getName());
        assertEquals("Pet", petsB.getType());
        var info = petsB.getPathInfo();
        assertEquals("/getpetsB", info.getOriginalPath());
        assertEquals("getPetsB", info.getFormattedPath());
        assertEquals("application/json", info.getProduces());
        assertNull(info.getConsumes());
        assertEquals("Pet[]", info.getReturnType().getSimpleName());

        var datatypeModel = petsB.getDatatypeModel();
        assertEquals("Pet", datatypeModel.getName());
        List<FieldModel> fields = datatypeModel.getFields();
        assertFalse(fields.isEmpty());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("tag")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("id")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("name")));

    }

    @Test
    void testSpreadsheetResultFiltering() throws IOException {
        var pm = converter.extractProjectModel("test.converter/data_tables/openapi.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        List<DataModel> dataModels = pm.getDataModels();
        Set<DatatypeModel> datatypeModels = pm.getDatatypeModels();
        assertEquals(8, spreadsheetResultModels.size());
        assertTrue(CollectionUtils.isEmpty(dataModels));
        assertEquals(14, datatypeModels.size());
    }

    @Test
    void testRuleWithRuntimeContext() throws IOException {
        var pm = converter
                .extractProjectModel("test.converter/data_tables/openapiRule_with_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    void testRuleWithoutRuntimeContext() throws IOException {
        var pm = converter
                .extractProjectModel("test.converter/data_tables/openapiRule_without_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    void testNesting() throws IOException {
        var pm = converter.extractProjectModel("test.converter/data_tables/nesting.json");
        List<DataModel> dataModels = pm.getDataModels();
        assertEquals(4, dataModels.size());

        var newDataTypeData = findDataModel(dataModels, "NewDatatypeData");
        List<FieldModel> fields = newDataTypeData.getDatatypeModel().getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("newStrField")));

        var myStrModel = findDataModel(dataModels, "MystrData");
        List<FieldModel> strFields = myStrModel.getDatatypeModel().getFields();
        assertEquals(1, strFields.size());
        var strModel = strFields.getFirst();
        assertEquals("this", strModel.getName());

        var superDataModel = findDataModel(dataModels, "SuperDatatypeData");
        List<FieldModel> superFields = superDataModel.getDatatypeModel().getFields();
        assertEquals(2, superFields.size());
        assertTrue(superFields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(superFields.stream().anyMatch(x -> x.getName().equals("newStrField")));

        var dataModel = findDataModel(dataModels, "MyDatatypeData");
        List<FieldModel> myDatatypeFields = dataModel.getDatatypeModel().getFields();
        assertEquals(3, myDatatypeFields.size());
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("newStrField")));
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("r")));
    }

    @Test
    void testMultipleNesting() throws IOException {
        var pm = converter.extractProjectModel("test.converter/data_tables/multiple_nesting.json");
        List<DataModel> dataModels = pm.getDataModels();
        assertEquals(2, dataModels.size());
        var dataLevelFore = findDataModel(dataModels, "DalaLevelForeData");
        List<FieldModel> fields = dataLevelFore.getDatatypeModel().getFields();
        assertEquals(4, fields.size());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("newField")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("filed1")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("filed2")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("filed4")));

        var dataLevelThree = findDataModel(dataModels, "Arlekino");
        List<FieldModel> dltFields = dataLevelThree.getDatatypeModel().getFields();
        assertEquals(3, dltFields.size());
        assertTrue(dltFields.stream().anyMatch(x -> x.getName().equals("newField")));
        assertTrue(dltFields.stream().anyMatch(x -> x.getName().equals("filed1")));
        assertTrue(dltFields.stream().anyMatch(x -> x.getName().equals("filed2")));

    }

    @Test
    void testGetPathNaming() throws IOException {
        var pm = converter
                .extractProjectModel("test.converter/data_tables/EPBDS-10839_get_capital_letter.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        List<DataModel> dataModels = pm.getDataModels();
        Set<DatatypeModel> datatypeModels = pm.getDatatypeModels();
        assertTrue(dataModels.isEmpty());
        assertEquals(1, datatypeModels.size());
        var dm = datatypeModels.iterator().next();
        assertEquals("JAXRSErrorResponse", dm.getName());

        assertEquals(1, spreadsheetResultModels.size());
        var sprModel = spreadsheetResultModels.getFirst();
        assertEquals("GetMyAlias", sprModel.getName());
        assertEquals("String[]", sprModel.getType());
        List<StepModel> steps = sprModel.getSteps();
        assertEquals(1, steps.size());

        var step = steps.getFirst();
        assertEquals("Result", step.getName());
        assertEquals("= new String[]{}", step.getValue());
    }

    @Test
    void test_dataTables() throws Exception {
        var projectModel = converter.extractProjectModel("test.converter/problems/data_tables_types.json");
        List<DataModel> dataModels = projectModel.getDataModels();
        assertEquals(4, dataModels.size());

        var newDatatypeData = findDataModel(dataModels, "NewDatatypeData");
        var newDatatypeDataPathInfo = newDatatypeData.getPathInfo();
        assertEquals("[Ljava.util.Date;", newDatatypeDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.OBJECT, newDatatypeDataPathInfo.getReturnType().getType());
        assertEquals("getNewDatatypeData", newDatatypeDataPathInfo.getFormattedPath());
        assertEquals("/getNewData/typeData", newDatatypeDataPathInfo.getOriginalPath());

        var mystrData = findDataModel(dataModels, "MystrData");
        var strDataPathInfo = mystrData.getPathInfo();
        assertEquals("[Ljava.lang.String;", strDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.OBJECT, strDataPathInfo.getReturnType().getType());
        assertEquals("getMystrData", strDataPathInfo.getFormattedPath());
        assertEquals("/getMys/trData", strDataPathInfo.getOriginalPath());

        var myDatatypeData = findDataModel(dataModels, "MyDatatypeData");
        var myDatatypeDataPathInfo = myDatatypeData.getPathInfo();
        assertEquals("MyDatatype[]", myDatatypeDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.DATATYPE, myDatatypeDataPathInfo.getReturnType().getType());
        assertEquals("getMyDatatypeData", myDatatypeDataPathInfo.getFormattedPath());
        assertEquals("/getMyData/typeData", myDatatypeDataPathInfo.getOriginalPath());

        var superDatatypeData = findDataModel(dataModels, "SuperDatatypeData");
        var superDatatypeDataPathInfo = superDatatypeData.getPathInfo();
        assertEquals("SuperDatatype[]", superDatatypeDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.DATATYPE, superDatatypeDataPathInfo.getReturnType().getType());
        assertEquals("getSuperDatatypeData", superDatatypeDataPathInfo.getFormattedPath());
        assertEquals("/getSuper/DatatypeData", superDatatypeDataPathInfo.getOriginalPath());
    }

    private DataModel findDataModel(final List<DataModel> dataModels, final String modelName) {
        Optional<DataModel> optionalResult = dataModels.stream().filter(x -> x.getName().equals(modelName)).findFirst();
        assertTrue(optionalResult.isPresent());
        return optionalResult.get();
    }
}
