package org.openl.rules.table;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.calc.SpreadsheetResult;

import com.rits.cloning.Cloner;
import com.rits.cloning.IInstantiationStrategy;

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
public class OpenLArgumentsCloner extends Cloner {

    public OpenLArgumentsCloner() {
        super(new ObjenesisInstantiationStrategy());
        dontCloneClasses();
    }

    private void dontCloneClasses() {
        // Register them as dont clone as in 90% of cases instances of them are not modified in rules.
        // But always cloning them degrades the performance very much. It becomes impossible to open the trace.
        //
        /*dontClone(SpreadsheetResult.class);
        dontClone(ByteValue.class);
        dontClone(ShortValue.class);
        dontClone(IntValue.class);
        dontClone(LongValue.class);
        dontClone(FloatValue.class);
        dontClone(DoubleValue.class);
        dontClone(BigIntegerValue.class);
        dontClone(BigDecimalValue.class);
        dontClone(StringValue.class);*/
        dontClone(ValueMetaInfo.class);
        //to avoid cloning generated at runtime custom SpreadsheetResult children classes
        dontCloneInstanceOf(SpreadsheetResult.class);

    }

    public static class ObjenesisInstantiationStrategy implements IInstantiationStrategy { // Required for correct working with classloaders.
        private final Objenesis objenesis = new ObjenesisStd();

        @SuppressWarnings("unchecked")
        public <T> T newInstance(Class<T> c) {
            return (T) objenesis.newInstance(c);
        }

        private static ObjenesisInstantiationStrategy instance = new ObjenesisInstantiationStrategy();

        public static ObjenesisInstantiationStrategy getInstance() {
            return instance;
        }
    }

}
