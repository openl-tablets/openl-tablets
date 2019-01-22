package org.openl.gen.writers;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.openl.gen.FieldDescription;

public class DefaultConstructorWriter extends DefaultBeanByteCodeWriter {

    private static final Method STR_CONSTR = Method.getMethod("void <init> (java.lang.String)");
    private static final Method DEF_CONSTR = Method.getMethod("void <init> ()");
    
    public static final Map<String, Class<?>> DEFAULT_COLLECTIONS_INTERFACES;
    private static final Map<String, Class<?>> boxed = new HashMap<String, Class<?>>(8);
    
    static {
        boxed.put(Byte.class.getName(), byte.class);
        boxed.put(Short.class.getName(), short.class);
        boxed.put(Integer.class.getName(), int.class);
        boxed.put(Boolean.class.getName(), boolean.class);
        boxed.put(Character.class.getName(), char.class);
        boxed.put(Long.class.getName(), long.class);
        boxed.put(Float.class.getName(), float.class);
        boxed.put(Double.class.getName(), double.class);

        Map<String, Class<?>> defaultInterfaceCollections = new HashMap<String, Class<?>>(6);
        defaultInterfaceCollections.put(Collection.class.getName(), ArrayList.class);
        defaultInterfaceCollections.put(List.class.getName(), ArrayList.class);
        defaultInterfaceCollections.put(Set.class.getName(), HashSet.class);
        defaultInterfaceCollections.put(SortedSet.class.getName(), TreeSet.class);
        defaultInterfaceCollections.put(Map.class.getName(), HashMap.class);
        defaultInterfaceCollections.put(SortedMap.class.getName(), TreeMap.class);
        
        DEFAULT_COLLECTIONS_INTERFACES = Collections.unmodifiableMap(defaultInterfaceCollections);
    }

    /**
     * 
     * @param beanNameWithPackage name of the class being generated with package,
     *            symbol '/' is used as separator<br>
     *            (e.g. <code>my/test/TestClass</code>)
     * @param parentClass class descriptor for super class.
     * @param beanFields fields of generating class.
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
            if (DEFAULT_COLLECTIONS_INTERFACES.containsKey(className)) {
                // Collection, Map, SortedMap, List, Set 
                Class<?> defaultImpl = DEFAULT_COLLECTIONS_INTERFACES.get(className);
                Type defaultImplType = Type.getType(defaultImpl);
                mg.newInstance(defaultImplType);
                mg.dup();
                mg.invokeConstructor(defaultImplType, DEF_CONSTR);
            } else {
                // new SomeType()
                mg.newInstance(type);
                mg.dup();
                mg.invokeConstructor(type, DEF_CONSTR);
            }
        } else if (className.equals(String.class.getName())) {
            mg.push((String) value);
        } else if (className.equals(Date.class.getName())) {
            // new Date("07/12/2017 12:00:00 AM")
            mg.newInstance(type);
            mg.dup();
            // SimpleDateFormat is thread-unsafe
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);
            mg.push(simpleDateFormat.format((Date) value));
            mg.invokeConstructor(type, STR_CONSTR);
        } else if (boxed.containsKey(className)) {
            // Boxed.valueOf(value)
            Class<?> prim = boxed.get(className);
            Type primType = Type.getType(prim);
            pushValue(mg, primType, value);
            mg.valueOf(primType);
        } else if (value.getClass().isEnum()) {
            //SomeEnum.NAME
            Class<?> enumClass = value.getClass();
            Type enumType = Type.getType(enumClass);
            mg.getStatic(enumType, ((Enum)value).name(), enumType);
        } else {
            // new SomeType("value")
            mg.newInstance(type);
            mg.dup();
            mg.push(String.valueOf(value));
            mg.invokeConstructor(type, STR_CONSTR);
        }
    }

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
