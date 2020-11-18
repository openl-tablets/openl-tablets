package org.openl.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

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
        DataModel petsB = dataModels.iterator().next();
        assertEquals("PetsB", petsB.getName());
        assertEquals("Pet", petsB.getType());
        PathInfo info = petsB.getInfo();
        assertEquals("/getpetsB", info.getOriginalPath());
        assertEquals("getpetsB", info.getFormattedPath());
        assertEquals("application/json", info.getProduces());
        assertNull(info.getConsumes());
        assertEquals("Object", info.getReturnType());

        DatatypeModel datatypeModel = petsB.getDatatypeModel();
        assertEquals("NewPet", datatypeModel.getParent());
        assertEquals("Pet", datatypeModel.getName());
        List<FieldModel> fields = datatypeModel.getFields();
        assertFalse(fields.isEmpty());
        FieldModel fm = fields.iterator().next();
        assertEquals("id", fm.getName());
        assertEquals("Long", fm.getType());
        assertNull(fm.getDefaultValue());
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
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/openapiRule_with_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }

    @Test
    public void testRuleWithoutRuntimeContext() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel pm = converter.extractProjectModel("test.converter/data_tables/openapiRule_without_runtimeContext.json");
        assertTrue(CollectionUtils.isEmpty(pm.getDataModels()));
    }
}
