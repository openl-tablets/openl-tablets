package org.openl.rules.table;

import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.calc.SpreadsheetResult;

public class OpenLArgumentsCloner extends OpenLCloner {

    public OpenLArgumentsCloner() {
        /*
         * to avoid cloning generated at runtime custom SpreadsheetResult children classes
         */
        dontCloneInstanceOf(SpreadsheetResult.class);
    }
}
