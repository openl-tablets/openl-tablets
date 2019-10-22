package org.openl.rules.table.properties;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

public class TablePropertiesTest extends BaseOpenlBuilderHelper {

    private static final String PROPERTY_ACTIVE = "active";

    public TablePropertiesTest() {
        super(SRC);
    }

    private static final String SRC = "test/rules/Tutorial_4_Test.xls";

    private static final String PROPERTY_BUILD_PHASE = "buildPhase";
    private static final String PROPERTY_REGION = "region";
    private static final String PROPERTY_USREGION = "usregion";
    private static final String PROPERTY_LOB = "lob";
    private static final String PROPERTY_CREATED_BY = "createdBy";
    private static final String PROPERTY_DESCRIPTION = "description";
    private static final String PROPERTY_EFFECTIVE_DATE = "effectiveDate";
    private static final String PROPERTY_EXPIRATION_DATE = "expirationDate";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_FAIL_ON_MISS = "failOnMiss";

    private TableProperties initTableProperties() {
        TableProperties tableProperties = new TableProperties();
        tableProperties.setCurrentTableType(XlsNodeTypes.XLS_METHOD.toString());
        tableProperties.setFieldValue(PROPERTY_NAME, "newName");
        tableProperties.setFieldValue(PROPERTY_DESCRIPTION, "newDescription");
        tableProperties.setFieldValue(PROPERTY_CREATED_BY, "tableLevelCreatedBy");
        return tableProperties;
    }

    private Map<String, Object> initDefaultProperties() {
        List<TablePropertyDefinition> propertiesWithDefaultValues = TablePropertyDefinitionUtils
            .getPropertiesToBeSetByDefault();
        Map<String, Object> defaultProperties = new HashMap<>();

        for (TablePropertyDefinition propertyWithDefaultValue : propertiesWithDefaultValues) {
            String propertyName = propertyWithDefaultValue.getName();
            defaultProperties.put(propertyName, propertyName + "Value");
        }
        return defaultProperties;
    }

    private Map<String, Object> initModuleProperties() {
        Map<String, Object> moduleProperties = new HashMap<>();
        moduleProperties.put(PROPERTY_BUILD_PHASE, "moduleLevelBuildPhase");
        moduleProperties.put(PROPERTY_CREATED_BY, "moduleLevelCreatedBy");
        moduleProperties.put(PROPERTY_EXPIRATION_DATE, new Date());
        return moduleProperties;
    }

    private Map<String, Object> initCategoryProperties() {
        Map<String, Object> categoryProperties = new HashMap<>();
        categoryProperties.put(PROPERTY_LOB, "newLob");
        categoryProperties.put(PROPERTY_USREGION, "alaska");
        categoryProperties.put(PROPERTY_REGION, "North America");
        categoryProperties.put(PROPERTY_BUILD_PHASE, "categoryLevelBuildPhase");
        return categoryProperties;
    }

    private TableProperties initProperties() {
        TableProperties tableProperties = initTableProperties();

        Map<String, Object> categoryProperties = initCategoryProperties();
        tableProperties.setCategoryProperties(categoryProperties);

        Map<String, Object> moduleProperties = initModuleProperties();
        tableProperties.setModuleProperties(moduleProperties);

        Map<String, Object> defaultProperties = initDefaultProperties();
        tableProperties.setDefaultProperties(defaultProperties);

        return tableProperties;
    }

    private String getPropertyValueAsString(String propertyName, Object propertyValue) {
        String result;
        TableProperties tablProp = new TableProperties();
        tablProp.setFieldValue(propertyName, propertyValue);
        result = tablProp.getPropertyValueAsString(propertyName);
        return result;
    }

    @Test
    public void testPropertyDef() {
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        assertEquals(61, tsns.length);
        assertEquals("Driver Age Type Table", tsns[4].getTableProperties().getName());
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        assertEquals("02/04/2237",
            sdf.format((Date) tsns[4].getTableProperties().getPropertyValue(PROPERTY_EFFECTIVE_DATE)));
    }

    @Test
    public void testGetValueAsString() {
        String result = null;
        String propertyNameValue = "MyName";
        result = getPropertyValueAsString(PROPERTY_NAME, propertyNameValue);
        assertEquals(propertyNameValue, result);

        Date dateValue = new Date(4098);
        result = getPropertyValueAsString(PROPERTY_EFFECTIVE_DATE, dateValue);
        SimpleDateFormat sDF = new SimpleDateFormat(
            TablePropertyDefinitionUtils.getPropertyByName(PROPERTY_EFFECTIVE_DATE).getFormat());
        assertEquals(sDF.format(dateValue), result);

        result = getPropertyValueAsString(PROPERTY_FAIL_ON_MISS, new Boolean(true));
        assertEquals("true", result);
    }

    @Test
    public void testInheritanceProperties() {
        TableProperties tableProperties = initProperties();

        Map<String, Object> allProperties = tableProperties.getAllProperties();

        // check table properties.
        assertTrue(allProperties.containsKey(PROPERTY_NAME));
        assertTrue(allProperties.containsKey(PROPERTY_DESCRIPTION));

        // check that property was overriden on TABLE level.
        assertEquals("tableLevelCreatedBy", allProperties.get(PROPERTY_CREATED_BY));

        // check that CATEGORY properties are inherited.
        assertTrue(allProperties.containsKey(PROPERTY_LOB));
        assertTrue(allProperties.containsKey(PROPERTY_USREGION));
        assertTrue(allProperties.containsKey(PROPERTY_REGION));

        // check that property was overriden on CATEGORY level.
        assertEquals("categoryLevelBuildPhase", allProperties.get(PROPERTY_BUILD_PHASE));

        // check that MODULE properties are inherited.
        assertTrue(allProperties.containsKey(PROPERTY_EXPIRATION_DATE));
    }

    @Test
    public void testPropertiesLevelDefinedOn() {
        TableProperties tableProperties = initProperties();

        // checks that property PROPERTY_NAME is defined on TABLE level.
        assertEquals(InheritanceLevel.TABLE, tableProperties.getPropertyLevelDefinedOn(PROPERTY_NAME));

        // checks that property PROPERTY_LOB is defined on CATEGORY level.
        assertEquals(InheritanceLevel.CATEGORY, tableProperties.getPropertyLevelDefinedOn(PROPERTY_LOB));

        // checks that property PROPERTY_EXPIRATION_DATE is defined on MODULE
        // level.
        assertEquals(InheritanceLevel.MODULE, tableProperties.getPropertyLevelDefinedOn(PROPERTY_EXPIRATION_DATE));

        // checks that property PROPERTY_ACTIVE, that is applied by default,
        // doesn`t have defined level.
        assertNull(tableProperties.getPropertyLevelDefinedOn(PROPERTY_ACTIVE));

        // checks that property PROPERTY_ACTIVE is applied by default.
        assertTrue(tableProperties.isPropertyAppliedByDefault(PROPERTY_ACTIVE));

    }
}
