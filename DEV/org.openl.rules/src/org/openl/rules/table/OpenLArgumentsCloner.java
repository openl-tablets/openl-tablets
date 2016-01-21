package org.openl.rules.table;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.calc.SpreadsheetResult;

import com.rits.cloning.Cloner;
import com.rits.cloning.IInstantiationStrategy;

/**
 * Extension for {@link Cloner}. To add OpenL classes to prevent cloning
 * instances of them.
 *
 * TODO: should be analyzed variations of tracing different rules. Check if we
 * have issues with mutatation of listed below OpenL not cloned classes.
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
        /*
         * Always cloning them degrades the performance very much. It becomes
         * impossible to open the trace.
         */
        registerImmutable(ByteValue.class);
        registerImmutable(ShortValue.class);
        registerImmutable(IntValue.class);
        registerImmutable(LongValue.class);
        registerImmutable(FloatValue.class);
        registerImmutable(DoubleValue.class);
        registerImmutable(BigIntegerValue.class);
        registerImmutable(BigDecimalValue.class);
        registerImmutable(StringValue.class);
        dontClone(ValueMetaInfo.class);
        /*
         * to avoid cloning generated at runtime custom SpreadsheetResult
         * children classes
         */
        dontCloneInstanceOf(SpreadsheetResult.class);

    }

    /* Required for correct working with classloaders. */
    public static class ObjenesisInstantiationStrategy implements IInstantiationStrategy {
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
