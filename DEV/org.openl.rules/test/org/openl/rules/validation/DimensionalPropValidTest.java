package org.openl.rules.validation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DimensionalPropValidTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/Dimensional_Prop_Valid.xls";

    public DimensionalPropValidTest() {
        super(SRC);
    }

    @Test
    public void checkTsnNumber() {
        // number of tsns consider to be 6 tables defined in rule source file.
        // and 2 extra tables generated for dimensional properties for overriden
        // tables.
        assertTrue(6 == getTableSyntaxNodes().length);
    }

}
