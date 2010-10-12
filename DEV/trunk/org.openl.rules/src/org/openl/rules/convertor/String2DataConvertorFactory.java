/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.convertor;

import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.IBindingContext;
import org.openl.meta.DoubleValue;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.utils.exception.ExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 * 
 */
public class String2DataConvertorFactory {

    public static class String2BooleanConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            if (data == null || data.length() == 0) {
                return Boolean.FALSE;
            }            
            
            Boolean boolValue = BooleanUtils.toBooleanObject(data);
            
            if (boolValue != null) {
                return boolValue;
            } else {
                throw new RuntimeException("Invalid boolean value: " + data);
            }            
        }

    }

    static public class String2CalendarConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            return new String2DateConvertor().format(((Calendar) data).getTime(), format);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            return parseCalendar(data, format);
        }

        public Calendar parseCalendar(String data, String format) {

            Date d = new String2DateConvertor().parseDate(data, format);

            Calendar c = Calendar.getInstance(getLocale());

            c.setTime(d);

            return c;

        }

    }

    public static class String2CharConvertor implements IString2DataConvertor {

        public String format(Object data, String forma) {
            return new String(new char[] { ((Character) data).charValue() });
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            if (data.length() != 1) {
                throw new IndexOutOfBoundsException("Character field must have only one symbol");
            }

            return new Character(data.charAt(0));
        }

    }

    public static class String2ClassConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            IOpenClass c = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, data);

            if (c == null) {
                throw new RuntimeException("Type " + data + " is not found");
            }

            return c.getInstanceClass();
        }

    }

    public static class String2ConstructorConvertor implements IString2DataConvertor {

        private Constructor<?> ctr;

        String2ConstructorConvertor(Constructor<?> ctr) {
            this.ctr = ctr;
        }

        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {

            try {
                return ctr.newInstance(new Object[] { data });
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

    }

    static public class String2DateConvertor implements IString2DataConvertor {
        
        private static final Log LOG = LogFactory.getLog(String2DateConvertor.class);
        private static final int YEAR_START_COUNT = 1900;
        private DateFormat defaultFormat = DateFormat.getDateInstance(DateFormat.SHORT, getLocale());

        public String format(Object data, String format) {
            DateFormat df = format == null ? DateFormat.getDateInstance(DateFormat.SHORT)
                    : new SimpleDateFormat(format);
            return df.format(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            return parseDate(data, format);
        }

        public Date parseDate(String data, String format) {
            DateFormat df = format == null ? defaultFormat : new SimpleDateFormat(format, getLocale());

            try {
                return df.parse(data);
            } catch (ParseException e) {
                try {
                    int value = Integer.parseInt(data);
                    Calendar cc = Calendar.getInstance();
                    cc.set(YEAR_START_COUNT, 0, 1);
                    cc.add(Calendar.DATE, value - 1);
                    return cc.getTime();

                } catch (NumberFormatException t) {
                    LOG.debug(t);
                }
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

    }

    public static class String2DoubleConvertor implements IString2DataConvertor {
        
        private static final String DEFAULT_DOUBLE_FORMAT = "#0.00";

        public String format(Object data, String format) {
            if (format == null) {
                format = DEFAULT_DOUBLE_FORMAT;
            }

            DecimalFormat df = new DecimalFormat(format);

            return df.format(((Number) data).doubleValue());
        }

        public Object parse(String xdata, String format, IBindingContext cxt) {

            if (format != null) {
                DecimalFormat df = new DecimalFormat(format);
                try {
                    Number n = df.parse(xdata);

                    return new Double(n.doubleValue());
                } catch (ParseException e) {
                    throw RuntimeExceptionWrapper.wrap("", e);
                }
            }

            String data = numberStringWithoutModifier(xdata);

            double d = Double.parseDouble(data);

            return xdata == data ? new Double(d) : new Double(d * numberModifier(xdata));
        }

    }

    // Initialize

    public static class String2EnumConvertor implements IString2DataConvertor {
        private Class<? extends Enum<?>> enumType;

        @SuppressWarnings("unchecked")
        public String2EnumConvertor(Class<?> clazz) {
            assert clazz.isEnum();
            enumType = (Class<? extends Enum<?>>) clazz;
        }

        public String format(Object data, String format) {
            // Enum can override toString() method to display user-friendly
            // values
            return parse(String.valueOf(data), format, null).toString();
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            Enum<?> resolvedConstant = null;

            for (Enum<?> enumConstant : enumType.getEnumConstants()) {
                if (data.equalsIgnoreCase(enumConstant.name())) {
                    resolvedConstant = enumConstant;
                    break;
                }
            }

            if (resolvedConstant == null) {
                throw new RuntimeException(String.format(
                        "Constant corresponding to value \"%s\" can't be found in Enum %s ", data, enumType.getName()));
            }

            return resolvedConstant;
        }
    }

    public static class String2IntConvertor implements IString2DataConvertor {
        
        public String format(Object data, String format) {
            if (format == null) {
                return String.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);
            return df.format(((Integer) data).intValue());
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            if (format == null) {
                try {
                    return Integer.valueOf(data);
                } catch (NumberFormatException e) {
                    ExceptionUtils.processNumberFormatException(e);
                }
            }
            DecimalFormat df = new DecimalFormat(format);

            Number n;
            try {
                n = df.parse(data);
            } catch (ParseException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }

            return Integer.valueOf(n.intValue());
        }

    }

    public static class String2LongConvertor implements IString2DataConvertor {
        
        public String format(Object data, String format) {
            if (format == null) {
                return String.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);
            return df.format(((Long) data).intValue());
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            if (format == null) {
                return Long.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);

            Number n;
            try {
                n = df.parse(data);
            } catch (ParseException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }

            return Long.valueOf(n.longValue());
        }

    }

    public static class String2ByteConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            if (format == null) {
                return String.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);
            return df.format(((Byte) data).byteValue());
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            if (format == null) {
                return Byte.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);

            Number n;
            try {
                n = df.parse(data);
            } catch (ParseException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }

            return Byte.valueOf(n.byteValue());
        }
    }

    public static class String2ShortConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            if (format == null) {
                return String.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);
            return df.format(((Short) data).shortValue());
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            if (format == null) {
                return Short.valueOf(data);
            }
            DecimalFormat df = new DecimalFormat(format);

            Number n;
            try {
                n = df.parse(data);
            } catch (ParseException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }

            return Short.valueOf(n.shortValue());
        }
    }
    
    public static class String2FloatConvertor implements IString2DataConvertor {

        private static final String DEFAULT_FLOAT_FORMAT = "#0.00";

        public String format(Object data, String format) {
            if (format == null) {
                format = DEFAULT_FLOAT_FORMAT;
            }

            DecimalFormat df = new DecimalFormat(format);

            return df.format(((Float) data).floatValue());
        }
        
        public Object parse(String xdata, String format, IBindingContext cxt) {

            if (format != null) {
                DecimalFormat df = new DecimalFormat(format);
                try {
                    Number n = df.parse(xdata);

                    return new Float(n.floatValue());
                } catch (ParseException e) {
                    throw RuntimeExceptionWrapper.wrap("", e);
                }
            }

            String data = numberStringWithoutModifier(xdata);

            float floatValue = Float.parseFloat(data);

            return xdata == data ? Float.valueOf(floatValue) : Float.valueOf((float) (floatValue * numberModifier(xdata)));
        }
    }

    public static class String2OpenClassConvertor implements IString2DataConvertor {
        
        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            
            if (data.endsWith("[]")) {
                
                String baseCode = data.substring(0, data.length() - 2);
                IOpenClass baseType = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, baseCode);
                
                if (baseType == null) {
                    return null;
                }
                
                return baseType.getAggregateInfo().getIndexedAggregateType(baseType, 1);
            }

            
            IOpenClass c = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, data);

            if (c == null) {
                throw new RuntimeException("Type " + data + " is not found");
            }

            return c;
        }

    }

    public static class String2IntRangeConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            return new IntRange(data);
        }

    }
    
    public static class String2DoubleValueConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            return new DoubleValue(data);
        }

    }
    
    public static class String2StringConvertor implements IString2DataConvertor {

        public String format(Object data, String format) {
            return String.valueOf(data);
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            return data;
        }

    }

// not used?    
//    public class String2ArrayConvertor implements IString2DataConvertor {
//
//        private Object type;
//        
//        public String format(Object data, String format) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        public Object parse(String data, String format, IBindingContext cxt) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//        
//    }
    
    public static class NoConvertor implements IString2DataConvertor {

        private Class<?> clazz;
        
        public NoConvertor(Class<?> clazz) {
            super();
            this.clazz = clazz;
        }

        public String format(Object data, String format) {
            throw new RuntimeException("Should not call this method");
        }

        public Object parse(String data, String format, IBindingContext cxt) {
            // FIXME: Wrong exception type. The error about not existing
            // converter must be thrown in corresponding factory. Throwing error
            // from looks like ugly design.
          throw new IllegalArgumentException("Convertor or Public Constructor " + clazz.getName()
             + "(String s) does not exist");    
        }

    }

    private static Locale locale = null;

    private static final String LOCALE_COUNTRY = "US";
    private static final String LOCALE_LANG = "en";

    private static HashMap<Class<?>, IString2DataConvertor> convertors;

    static {
        convertors = new HashMap<Class<?>, IString2DataConvertor>();

        convertors.put(int.class, new String2IntConvertor());
        convertors.put(double.class, new String2DoubleConvertor());
        convertors.put(char.class, new String2CharConvertor());
        convertors.put(boolean.class, new String2BooleanConvertor());
        convertors.put(long.class, new String2LongConvertor());
        convertors.put(byte.class, new String2ByteConvertor());
        convertors.put(short.class, new String2ShortConvertor());
        convertors.put(float.class, new String2FloatConvertor());    

        convertors.put(Integer.class, new String2IntConvertor());
        convertors.put(Byte.class, new String2ByteConvertor());        
        convertors.put(Short.class, new String2ShortConvertor());        
        convertors.put(Float.class, new String2FloatConvertor());            
        convertors.put(Double.class, new String2DoubleConvertor());
        convertors.put(Character.class, new String2CharConvertor());
        convertors.put(Boolean.class, new String2BooleanConvertor());
        convertors.put(Long.class, new String2LongConvertor());

        convertors.put(String.class, new String2StringConvertor());
        convertors.put(Date.class, new String2DateConvertor());
        convertors.put(Calendar.class, new String2CalendarConvertor());
        convertors.put(Class.class, new String2ClassConvertor());
        convertors.put(IOpenClass.class, new String2OpenClassConvertor());        
        convertors.put(DoubleValue.class, new String2DoubleValueConvertor());        
        convertors.put(IntRange.class, new String2IntRangeConvertor());        
    }

    public static synchronized IString2DataConvertor getConvertor(Class<?> clazz) {
        IString2DataConvertor convertor = convertors.get(clazz);
        if (convertor != null)
            return convertor;
        
            if (clazz.isEnum()) {
                convertor = new String2EnumConvertor(clazz);
            } else  if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                IString2DataConvertor componentConvertor = getConvertor(componentType);
                convertor = new String2ArrayConvertor(componentConvertor);
            }else {
                try {
                    Constructor<?> ctr = clazz.getDeclaredConstructor(new Class[] { String.class });
                    convertor = new String2ConstructorConvertor(ctr);
                } catch (NoSuchMethodException t) {
                    convertor = new NoConvertor(clazz);
                    // throw new IllegalArgumentException("Convertor or Public
                    // Constructor " + clazz.getName()
                    // + "(String s) does not exist");
                }
            }
            
        convertors.put(clazz, convertor);    
        return convertor;

    }

    static public Locale getLocale() {
        if (locale == null) {
            String country = System.getProperty("org.openl.locale.country");
            String lang = System.getProperty("org.openl.locale.lang");

            locale = new Locale(lang == null ? LOCALE_LANG : lang, country == null ? LOCALE_COUNTRY : country);
        }

        return locale;
    }

    static double numberModifier(String s) {
        if (s.endsWith("%")) {
            return 0.01;
        }

        return 1;
    }

    static String numberStringWithoutModifier(String s) {
        if (s.endsWith("%")) {
            return s.substring(0, s.length() - 1);
        }

        return s;
    }

    public static void registerConvertor(Class<?> clazz, IString2DataConvertor conv) {
        convertors.put(clazz, conv);
    }
    
    /**
     * Removes the specified Class from convertors cache.
     * 
     * @param clazz Class to unregister.
     */
    public static void unregisterConvertorForClass(Class<?> clazz) {
        convertors.remove(clazz);
    }

    /**
     * Unregister all Classes from the specified class loader.
     * 
     * @param classLoader ClassLoader to unregister.
     */
    public static void unregisterClassLoader(ClassLoader classLoader) {
        List<Class<?>> toRemove = new ArrayList<Class<?>>();
        for (Class<?> clazz : convertors.keySet()) {
            if (clazz.getClassLoader() == classLoader) {
                toRemove.add(clazz);
            }
        }
        for (Class<?> clazz : toRemove) {
            unregisterConvertorForClass(clazz);
        }
    }
}
