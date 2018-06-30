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
import org.openl.types.IOpenField;

public class PropertyTableTest extends BaseOpenlBuilderHelper{    

    private static final String SRC = "test/rules/PropertyTableTest.xls";
    
    public PropertyTableTest() {
        super(SRC);        
    }
    
    @Test
    public void testPropertyTableLoading() {
        String tableName = "Rules void hello1(int hour)";        
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
                
            Map<String, Object> moduleProperties = tableProperties.getModuleProperties();                
            assertTrue(moduleProperties.size() == 3);
            assertEquals(InheritanceLevel.MODULE.getDisplayName(),(String) moduleProperties.get("scope"));
            assertEquals("Any phase",(String) moduleProperties.get("buildPhase"));                
            assertEquals(ValidateDTEnum.ON, (ValidateDTEnum) moduleProperties.get("validateDT"));
                
            Map<String, Object> categoryProperties = tableProperties.getCategoryProperties();                
            assertTrue(categoryProperties.size() == 4);
            assertEquals(InheritanceLevel.CATEGORY.getDisplayName(),(String) categoryProperties.get("scope"));
            assertEquals("newLob",(String) categoryProperties.get("lob"));
            assertEquals(UsRegionsEnum.SE.name(), ((UsRegionsEnum[])categoryProperties.get("usregion"))[0].name());                
            assertEquals(RegionsEnum.NCSA.name(),((RegionsEnum[]) categoryProperties.get("region"))[0].name());
                
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
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        Map<String, IOpenField> fields = compiledOpenClass.getOpenClassWithErrors().getFields();
        //properties table with name will be represented as field
        assertTrue(fields.containsKey("categoryProp"));
        //properties table without name will not be represented as field
        for(String fieldName: fields.keySet()){
            if(getField(fieldName) instanceof PropertiesOpenField){
                ITableProperties properties = (ITableProperties)getFieldValue(fieldName);
                String scope = properties.getScope();
                assertFalse(InheritanceLevel.MODULE.getDisplayName().equalsIgnoreCase(scope));
            }
        }
    }
}
