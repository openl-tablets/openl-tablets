package org.openl.rules.datatype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;

public class DatatypeTableTest extends BaseOpenlBuilderHelper {
    
    private static String src = "test/rules/datatype/DatatypeTableTest.xls";
    
    public DatatypeTableTest() {
        super(src);        
    }
    
    @Test
    public void testCanAccessDatatype() {
        Map<String, IOpenClass> internalTypes = getCompiledOpenClass().getTypes();
        assertTrue("There is Driver datatype", 
            internalTypes.containsKey(StringTool.buildTypeName(ISyntaxConstants.THIS_NAMESPACE, "Driver")));
    }
    
    
    @Test
    public void testDatatypeMember() {
        TableSyntaxNode node = findTable("Datatype Driver");
        if (node != null) {
            assertEquals("Driver", node.getMember().getName());
        } else {
            fail();
        }
        
    }
    
  

}
