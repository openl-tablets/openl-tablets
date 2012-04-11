package org.openl.rules.convertor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.reflect.MethodUtils;
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
     * Contains static method as a private field for constructing new objects of appropriate type.
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
                // first argument is null as field staticMethod represents only static method.
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

    }

    private static Map<ClassCastPair, IObjectToDataConvertor> convertors = new HashMap<ClassCastPair, IObjectToDataConvertor>();
    static {
        try {
            convertors.put(new ClassCastPair(Integer.class, IntRange.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    return new IntRange((Integer) data);
                }

            });

            convertors.put(new ClassCastPair(Double.class, DoubleValue.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    return new DoubleValue((Double) data);
                }

            });
            convertors.put(new ClassCastPair(Double.class, Double.class), new CopyConvertor());
            convertors.put(new ClassCastPair(Double.class, double.class), new CopyConvertor());
            convertors.put(new ClassCastPair(Integer.class, int.class), new CopyConvertor());
            convertors.put(new ClassCastPair(String.class, String.class), new CopyConvertor());
            convertors.put(new ClassCastPair(Date.class, Date.class), new CopyConvertor());
            convertors.put(new ClassCastPair(Date.class, Calendar.class), new IObjectToDataConvertor() {

                public Object convert(Object data, IBindingContext bindingContext) {
                    Calendar cal = Calendar.getInstance(LocaleDependConvertor.getLocale());
                    cal.setTime((Date) data);
                    return cal;
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
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
    public static synchronized IObjectToDataConvertor getConvertor(Class<?> toClass, Class<?> fromClass) {
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        IObjectToDataConvertor convertor = NO_Convertor;
        if (!convertors.containsKey(pair)) {
            // at first try to find static initialization method, for some numeric classes(e.g. Integer, Double, etc)
            // there are predefined cached values(see Integer.valueOf(int a)).
            //
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
            convertors.put(pair, convertor);
        } else {
            convertor = convertors.get(pair);
        }
        return convertor;
    }

    public static IObjectToDataConvertor registerConvertor(Class<?> toClass, Class<?> fromClass,
            IObjectToDataConvertor convertor) {
        ClassCastPair pair = new ClassCastPair(fromClass, toClass);
        return convertors.put(pair, convertor);
    }

}
