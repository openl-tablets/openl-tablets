package org.openl.rules.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenField;
import org.openl.vm.SimpleVM;

class PropertyTableTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/PropertyTableTest.xls";

    public PropertyTableTest() {
        super(SRC);
    }

    @Test
    void testPropertyTableLoading() {
        var tableName = "Rules void hello1(int hour)";
        var resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        var tableProperties = resultTsn.getTableProperties();
        assertNotNull(tableProperties);

        Map<String, Object> moduleProperties = tableProperties.getModuleProperties();
        assertEquals(3, moduleProperties.size());
        assertEquals(InheritanceLevel.MODULE.getDisplayName(), moduleProperties.get("scope"));
        assertEquals("Any phase", moduleProperties.get("buildPhase"));
        assertEquals(ValidateDTEnum.ON, moduleProperties.get("validateDT"));

        Map<String, Object> categoryProperties = tableProperties.getCategoryProperties();
        assertEquals(4, categoryProperties.size());
        assertEquals(InheritanceLevel.CATEGORY.getDisplayName(), categoryProperties.get("scope"));
        assertEquals("newLob", ((String[]) categoryProperties.get("lob"))[0]);
        assertEquals(UsRegionsEnum.SE, ((UsRegionsEnum[]) categoryProperties.get("usregion"))[0]);
        assertEquals(RegionsEnum.NCSA, ((RegionsEnum[]) categoryProperties.get("region"))[0]);

        Map<String, Object> defaultProperties = tableProperties.getDefaultProperties();
        // assertTrue(defaultProperties.size() == 5);
        // assertEquals("US",(String) defaultProperties.get("country"));

        assertTrue((Boolean) defaultProperties.get("active"));
        assertFalse((Boolean) defaultProperties.get("failOnMiss"));
    }

    @Test
    void testFieldsInOpenClass() {
        var compiledOpenClass = getCompiledOpenClass();
        var openClassWithErrors = compiledOpenClass.getOpenClassWithErrors();
        Collection<IOpenField> fields = openClassWithErrors.getFields();
        assertTrue(fields.stream().anyMatch(e -> "categoryProp".equals(e.getName())));
        for (IOpenField openField : fields) {
            var field = openClassWithErrors.getField(openField.getName());
            if (field instanceof PropertiesOpenField) {
                var environment = new SimpleVM().getRuntimeEnv();
                var myInstance = openClassWithErrors.newInstance(environment);
                var properties = (ITableProperties) field.get(myInstance, environment);
                var scope = properties.getScope();
                assertFalse(InheritanceLevel.MODULE.getDisplayName().equalsIgnoreCase(scope));
            }
        }
    }

}
