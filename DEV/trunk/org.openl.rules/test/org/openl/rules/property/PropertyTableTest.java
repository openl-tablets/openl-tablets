package org.openl.rules.property;

import static org.junit.Assert.*;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;

import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class PropertyTableTest {    

    private String __src = "test/rules/PropertyTableTest.xls";
    private XlsModuleSyntaxNode xsn = null;
    
    @Before
    public void getTables() {        
        OpenClassJavaWrapper wrapper = getJavaWrapper();
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();        
    }

    private OpenClassJavaWrapper getJavaWrapper() {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, __src);
        return wrapper;
    }
    
    @Test
    public void testPropertyTableLoading() {
        XlsModuleSyntaxNode module = xsn;
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        
        for (TableSyntaxNode tsn : tsns) {
            if ("Rules void hello1(int hour)".equals(tsn.getDisplayName())) {
                ITableProperties tableProperties  = tsn.getTableProperties();
                assertNotNull(tableProperties);
                
                Map<String, Object> moduleProperties = tableProperties.getPropertiesAppliedForModule();                
                assertTrue(moduleProperties.size() == 3);
                assertEquals("module",(String) moduleProperties.get("scope"));
                assertEquals("Any phase",(String) moduleProperties.get("buildPhase"));                
                assertEquals("Vasia Pupkin",(String) moduleProperties.get("createdBy"));
                
                Map<String, Object> categoryProperties = tableProperties.getPropertiesAppliedForCategory();                
                assertTrue(categoryProperties.size() == 3);
                assertEquals("newLob",(String) categoryProperties.get("lob"));
                assertEquals("alaska",(String) categoryProperties.get("usregion"));                
                assertEquals("east",(String) categoryProperties.get("region"));
                
                Map<String, Object> defaultProperties = tableProperties.getPropertiesAppliedByDefault();                
                assertTrue(defaultProperties.size() == 5);
                assertEquals("US",(String) defaultProperties.get("country"));
                assertEquals("USD",(String) defaultProperties.get("currency"));                
                assertEquals("en",(String) defaultProperties.get("lang"));
                assertTrue((Boolean) defaultProperties.get("active"));
                assertTrue((Boolean) defaultProperties.get("failOnMiss"));
            }        
        }
    }

}
