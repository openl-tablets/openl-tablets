package org.openl.rules.lang.xls.bindibg;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

public class TestPropArray {
    private String __src = "test/rules/TestArrayInPropSection.xls";
    
    
    private XlsModuleSyntaxNode getTables() {        
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, __src);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }
    
    @Test
    public void testLoadingArrayInPropertyTableSection() {       
        XlsModuleSyntaxNode tables = getTables();
        TableSyntaxNode[] tsns = tables.getXlsTableSyntaxNodesWithoutErrors();
        for (TableSyntaxNode tsn : tsns) {
            if ("Rules DoubleValue driverRiskScoreOverloadTest(String driverRisk)".equals(tsn.getDisplayName())) {
//                List<TablePropertyDefinition> defaultPropDefinitions = TablePropertyDefinitionUtils
//                                                                           .getPropertiesToBeSetByDefault();
                assertEquals("Check that number of properties defined in table is 4",
                        tsn.getTableProperties().getPropertiesDefinedInTable().size(), 4);
                assertEquals("tag1", tsn.getTableProperties().getTags()[0]);
                assertEquals("tag3", tsn.getTableProperties().getTags()[1]);
                assertEquals("tag4", tsn.getTableProperties().getTags()[2]);
            }        
        }
       
    }

}
