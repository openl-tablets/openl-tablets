package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.util.CollectionUtils;

public class DataTableTest {

    private OpenAPIModelConverter converter;

    @Before
    public void setUp() {
        converter = new OpenAPIScaffoldingConverter();
    }

    @Test
    public void testDataTableGenerationEmptyRequest() throws IOException {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/data_tables/EPBDS-10250_data_tables.json");
        List<DataModel> dataModels = projectModel.getDataModels();
        assertFalse(dataModels.isEmpty());
        DataModel petsB = findDataModel(dataModels, "PetsB");
        assertEquals("PetsB", petsB.getName());
        assertEquals("Pet", petsB.getType());
        PathInfo info = petsB.getPathInfo();
        assertEquals("/getpetsB", info.getOriginalPath());
        assertEquals("getPetsB", info.getFormattedPath());
        assertEquals("application/json", info.getProduces());
        assertNull(info.getConsumes());
        assertEquals("Pet[]", info.getReturnType().getSimpleName());

        DatatypeModel datatypeModel = petsB.getDatatypeModel();
        assertEquals("Pet", datatypeModel.getName());
        List<FieldModel> fields = datatypeModel.getFields();
        assertFalse(fields.isEmpty());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("tag")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("id")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("name")));

    }

    @Test
    public void testSpreadsheetResultFiltering() throws IOException {
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/openapi.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        List<DataModel> dataModels = pm.getDataModels();
        Set<DatatypeModel> datatypeModels = pm.getDatatypeModels();
        assertEquals(8, spreadsheetResultModels.size());
        assertTrue(CollectionUtils.isEmpty(dataModels));
        assertEquals(14, datatypeModels.size());
    }

    @Test
    public void testRuleWithRuntimeContext() throws IOException {
        ProjectModel pm = converter
            .extractProjectModel("test.converter/data_tables/openapiRule_with_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    public void testRuleWithoutRuntimeContext() throws IOException {
        ProjectModel pm = converter
            .extractProjectModel("test.converter/data_tables/openapiRule_without_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    public void testNesting() throws IOException {
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/nesting.json");
        List<DataModel> dataModels = pm.getDataModels();
        assertEquals(4, dataModels.size());

        DataModel newDataTypeData = findDataModel(dataModels, "NewDatatypeData");
        List<FieldModel> fields = newDataTypeData.getDatatypeModel().getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("newStrField")));

        DataModel myStrModel = findDataModel(dataModels, "MystrData");
        List<FieldModel> strFields = myStrModel.getDatatypeModel().getFields();
        assertEquals(1, strFields.size());
        FieldModel strModel = strFields.iterator().next();
        assertEquals("this", strModel.getName());

        DataModel superDataModel = findDataModel(dataModels, "SuperDatatypeData");
        List<FieldModel> superFields = superDataModel.getDatatypeModel().getFields();
        assertEquals(2, superFields.size());
        assertTrue(superFields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(superFields.stream().anyMatch(x -> x.getName().equals("newStrField")));

        DataModel dataModel = findDataModel(dataModels, "MyDatatypeData");
        List<FieldModel> myDatatypeFields = dataModel.getDatatypeModel().getFields();
        assertEquals(3, myDatatypeFields.size());
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("newStrField")));
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("r")));
    }

    @Test
    public void testMultipleNesting() throws IOException {
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/multiple_nesting.json");
        List<DataModel> dataModels = pm.getDataModels();
        assertEquals(2, dataModels.size());
        DataModel dataLevelFore = findDataModel(dataModels, "DalaLevelForeData");
        List<FieldModel> fields = dataLevelFore.getDatatypeModel().getFields();
        assertEquals(4, fields.size());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("newField")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("filed1")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("filed2")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("filed4")));

        DataModel dataLevelThree = findDataModel(dataModels, "Arlekino");
        List<FieldModel> dltFields = dataLevelThree.getDatatypeModel().getFields();
        assertEquals(3, dltFields.size());
        assertTrue(dltFields.stream().anyMatch(x -> x.getName().equals("newField")));
        assertTrue(dltFields.stream().anyMatch(x -> x.getName().equals("filed1")));
        assertTrue(dltFields.stream().anyMatch(x -> x.getName().equals("filed2")));

    }

    @Test
    public void testGetPathNaming() throws IOException {
        ProjectModel pm = converter
            .extractProjectModel("test.converter/data_tables/EPBDS-10839_get_capital_letter.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        List<DataModel> dataModels = pm.getDataModels();
        Set<DatatypeModel> datatypeModels = pm.getDatatypeModels();
        assertTrue(dataModels.isEmpty());
        assertEquals(1, datatypeModels.size());
        DatatypeModel dm = datatypeModels.iterator().next();
        assertEquals("JAXRSErrorResponse", dm.getName());

        assertEquals(1, spreadsheetResultModels.size());
        SpreadsheetModel sprModel = spreadsheetResultModels.iterator().next();
        assertEquals("GetMyAlias", sprModel.getName());
        assertEquals("String[]", sprModel.getType());
        List<StepModel> steps = sprModel.getSteps();
        assertEquals(1, steps.size());

        StepModel step = steps.iterator().next();
        assertEquals("Result", step.getName());
        assertEquals("= new String[]{}", step.getValue());
    }

    @Test
    public void test_EPBDS_10990() throws Exception {
        ProjectModel projectModel = converter
                .extractProjectModel("test.converter/data_tables/EPBDS-10990_no_default_runtime_context_generate.json");
        assertEquals(1, projectModel.getDatatypeModels().size());
        ProjectModel projectModel2 = converter
                .extractProjectModel("test.converter/data_tables/EPBDS-10990_default_runtime_context_generate.json");
        assertEquals(3, projectModel2.getDatatypeModels().size());
    }

    @Test
    public void test_dataTables() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/data_tables_types.json");
        List<DataModel> dataModels = projectModel.getDataModels();
        assertEquals(4, dataModels.size());

        DataModel newDatatypeData = findDataModel(dataModels, "NewDatatypeData");
        PathInfo newDatatypeDataPathInfo = newDatatypeData.getPathInfo();
        assertEquals("[Ljava.util.Date;", newDatatypeDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.OBJECT, newDatatypeDataPathInfo.getReturnType().getType());
        assertEquals("getNewDatatypeData", newDatatypeDataPathInfo.getFormattedPath());
        assertEquals("/getNewData/typeData", newDatatypeDataPathInfo.getOriginalPath());

        DataModel mystrData = findDataModel(dataModels, "MystrData");
        PathInfo strDataPathInfo = mystrData.getPathInfo();
        assertEquals("[Ljava.lang.String;", strDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.OBJECT, strDataPathInfo.getReturnType().getType());
        assertEquals("getMystrData", strDataPathInfo.getFormattedPath());
        assertEquals("/getMys/trData", strDataPathInfo.getOriginalPath());

        DataModel myDatatypeData = findDataModel(dataModels, "MyDatatypeData");
        PathInfo myDatatypeDataPathInfo = myDatatypeData.getPathInfo();
        assertEquals("MyDatatype[]", myDatatypeDataPathInfo.getReturnType().getJavaName());
        assertEquals(TypeInfo.Type.DATATYPE, myDatatypeDataPathInfo.getReturnType().getType());
        assertEquals("getMyDatatypeData", myDatatypeDataPathInfo.getFormattedPath());
        assertEquals("/getMyData/typeData",myDatatypeDataPathInfo.getOriginalPath());

        DataModel superDatatypeData = findDataModel(dataModels, "SuperDatatypeData");
        PathInfo superDatatypeDataPathInfo = superDatatypeData.getPathInfo();
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
