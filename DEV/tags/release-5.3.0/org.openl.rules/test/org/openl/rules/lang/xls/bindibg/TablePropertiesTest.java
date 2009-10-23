package org.openl.rules.lang.xls.bindibg;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;


public class TablePropertiesTest {

    private String __src = "test/rules/Tutorial_4_Test.xls";
    
    private XlsModuleSyntaxNode getTables() {        
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, __src);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }
    
    @Test
    public void testPropertyDef() {
        TableSyntaxNode[] tsns = getTables().getXlsTableSyntaxNodes(); 
        assertTrue(61 == tsns.length);        
        assertEquals("Driver Age Type Table", tsns[4].getTableProperties().getProperty("name").getValue().getValue());
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        assertEquals("02/04/2237", sdf.format(((Date)tsns[4].getTableProperties()
                .getProperty("effectiveDate").getValue().getValue())));
    }
    
    @Test
    public void testGetValueAsString() {
        String result = null;
        String propName = "name";
        String strValue = "MyName";
        Property[] prop = new Property[]{new Property(new StringValue(propName), new ObjectValue(strValue))};
        TableProperties tablProp = new TableProperties(null, prop);
        result = tablProp.getPropertyValueAsString(propName);
        assertEquals(strValue, result);
        
        propName = "effectiveDate";
        Date dateValue = new Date(4098); 
        prop = new Property[]{new Property(new StringValue(propName), new ObjectValue(dateValue))};
        tablProp = new TableProperties(null, prop);
        result = tablProp.getPropertyValueAsString(propName);
        SimpleDateFormat sDF = new SimpleDateFormat(DefaultPropertyDefinitions.getPropertyByName(propName).getFormat());
        assertEquals(sDF.format(dateValue), result);
        
        propName = "isReady";
        Boolean boolValue = new Boolean(true); 
        prop = new Property[]{new Property(new StringValue(propName), new ObjectValue(boolValue))};
        tablProp = new TableProperties(null, prop);
        result = tablProp.getPropertyValueAsString(propName);        
        assertEquals("true", result);
        
        propName = "age";
        Integer intValue = new Integer(37); 
        prop = new Property[]{new Property(new StringValue(propName), new ObjectValue(intValue))};
        tablProp = new TableProperties(null, prop);
        result = tablProp.getPropertyValueAsString(propName);        
        assertEquals("37", result);
        String unexistingName = "noSuchName";
        result = tablProp.getPropertyValueAsString(unexistingName);        
        assertNull(result);
    }
}
