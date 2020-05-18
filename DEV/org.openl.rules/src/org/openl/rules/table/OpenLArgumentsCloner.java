package org.openl.rules.table;

import org.openl.rules.calc.SpreadsheetResult;

public class OpenLArgumentsCloner extends OpenLCloner {

    public OpenLArgumentsCloner() {
        /*
         * to avoid cloning generated at runtime custom SpreadsheetResult children classes
         */
        dontCloneInstanceOf(SpreadsheetResult.class);
    }
}
