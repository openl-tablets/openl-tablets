package org.openl.gen.writers;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.openl.gen.ByteCodeGenerationException;
import org.openl.gen.FieldDescription;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class DefaultConstructorWriter extends DefaultBeanByteCodeWriter {

    private static final Method STR_CONSTR = Method.getMethod("void <init> (java.lang.String)");
    private static final Class<?>[] STR_CONSTR_PARAMS = {String.class};
    private static final Method DEF_CONSTR = Method.getMethod("void <init> ()");
    private static final Class<?>[] DEF_CONSTR_PARAMS = {};

    private static final Map<String, Class<?>> DEFAULT_COLLECTIONS_INTERFACES;
    private static final Map<String, Class<?>> boxed = new HashMap<>(8);
    private static final Method ZONE_ID_OF = Method.getMethod("java.time.ZoneId of(java.lang.String)");
    private static final Method ZONED_DATETIME_OF = Method
            .getMethod("java.time.ZonedDateTime of(int, int, int, int, int, int, int, java.time.ZoneId)");
    private static final Method INSTANT_OF = Method
            .getMethod("java.time.Instant ofEpochMilli(long)");
    private static final Method LOCAL_DATE_OF = Method.getMethod("java.time.LocalDate of(int, int, int)");
    private static final Method LOCAL_TIME_OF = Method.getMethod("java.time.LocalTime of(int, int, int)");
    private static final Method LOCAL_DATETIME_OF = Method
            .getMethod("java.time.LocalDateTime of(int, int, int, int, int, int)");

    static {
        boxed.put(Byte.class.getName(), byte.class);
        boxed.put(Short.class.getName(), short.class);
        boxed.put(Integer.class.getName(), int.class);
        boxed.put(Boolean.class.getName(), boolean.class);
        boxed.put(Character.class.getName(), char.class);
        boxed.put(Long.class.getName(), long.class);
        boxed.put(Float.class.getName(), float.class);
        boxed.put(Double.class.getName(), double.class);

        Map<String, Class<?>> defaultInterfaceCollections = new HashMap<>(6);
        defaultInterfaceCollections.put(Collection.class.getName(), ArrayList.class);
        defaultInterfaceCollections.put(List.class.getName(), ArrayList.class);
        defaultInterfaceCollections.put(Set.class.getName(), HashSet.class);
        defaultInterfaceCollections.put(SortedSet.class.getName(), TreeSet.class);
        defaultInterfaceCollections.put(Map.class.getName(), HashMap.class);
        defaultInterfaceCollections.put(SortedMap.class.getName(), TreeMap.class);

        DEFAULT_COLLECTIONS_INTERFACES = Collections.unmodifiableMap(defaultInterfaceCollections);
    }

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *                            (e.g. <code>my/test/TestClass</code>)
     * @param parentClass         class descriptor for super class.
     * @param beanFields          fields of generating class.
     */
    public DefaultConstructorWriter(String beanNameWithPackage,
                                    Class<?> parentClass,
                                    Map<String, FieldDescription> beanFields) {
        super(beanNameWithPackage, parentClass, beanFields);
    }

    private static void pushValue(GeneratorAdapter mg, Type type, Object value) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                mg.push((Boolean) value);
                break;
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mg.push(((Number) value).intValue());
                break;
            case Type.CHAR:
                mg.push((Character) value);
                break;
            case Type.DOUBLE:
                mg.push((Double) value);
                break;
            case Type.FLOAT:
                mg.push((Float) value);
                break;
            case Type.LONG:
                mg.push((Long) value);
                break;
            case Type.ARRAY:
                pushArray(mg, type, value);
                break;
            case Type.OBJECT:
                pushObject(mg, type, value);
                break;
            default:
                throw new IllegalStateException("Unsupported type");
        }
    }

    private static void pushArray(GeneratorAdapter mg, Type type, Object value) {
        int dimensions = type.getDimensions();
        if (DefaultValue.DEFAULT.equals(value)) {
            if (dimensions == 1) {
                // new SomeArray[0]
                mg.visitInsn(Opcodes.ICONST_0);
                Type elementType = type.getElementType();
                mg.newArray(elementType);
            } else {
                // new SomeArray[0][0]
                for (int i = 0; i < dimensions; i++) {
                    mg.visitInsn(Opcodes.ICONST_0);
                }
                mg.visitMultiANewArrayInsn(type.getDescriptor(), dimensions);
            }
        } else {
            int length = Array.getLength(value);
            mg.push(length);
            Type elementType = Type.getType(type.getDescriptor().substring(1));
            mg.newArray(elementType);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                mg.dup();// ar
                mg.push(i); // index
                pushValue(mg, elementType, element);
                mg.arrayStore(elementType); // ar[i]=value;
            }
        }
    }

    private static void pushObject(GeneratorAdapter mg, Type type, Object value) {
        String className = type.getClassName();

        if (DefaultValue.DEFAULT.equals(value)) {
            pushDefault(mg, type, className);
        } else if (className.equals(String.class.getName())) {
            mg.push((String) value);
        } else if (className.equals(Date.class.getName())) {
            pushDate(mg, type, (Date) value);
        } else if (className.equals(LocalDate.class.getName())) {
            pushLocalDate(mg, type, (LocalDate) value);
        } else if (className.equals(ZonedDateTime.class.getName())) {
            pushZonedDateTime(mg, type, (ZonedDateTime) value);
        } else if (className.equals(Instant.class.getName())) {
            pushInstant(mg, type, (Instant) value);
        } else if (className.equals(LocalDateTime.class.getName())) {
            pushLocalDateTime(mg, type, (LocalDateTime) value);
        } else if (className.equals(LocalTime.class.getName())) {
            pushLocalTime(mg, type, (LocalTime) value);
        } else if (boxed.containsKey(className)) {
            pushBoxed(mg, value, className);
        } else if (value.getClass().isEnum()) {
            pushEnum(mg, value);
        } else {
            pushString(mg, type, value);
        }
    }

    private static void pushString(GeneratorAdapter mg, Type type, Object value) {
        // new SomeType("value")
        validateConstructor(type.getClassName(), STR_CONSTR_PARAMS);
        mg.newInstance(type);
        mg.dup();
        mg.push(String.valueOf(value));
        mg.invokeConstructor(type, STR_CONSTR);
    }

    private static void pushEnum(GeneratorAdapter mg, Object value) {
        // SomeEnum.NAME
        Class<?> enumClass = value.getClass();
        Type enumType = Type.getType(enumClass);
        mg.getStatic(enumType, ((Enum) value).name(), enumType);
    }

    private static void pushBoxed(GeneratorAdapter mg, Object value, String className) {
        // Boxed.valueOf(value)
        Class<?> prim = boxed.get(className);
        Type primType = Type.getType(prim);
        pushValue(mg, primType, value);
        mg.valueOf(primType);
    }

    private static void pushDefault(GeneratorAdapter mg, Type type, String className) {
        if (DEFAULT_COLLECTIONS_INTERFACES.containsKey(className)) {
            // Collection, Map, SortedMap, List, Set
            Class<?> defaultImpl = DEFAULT_COLLECTIONS_INTERFACES.get(className);
            Type defaultImplType = Type.getType(defaultImpl);
            mg.newInstance(defaultImplType);
            mg.dup();
            mg.invokeConstructor(defaultImplType, DEF_CONSTR);
        } else {
            // new SomeType()
            validateConstructor(type.getClassName(), DEF_CONSTR_PARAMS);
            mg.newInstance(type);
            mg.dup();
            mg.invokeConstructor(type, DEF_CONSTR);
        }
    }

    private static void pushDate(GeneratorAdapter mg, Type type, Date value) {
        // new Date("07/12/2017 12:00:00 AM")
        mg.newInstance(type);
        mg.dup();
        // SimpleDateFormat is thread-unsafe
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);
        mg.push(simpleDateFormat.format(value));
        mg.invokeConstructor(type, STR_CONSTR);
    }

    private static void pushLocalDate(GeneratorAdapter mg, Type type, LocalDate value) {
        LocalDate ld = value;
        mg.push(ld.getYear());
        mg.push(ld.getMonthValue());
        mg.push(ld.getDayOfMonth());
        mg.invokeStatic(type, LOCAL_DATE_OF);
    }

    private static void pushZonedDateTime(GeneratorAdapter mg, Type type, ZonedDateTime value) {
        ZonedDateTime zdt = value;
        mg.push(zdt.getYear());
        mg.push(zdt.getMonthValue());
        mg.push(zdt.getDayOfMonth());
        mg.push(zdt.getHour());
        mg.push(zdt.getMinute());
        mg.push(zdt.getSecond());
        mg.push(zdt.getNano());
        mg.push(zdt.getZone().getId());
        mg.invokeStatic(Type.getType(ZoneId.class), ZONE_ID_OF);
        mg.invokeStatic(type, ZONED_DATETIME_OF);
    }

    private static void pushInstant(GeneratorAdapter mg, Type type, Instant value) {
        Instant instantDate = value;
        mg.push(instantDate.toEpochMilli());
        mg.invokeStatic(type, INSTANT_OF);
    }

    private static void pushLocalDateTime(GeneratorAdapter mg, Type type, LocalDateTime value) {
        LocalDateTime ldt = value;
        mg.push(ldt.getYear());
        mg.push(ldt.getMonthValue());
        mg.push(ldt.getDayOfMonth());
        mg.push(ldt.getHour());
        mg.push(ldt.getMinute());
        mg.push(ldt.getSecond());
        mg.invokeStatic(type, LOCAL_DATETIME_OF);
    }

    private static void pushLocalTime(GeneratorAdapter mg, Type type, LocalTime value) {
        LocalTime lt = value;
        mg.push(lt.getHour());
        mg.push(lt.getMinute());
        mg.push(lt.getSecond());
        mg.invokeStatic(type, LOCAL_TIME_OF);
    }

    private static void validateConstructor(String className, Class<?>[] parameterTypes) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // it's OK. Maybe happen when datatype class wasn't generated yet
            return;
        }

        try {
            clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            if (parameterTypes.length == 0) {
                throw new ByteCodeGenerationException(
                        String.format("There is no default constructor for type '%s'.", className));
            } else {
                throw new ByteCodeGenerationException(
                        String.format("'%s' does not have a constructor with parameters '%s'.",
                                className,
                                Arrays.toString(parameterTypes)));
            }
        }
    }

    @Override
    public void write(ClassWriter classWriter) {

        GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, DEF_CONSTR, null, null, classWriter);

        // invokes the super class constructor
        mg.loadThis();
        mg.invokeConstructor(Type.getType(getParentClass()), DEF_CONSTR);

        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            FieldDescription fieldDescription = field.getValue();

            if (fieldDescription.hasDefaultValue()) {
                mg.loadThis();

                Object value = fieldDescription.getDefaultValue();
                String retClass = fieldDescription.getTypeDescriptor();
                Type type = Type.getType(retClass);
                pushValue(mg, type, value);

                String fieldTypeName = fieldDescription.getTypeDescriptor();
                mg.visitFieldInsn(Opcodes.PUTFIELD, getBeanNameWithPackage(), field.getKey(), fieldTypeName);
            }
        }

        mg.returnValue();
        mg.endMethod();
    }
}
