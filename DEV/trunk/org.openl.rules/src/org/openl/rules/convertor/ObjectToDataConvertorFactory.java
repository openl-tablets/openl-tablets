package org.openl.rules.convertor;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.openl.binding.IBindingContext;
import org.openl.meta.DoubleValue;
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
            return new HashCodeBuilder().append(from.getName()).append(to.getName()).hashCode();
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

    private static Map<ClassCastPair, IObjectToDataConvertor> cach = new HashMap<ClassCastPair, IObjectToDataConvertor>();
    static {
        try {
            cach.put(new ClassCastPair(Integer.class, IntRange.class), new MatchedConstructorConvertor(IntRange.class
                    .getConstructor(Integer.class)));
            cach.put(new ClassCastPair(Double.class, DoubleValue.class), new MatchedConstructorConvertor(IntRange.class
                    .getConstructor(Double.class)));
        } catch (Exception e) {
        }
    }

    /**
     * @return <code>null</code> if value is not convertable to expected type.
     */
    public static IObjectToDataConvertor getConvertor(Class<?> toClass, Class<?> fromClass) {
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        IObjectToDataConvertor convertor;
        if (!cach.containsKey(pair)) {
            Constructor<?> ctr = ConstructorUtils.getMatchingAccessibleConstructor(toClass, new Class[] { fromClass });
            if (ctr != null) {
                convertor = new MatchedConstructorConvertor(ctr);
            } else {
                convertor = null;
            }
            cach.put(pair, convertor);
        } else {
            convertor = cach.get(pair);
        }
        return convertor;
    }

}
