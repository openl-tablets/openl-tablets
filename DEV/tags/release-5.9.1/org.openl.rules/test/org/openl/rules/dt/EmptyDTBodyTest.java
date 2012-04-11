package org.openl.rules.dt;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

public class EmptyDTBodyTest extends BaseOpenlBuilderHelper {
    
    private static String src = "test/rules/dt/EmptyDTBody.xls";
    
    public EmptyDTBodyTest() {
        super(src);        
    }
    
    @Test
    public void testEmptyBodyError() {
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            assertTrue(1 == resultTsn.getErrors().length);
            assertTrue(resultTsn.getErrors()[0] instanceof SyntaxNodeException);
            assertTrue(resultTsn.getErrors()[0].getMessage().equals(DecisionTableLoader.EMPTY_BODY));
        } else {
            fail();
        }
    }

}
