package org.openl.rules.property;

import static org.junit.Assert.*;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.enumeration.LanguagesEnum;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

public class PropertyTableTest extends BaseOpenlBuilderHelper{    

    private static String __src = "test/rules/PropertyTableTest.xls";
    
    public PropertyTableTest() {
        super(__src);        
    }
    
    @Test
    public void testPropertyTableLoading() {
        String tableName = "Rules void hello1(int hour)";        
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
                
            Map<String, Object> moduleProperties = tableProperties.getPropertiesAppliedForModule();                
            assertTrue(moduleProperties.size() == 3);
            assertEquals(InheritanceLevel.MODULE.getDisplayName(),(String) moduleProperties.get("scope"));
            assertEquals("Any phase",(String) moduleProperties.get("buildPhase"));                
            assertEquals("on",(String) moduleProperties.get("validateDT"));
                
            Map<String, Object> categoryProperties = tableProperties.getPropertiesAppliedForCategory();                
            assertTrue(categoryProperties.size() == 4);
            assertEquals(InheritanceLevel.CATEGORY.getDisplayName(),(String) categoryProperties.get("scope"));
            assertEquals("newLob",(String) categoryProperties.get("lob"));
            assertEquals(UsRegionsEnum.SE.name(), ((UsRegionsEnum[])categoryProperties.get("usregion"))[0].name());                
            assertEquals(RegionsEnum.NCSA.name(),((RegionsEnum[]) categoryProperties.get("region"))[0].name());
                
             Map<String, Object> defaultProperties = tableProperties.getPropertiesAppliedByDefault();
            // assertTrue(defaultProperties.size() == 5);
            // assertEquals("US",(String) defaultProperties.get("country"));
            assertEquals(org.openl.rules.enumeration.CurrenciesEnum.USD,
                    ((org.openl.rules.enumeration.CurrenciesEnum[]) defaultProperties.get("currency"))[0]);
            assertEquals(LanguagesEnum.EN.name(),((LanguagesEnum[]) defaultProperties.get("lang"))[0].name());
            assertTrue((Boolean) defaultProperties.get("active"));
            assertFalse((Boolean) defaultProperties.get("failOnMiss"));
            } else {
                fail();
        }
    }
}
