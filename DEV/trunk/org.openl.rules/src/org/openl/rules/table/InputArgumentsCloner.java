package org.openl.rules.table;

import java.util.Map;

import org.objenesis.Objenesis;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.result.SpreadsheetResultHelper;

import com.rits.cloning.Cloner;

/**
 * Extension for {@link Cloner}. To add OpenL classes
 * to prevent cloning instances of them.
 * 
 * TODO: should be analyzed variations of tracing different rules. Check if we have issues with mutatation
 * of listed below OpenL not cloned classes.
 * 
 * @author DLiauchuk
 *
 */
public class InputArgumentsCloner extends Cloner {
    
    public InputArgumentsCloner() {
        super();
        dontCloneClasses();
    }
    
    private void dontCloneClasses() {
        // Register them as dont clone as in 90% of cases instances of them are not modified in rules.
        // But always cloning them degrades the performance very much. It becomes impossible to open the trace.
        //
        dontClone(SpreadsheetResult.class);
        dontClone(ByteValue.class);
        dontClone(ShortValue.class);
        dontClone(IntValue.class);
        dontClone(LongValue.class);
        dontClone(FloatValue.class);
        dontClone(DoubleValue.class);
        dontClone(BigIntegerValue.class);
        dontClone(BigDecimalValue.class);
        dontClone(StringValue.class);
    }

    public InputArgumentsCloner(final Objenesis objenesis) {
        super(objenesis);
        dontCloneClasses();
    }
    
    //Overriden to avoid cloning generated at runtime custom SpreadsheetResult children classes
	@Override
	public <T> T cloneInternal(final T o, final Map<Object, Object> clones) throws IllegalAccessException {
	    if (o == null) return null;
	    if (SpreadsheetResultHelper.isSpreadsheetResult(o.getClass())) {
	        dontClone(o.getClass());
	    }
	    return super.cloneInternal(o, clones);
	}
}
