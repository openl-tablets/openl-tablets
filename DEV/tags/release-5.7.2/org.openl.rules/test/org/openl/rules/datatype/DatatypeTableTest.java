package org.openl.rules.datatype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;

public class DatatypeTableTest extends BaseOpenlBuilderHelper {
    
    private static String src = "test/rules/datatype/DatatypeTableTest.xls";
    
    public DatatypeTableTest() {
        super(src);        
    }
    
    @Test
    public void testCanAccessDatatype() {
        Map<String, IOpenClass> internalTypes = getJavaWrapper().getCompiledClass().getTypes();
        assertTrue(internalTypes.containsKey("org.openl.this.Driver"));
    }
    
    
    @Test
    public void testDatatypeMember() {
        TableSyntaxNode node = findTable("Datatype Driver", getTableSyntaxNodes());
        if (node != null) {
            assertEquals("Driver", node.getMember().getName());
        } else {
            fail();
        }
        
    }
    
  

}
