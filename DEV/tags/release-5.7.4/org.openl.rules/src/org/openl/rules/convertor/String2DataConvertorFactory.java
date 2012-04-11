/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.convertor;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.openl.meta.DoubleValue;
import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class String2DataConvertorFactory {

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
        convertors.put(BigInteger.class, new String2BigIntegerConvertor());
        convertors.put(BigDecimal.class, new String2BigDecimalConvertor());
    }

    public static synchronized IString2DataConvertor getConvertor(Class<?> clazz) {

        IString2DataConvertor convertor = convertors.get(clazz);

        if (convertor != null) {
            return convertor;
        }

        if (clazz.isEnum()) {
            convertor = new String2EnumConvertor(clazz);
        } else if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            IString2DataConvertor componentConvertor = getConvertor(componentType);
            convertor = new String2ArrayConvertor(componentConvertor);
        } else {
            try {
                Constructor<?> ctr = clazz.getDeclaredConstructor(new Class[] { String.class });
                convertor = new String2ConstructorConvertor(ctr);
            } catch (NoSuchMethodException t) {
                convertor = new NoConvertor(clazz);
            }
        }

        convertors.put(clazz, convertor);

        return convertor;
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
