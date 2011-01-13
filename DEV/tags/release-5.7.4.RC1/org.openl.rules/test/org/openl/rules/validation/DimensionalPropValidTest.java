package org.openl.rules.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DimensionalPropValidTest extends BaseOpenlBuilderHelper {
    
    private static String __src = "test/rules/validation/Dimensional_Prop_Valid.xls";
    
    public DimensionalPropValidTest() {
        super(__src);        
    }
    
    @Test
    public void checkTsnNumber() {
        // number of tsns consider to be 6 tables defined in rule source file.
        // and 2 extra tables generated for dimensional properties for overriden tables. 
        assertTrue(8 == getTableSyntaxNodes().length);
    }
    
}
