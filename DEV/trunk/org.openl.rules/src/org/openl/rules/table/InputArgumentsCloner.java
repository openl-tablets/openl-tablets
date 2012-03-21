package org.openl.rules.table;

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

import com.rits.cloning.Cloner;

/**
 * Extension for {@link Cloner}. To add OpenL conditionally immutable classes
 * to prevent cloning instances of them.
 * 
 * TODO: should be analyzed variations of tracing different rules. Check if we have issues with mutatation
 * of listed below OpenL conditionally immutable classes.
 * 
 * @author DLiauchuk
 *
 */
public class InputArgumentsCloner extends Cloner {
	@Override
	protected void registerKnownJdkImmutableClasses() {		
		super.registerKnownJdkImmutableClasses();
		// register conditionally immutable OpenL classes
		registerConditionallyOpenLImmutableClasess();
	}
	
	private void registerConditionallyOpenLImmutableClasess() {
		// though by implementation this classes are mutable,
		// register them as immutable as in 90% of cases instances of them are not modified in rules.
		// But always cloning them degrades the performance very much. It becomes impossible to open the trace.
		//
		registerImmutable(SpreadsheetResult.class);
		registerImmutable(ByteValue.class);
		registerImmutable(ShortValue.class);
		registerImmutable(IntValue.class);
		registerImmutable(LongValue.class);
		registerImmutable(FloatValue.class);
		registerImmutable(DoubleValue.class);
		registerImmutable(BigIntegerValue.class);
		registerImmutable(BigDecimalValue.class);
		registerImmutable(StringValue.class);
	}
}
