package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.util.CollectionUtils;

public class DataTableTest {

    @Test
    public void testDataTableGenerationEmptyRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/data_tables/EPBDS-10250_data_tables.json");
        List<DataModel> dataModels = projectModel.getDataModels();
        assertFalse(dataModels.isEmpty());
        Optional<DataModel> optionalPetsB = dataModels.stream().filter(x -> x.getName().equals("PetsB")).findFirst();
        assertTrue(optionalPetsB.isPresent());
        DataModel petsB = optionalPetsB.get();
        assertEquals("PetsB", petsB.getName());
        assertEquals("Pet", petsB.getType());
        PathInfo info = petsB.getInfo();
        assertEquals("/getpetsB", info.getOriginalPath());
        assertEquals("getpetsB", info.getFormattedPath());
        assertEquals("application/json", info.getProduces());
        assertNull(info.getConsumes());
        assertEquals("Object", info.getReturnType());

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
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/openapi.json");
        List<SpreadsheetModel> spreadsheetResultModels = pm.getSpreadsheetResultModels();
        List<DataModel> dataModels = pm.getDataModels();
        List<DatatypeModel> datatypeModels = pm.getDatatypeModels();
        assertEquals(8, spreadsheetResultModels.size());
        assertTrue(CollectionUtils.isEmpty(dataModels));
        assertEquals(14, datatypeModels.size());
    }

    @Test
    public void testRuleWithRuntimeContext() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter
            .extractProjectModel("test.converter/data_tables/openapiRule_with_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    public void testRuleWithoutRuntimeContext() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter
            .extractProjectModel("test.converter/data_tables/openapiRule_without_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    public void testNesting() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/nesting.json");
        List<DataModel> dataModels = pm.getDataModels();
        assertEquals(4, dataModels.size());

        Optional<DataModel> newDatatypeDataOptional = dataModels.stream()
            .filter(x -> x.getName().equals("NewDatatypeData"))
            .findFirst();
        assertTrue(newDatatypeDataOptional.isPresent());
        DataModel newDataTypeData = newDatatypeDataOptional.get();
        List<FieldModel> fields = newDataTypeData.getDatatypeModel().getFields();
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(fields.stream().anyMatch(x -> x.getName().equals("newStrField")));

        Optional<DataModel> myStrOptional = dataModels.stream()
            .filter(x -> x.getName().equals("MystrData"))
            .findFirst();
        assertTrue(myStrOptional.isPresent());
        DataModel myStrModel = myStrOptional.get();
        List<FieldModel> strFields = myStrModel.getDatatypeModel().getFields();
        assertEquals(1, strFields.size());
        FieldModel strModel = strFields.iterator().next();
        assertEquals("this", strModel.getName());

        Optional<DataModel> superDatatypeOptional = dataModels.stream()
            .filter(x -> x.getName().equals("SuperDatatypeData"))
            .findFirst();
        assertTrue(superDatatypeOptional.isPresent());
        DataModel superDataModel = superDatatypeOptional.get();
        List<FieldModel> superFields = superDataModel.getDatatypeModel().getFields();
        assertEquals(2, superFields.size());
        assertTrue(superFields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(superFields.stream().anyMatch(x -> x.getName().equals("newStrField")));

        Optional<DataModel> myDatatypeDataOptional = dataModels.stream()
            .filter(x -> x.getName().equals("MyDatatypeData"))
            .findFirst();
        assertTrue(myDatatypeDataOptional.isPresent());
        DataModel dataModel = myDatatypeDataOptional.get();
        List<FieldModel> myDatatypeFields = dataModel.getDatatypeModel().getFields();
        assertEquals(3, myDatatypeFields.size());
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("dtpField")));
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("newStrField")));
        assertTrue(myDatatypeFields.stream().anyMatch(x -> x.getName().equals("r")));
    }

    @Test
    public void testMultipleNesting() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/multiple_nesting.json");
        List<DataModel> dataModels = pm.getDataModels();
        assertEquals(2, dataModels.size());
        Optional<DataModel> dataLevelForeData = dataModels.stream().filter(x -> x.getName().equals("DalaLevelForeData")).findFirst();
        assertTrue(dataLevelForeData.isPresent());
        DataModel dataLevelFore = dataLevelForeData.get();
        List<FieldModel> fields = dataLevelFore.getDatatypeModel().getFields();
        assertEquals(4, fields.size());
        assertTrue(fields.stream().anyMatch(x->x.getName().equals("newField")));
        assertTrue(fields.stream().anyMatch(x->x.getName().equals("filed1")));
        assertTrue(fields.stream().anyMatch(x->x.getName().equals("filed2")));
        assertTrue(fields.stream().anyMatch(x->x.getName().equals("filed4")));

        Optional<DataModel> dataLevelThreeData = dataModels.stream().filter(x -> x.getName().equals("Arlekino")).findFirst();
        assertTrue(dataLevelThreeData.isPresent());
        DataModel dataLevelThree = dataLevelThreeData.get();
        List<FieldModel> dltFields = dataLevelThree.getDatatypeModel().getFields();
        assertEquals(3, dltFields.size());
        assertTrue(dltFields.stream().anyMatch(x->x.getName().equals("newField")));
        assertTrue(dltFields.stream().anyMatch(x->x.getName().equals("filed1")));
        assertTrue(dltFields.stream().anyMatch(x->x.getName().equals("filed2")));

    }
}
