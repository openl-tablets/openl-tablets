package org.openl.rules.convertor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.openl.binding.IBindingContext;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.rules.helpers.IntRange;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Gives convertors from one class to another.
 * 
 * @author PUdalau
 */
public class ObjectToDataConvertorFactory {
    public static class ClassCastPair {
        private Class<?> from;
        private Class<?> to;

        public ClassCastPair(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        public Class<?> getFrom() {
            return from;
        }

        public Class<?> getTo() {
            return to;
        }

        @Override
        public int hashCode() {
            return to.hashCode() + from.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassCastPair)) {
                return false;
            }
            ClassCastPair pair = (ClassCastPair) obj;
            return from == pair.from && to == pair.to;
        }
    }

    public static class MatchedConstructorConvertor implements IObjectToDataConvertor {
        private Constructor<?> ctr;

        public MatchedConstructorConvertor(Constructor<?> ctr) {
            this.ctr = ctr;
        }

        public Object convert(Object data, IBindingContext bindingContext) {
            try {
                return ctr.newInstance(new Object[] { data });
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
    }

    /**
     * Contains static method as a private field for constructing new objects of
     * appropriate type.
     * 
     * @author DLiauchuk
     *
     */
    public static class StaticMethodConvertor implements IObjectToDataConvertor {
        private Method staticMethod;

        public StaticMethodConvertor(Method staticMethod) {
            if (!Modifier.isStatic(staticMethod.getModifiers())) {
                throw new IllegalArgumentException("Income method should be static");
            }
            this.staticMethod = staticMethod;
        }

        public Object convert(Object data, IBindingContext bindingContext) {
            try {
                // first argument is null as field staticMethod represents only
                // static method.
                //
                return staticMethod.invoke(null, data);
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
    }

    public static class CopyConvertor implements IObjectToDataConvertor {
        public Object convert(Object data, IBindingContext bindingContext) {
            return data;
        }

        static public CopyConvertor the = new CopyConvertor();
    }

    /**
     * Convertor that looks for the instance method 'getValue' for conversion to
     * the appropriate type.
     * 
     * @author DLiauchuk
     *
     */
    public static class GetValueConvertor implements IObjectToDataConvertor {
        public Object convert(Object data, IBindingContext bindingContext) {
            if (data != null) {
                Method getValueMethod = null;
                try {
                    getValueMethod = data.getClass().getMethod("getValue", new Class<?>[0]);
                } catch (Exception e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
                Object value = null;
                try {
                    value = getValueMethod.invoke(data, new Object[0]);
                } catch (Exception e) {
                    throw RuntimeExceptionWrapper.wrap(e);
                }
                return value;
            }
            return data;
        }
    }

    private static Map<ClassCastPair, IObjectToDataConvertor> convertors = new HashMap<ClassCastPair, IObjectToDataConvertor>();
    private static ReadWriteLock convertorsLock = new ReentrantReadWriteLock();

    static {
        Lock writeLock = convertorsLock.writeLock();
        try {
            writeLock.lock();
            convertors.put(new ClassCastPair(Integer.class, IntRange.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    return new IntRange((Integer) data);
                }

            });

            convertors.put(new ClassCastPair(int.class, IntRange.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    return new IntRange((Integer) data);
                }

            });

            convertors.put(new ClassCastPair(Double.class, DoubleValue.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    return new DoubleValue((Double) data);
                }

            });
            convertors.put(new ClassCastPair(Double.class, double.class), CopyConvertor.the);
            convertors.put(new ClassCastPair(double.class, Double.class), CopyConvertor.the);
            convertors.put(new ClassCastPair(int.class, Integer.class), CopyConvertor.the);
            convertors.put(new ClassCastPair(Integer.class, int.class), CopyConvertor.the);

            convertors.put(new ClassCastPair(double.class, DoubleValue.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    return new DoubleValue((Double) data);
                }
            });

            convertors.put(new ClassCastPair(Date.class, Calendar.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    Calendar cal = Calendar.getInstance(LocaleDependConvertor.getLocale());
                    cal.setTime((Date) data);
                    return cal;
                }

            });

            convertors.put(new ClassCastPair(Date.class, Calendar.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    Calendar cal = Calendar.getInstance(LocaleDependConvertor.getLocale());
                    cal.setTime((Date) data);
                    return cal;
                }

            });

            /**
             * convertors from Openl types with meta info to common java types
             */
            convertors.put(new ClassCastPair(ByteValue.class, Byte.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(ShortValue.class, Short.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(IntValue.class, Integer.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(LongValue.class, Long.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(FloatValue.class, Float.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(DoubleValue.class, Double.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(BigIntegerValue.class, BigInteger.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(BigDecimalValue.class, BigDecimal.class), new GetValueConvertor());
            convertors.put(new ClassCastPair(org.openl.meta.StringValue.class, String.class), new GetValueConvertor());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }

    public static final IObjectToDataConvertor NO_Convertor = new IObjectToDataConvertor() {

        public Object convert(Object data, IBindingContext bindingContext) {
            throw new UnsupportedOperationException();
        }

    };

    /**
     * @return NO_Convertor if value is not convertable to expected type.
     */
    public static IObjectToDataConvertor getConvertor(Class<?> toClass, Class<?> fromClass) {

        if (toClass == fromClass)
            return CopyConvertor.the;
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        IObjectToDataConvertor convertor = NO_Convertor;

        Lock readLock = convertorsLock.readLock();
        try {
            readLock.lock();
            if (convertors.containsKey(pair)) {
                return convertors.get(pair);
            }
        } finally {
            readLock.unlock();
        }

        Method method = MethodUtils.getAccessibleMethod(toClass, "valueOf", fromClass);
        if (method != null) {
            convertor = new StaticMethodConvertor(method);
        } else {
            // try to find appropriate constructor.
            //
            Constructor<?> ctr = ConstructorUtils.getMatchingAccessibleConstructor(toClass, new Class[] { fromClass });

            if (ctr != null) {
                convertor = new MatchedConstructorConvertor(ctr);
            } else {
                convertor = NO_Convertor;
            }
        }
        Lock writeLock = convertorsLock.writeLock();
        try {
            writeLock.lock();
            convertors.put(pair, convertor);
        } finally {
            writeLock.unlock();
        }
        return convertor;
    }

    public static IObjectToDataConvertor registerConvertor(Class<?> toClass,
            Class<?> fromClass,
            IObjectToDataConvertor convertor) {
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        Lock writeLock = convertorsLock.writeLock();
        try {
            writeLock.lock();
            return convertors.put(pair, convertor);
        } finally {
            writeLock.unlock();
        }
    }

}
