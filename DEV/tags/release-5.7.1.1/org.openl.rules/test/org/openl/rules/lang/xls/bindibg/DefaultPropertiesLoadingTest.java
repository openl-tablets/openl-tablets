package org.openl.rules.lang.xls.bindibg;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

public class DefaultPropertiesLoadingTest extends BaseOpenlBuilderHelper{
    
    private static String __src = "test/rules/DefaultPropertiesLoadingTest.xls";
    
    public DefaultPropertiesLoadingTest() {
        super(__src);        
    }    
    
    @Test
    public void testLoadingDefaultValuesForPreviouslyEmptyProp() {  
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);       
        if (resultTsn != null) {
            List<TablePropertyDefinition> defaultPropDefinitions = TablePropertyDefinitionUtils
                                                                           .getPropertiesToBeSetByDefault();
            assertEquals("Check that number of properties defined in table is 0",
                    resultTsn.getTableProperties().getPropertiesDefinedInTable().size(), 0);                
            List<String> defaultPropDefinitionsNames = new ArrayList<String>();
            for (TablePropertyDefinition dataPropertyDefinition : defaultPropDefinitions) {
                defaultPropDefinitionsNames.add(dataPropertyDefinition.getName());
            }
            assertTrue("Tsn doesn`t have properties defined in appropriate table in excel", 
                    !resultTsn.hasPropertiesDefinedInTable());                
            List<String> tsnPropNames = new ArrayList<String>();
            for (Map.Entry<String, Object> property : resultTsn.getTableProperties().getAllProperties().entrySet()) {
                tsnPropNames.add(property.getKey());
            }
            assertTrue("Tsn contains all properties that must be set by default",
                    tsnPropNames.containsAll(defaultPropDefinitionsNames));
       } else {
           fail();
       }
    } 

}
