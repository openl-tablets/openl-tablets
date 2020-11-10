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

public class DataTableTest {

    @Test
    public void testDataTableGenerationEmptyRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/data_tables/EPBDS-10250_data_tables.json");
        List<DataModel> dataModels = projectModel.getDataModels();
        assertFalse(dataModels.isEmpty());
        DataModel petsB = dataModels.iterator().next();
        assertEquals("petsB", petsB.getName());
        assertEquals("Pet", petsB.getType());
        PathInfo info = petsB.getInfo();
        assertEquals("/petsB", info.getOriginalPath());
        assertEquals("petsB", info.getFormattedPath());
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
        assertEquals(5, spreadsheetResultModels.size());
        assertEquals(2, dataModels.size());
        assertEquals(14, datatypeModels.size());

        Optional<DataModel> childDatatype1DataTableOptional = dataModels.stream()
            .filter(x -> x.getName().equals("childDatatype1"))
            .findFirst();
        assertTrue(childDatatype1DataTableOptional.isPresent());
        DataModel childDatatype1Model = childDatatype1DataTableOptional.get();
        assertEquals("childDatatype1", childDatatype1Model.getType());
        PathInfo info = childDatatype1Model.getInfo();
        assertEquals("/childDatatype1", info.getOriginalPath());
        assertEquals("childDatatype1", info.getFormattedPath());
        assertEquals("application/json", info.getProduces());
        assertEquals("GET", info.getOperation());
        DatatypeModel datatypeModel = childDatatype1Model.getDatatypeModel();
        assertEquals("childDatatype1", datatypeModel.getName());
        assertEquals("parentDatatype1", datatypeModel.getParent());
        assertTrue(datatypeModel.getFields().isEmpty());

        Optional<DataModel> childDatatype2DataTableOptional = dataModels.stream()
            .filter(x -> x.getName().equals("childDatatype2"))
            .findFirst();
        assertTrue(childDatatype2DataTableOptional.isPresent());
        DataModel childDatatype2Model = childDatatype2DataTableOptional.get();
        assertEquals("childDatatype2", childDatatype2Model.getType());
        PathInfo childDatatype2ModelInfo = childDatatype2Model.getInfo();
        assertEquals("/childDatatype2", childDatatype2ModelInfo.getOriginalPath());
        assertEquals("childDatatype2", childDatatype2ModelInfo.getFormattedPath());
        assertEquals("application/json", childDatatype2ModelInfo.getProduces());
        assertEquals("GET", childDatatype2ModelInfo.getOperation());
        DatatypeModel datatype2Model = childDatatype2Model.getDatatypeModel();
        assertEquals("childDatatype2", datatype2Model.getName());
        assertEquals("parentDatatype1", datatype2Model.getParent());
        assertEquals(1, datatype2Model.getFields().size());

    }
}
