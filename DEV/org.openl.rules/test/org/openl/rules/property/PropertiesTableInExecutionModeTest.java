package org.openl.rules.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.enumeration.ValidateDTEnum;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Test for properties recognition in execution mode.
 * 
 * @author PUdalau
 */
public class PropertiesTableInExecutionModeTest {

    private static final String SRC = "test/rules/PropertyTableTest.xls";

    @Test
    public void testPropertyTableLoading() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(SRC);
        engineFactory.setExecutionMode(true);
        CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();
        IOpenMethod method = compiledOpenClass.getOpenClass().getMethod("hello1",
                new IOpenClass[] { JavaOpenClass.INT });
        if (method != null) {
            ITableProperties tableProperties = PropertiesHelper.getTableProperties(method);
            assertNotNull(tableProperties);

            Map<String, Object> moduleProperties = tableProperties.getModuleProperties();
            assertTrue(moduleProperties.size() == 3);
            assertEquals(InheritanceLevel.MODULE.getDisplayName(), (String) moduleProperties.get("scope"));
            assertEquals("Any phase", (String) moduleProperties.get("buildPhase"));
            assertEquals(ValidateDTEnum.ON, (ValidateDTEnum) moduleProperties.get("validateDT"));

            Map<String, Object> categoryProperties = tableProperties.getCategoryProperties();
            assertTrue(categoryProperties.size() == 4);
            assertEquals(InheritanceLevel.CATEGORY.getDisplayName(), (String) categoryProperties.get("scope"));
            assertEquals("newLob", ((String[]) categoryProperties.get("lob"))[0]);
            assertEquals(UsRegionsEnum.SE.name(), ((UsRegionsEnum[]) categoryProperties.get("usregion"))[0].name());
            assertEquals(RegionsEnum.NCSA.name(), ((RegionsEnum[]) categoryProperties.get("region"))[0].name());

            Map<String, Object> defaultProperties = tableProperties.getDefaultProperties();
            // assertTrue(defaultProperties.size() == 5);
            // assertEquals("US",(String) defaultProperties.get("country"));

            assertTrue((Boolean) defaultProperties.get("active"));
            assertFalse((Boolean) defaultProperties.get("failOnMiss"));
        } else {
            fail();
        }
    }

    @Test
    public void testFielsInOpenClass() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(SRC);
        engineFactory.setExecutionMode(true);
        CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();
        Map<String, IOpenField> fields = compiledOpenClass.getOpenClass().getFields();
        // properties table with name will be represented as field
        assertTrue(fields.containsKey("categoryProp"));
        // properties table without name will not be represented as field
        IRuntimeEnv env = engineFactory.getOpenL().getVm().getRuntimeEnv();
        for (Entry<String, IOpenField> field : fields.entrySet()) {
            if (field.getValue() instanceof PropertiesOpenField) {
                ITableProperties properties = (ITableProperties) field.getValue().get(
                        compiledOpenClass.getOpenClass().newInstance(env), env);
                String scope = properties.getScope();
                assertFalse(InheritanceLevel.MODULE.getDisplayName().equalsIgnoreCase(scope));
            }
        }
    }
}
