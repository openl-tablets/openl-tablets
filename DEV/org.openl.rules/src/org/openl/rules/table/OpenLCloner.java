package org.openl.rules.table;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.types.TransientFieldsValues;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.slf4j.Logger;

import com.rits.cloning.Cloner;
import com.rits.cloning.IInstantiationStrategy;

/**
 * Extension for {@link Cloner}. To add OpenL classes to prevent cloning instances of them.
 *
 * TODO: should be analyzed variations of tracing different rules. Check if we have issues with mutatation of listed
 * below OpenL not cloned classes.
 *
 * @author DLiauchuk
 *
 */
public class OpenLCloner extends Cloner {

    private static final TransientFieldsValuesHolder TRANSIENT_FIELDS_VALUES = new TransientFieldsValuesHolder();

    private static class TransientFieldsValuesHolder {
        private final ReferenceQueue<TransientFieldsValues> queue = new ReferenceQueue<>();
        private final Set<Reference<TransientFieldsValues>> values = new HashSet<>();
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        private void registerTransientFieldsValues(TransientFieldsValues transientFieldsValues) {
            Lock writeLock = readWriteLock.writeLock();
            try {
                writeLock.lock();
                Reference<? extends TransientFieldsValues> zombie = queue.poll();
                while (zombie != null) {
                    values.remove(zombie);
                    zombie = queue.poll();
                }
                WeakReference<TransientFieldsValues> weakReference = new WeakReference<>(transientFieldsValues, queue);
                values.add(weakReference);
            } finally {
                writeLock.unlock();
            }
        }

        private void clone(Object o, Object t, OpenLCloner cloner) {
            Lock readLock = readWriteLock.readLock();
            try {
                readLock.lock();
                for (Reference<TransientFieldsValues> transientFieldsValuesReference : values) {
                    TransientFieldsValues transientFieldsValues = transientFieldsValuesReference.get();
                    if (transientFieldsValues != null && transientFieldsValues.hasValue(o)) {
                        Object v = transientFieldsValues.getValue(o);
                        transientFieldsValues.setValue(t, cloner.deepClone(v));
                    }
                }
            } finally {
                readLock.unlock();
            }
        }
    }

    public static void registerTransientFieldsValues(TransientFieldsValues transientFieldsValues) {
        TRANSIENT_FIELDS_VALUES.registerTransientFieldsValues(transientFieldsValues);
    }

    public OpenLCloner() {
        super(new ObjenesisInstantiationStrategy());
        dontCloneClasses();
    }

    private void dontCloneClasses() {
        /*
         * Always cloning them degrades the performance very much. It becomes impossible to open the trace.
         */
        registerImmutable(ByteValue.class);
        registerImmutable(ShortValue.class);
        registerImmutable(IntValue.class);
        registerImmutable(LongValue.class);
        registerImmutable(FloatValue.class);
        // registerImmutable(DoubleValue.class); EPBDS-6604 DoubleValue is not immutable
        registerImmutable(BigIntegerValue.class);
        registerImmutable(BigDecimalValue.class);
        registerImmutable(StringValue.class);
        dontClone(ValueMetaInfo.class);
        /*
         * to avoid cloning generated at runtime custom SpreadsheetResult children classes
         */
        dontCloneInstanceOf(SpreadsheetResult.class);
        dontCloneInstanceOf(Logger.class);
        dontCloneInstanceOf(IOpenClass.class);
        dontCloneInstanceOf(IOpenMember.class);
        dontCloneInstanceOf(InvocationHandler.class);
        dontCloneInstanceOf(TransientFieldsValues.NullValue.class);
    }

    @Override
    public <T> T cloneInternal(T o, Map<Object, Object> clones) throws IllegalAccessException {
        T t = super.cloneInternal(o, clones);
        TRANSIENT_FIELDS_VALUES.clone(o, t, this);
        return t;
    }

    /* Required for correct working with classloaders. */
    public static class ObjenesisInstantiationStrategy implements IInstantiationStrategy {
        private final Objenesis objenesis = new ObjenesisStd();

        @Override
        public <T> T newInstance(Class<T> c) {
            return objenesis.newInstance(c);
        }

        private static ObjenesisInstantiationStrategy instance = new ObjenesisInstantiationStrategy();

        public static ObjenesisInstantiationStrategy getInstance() {
            return instance;
        }
    }

}
