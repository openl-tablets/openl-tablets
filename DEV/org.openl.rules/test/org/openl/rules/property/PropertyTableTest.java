package org.openl.rules.property;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class PropertyTableTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/PropertyTableTest.xls";

    public PropertyTableTest() {
        super(SRC);
    }

    @Test
    public void testPropertyTableLoading() {
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);
        ITableProperties tableProperties = resultTsn.getTableProperties();
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
    public void testFielsInOpenClass() {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        IOpenClass openClassWithErrors = compiledOpenClass.getOpenClassWithErrors();
        Map<String, IOpenField> fields = openClassWithErrors.getFields();
        assertTrue(fields.containsKey("categoryProp"));
        for (String fieldName : fields.keySet()) {
            IOpenField field = openClassWithErrors.getField(fieldName);
            if (field instanceof PropertiesOpenField) {
                IRuntimeEnv environment = new SimpleVM().getRuntimeEnv();
                Object myInstance = openClassWithErrors.newInstance(environment);
                ITableProperties properties = (ITableProperties) field.get(myInstance, environment);
                String scope = properties.getScope();
                assertFalse(InheritanceLevel.MODULE.getDisplayName().equalsIgnoreCase(scope));
            }
        }
    }

}
