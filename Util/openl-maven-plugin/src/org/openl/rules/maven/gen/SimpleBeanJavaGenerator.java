package org.openl.rules.maven.gen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

public class SimpleBeanJavaGenerator extends JavaGenerator {

    private final Logger log = LoggerFactory.getLogger(SimpleBeanJavaGenerator.class);

    private Map<String, Class<?>> datatypeDeclaredFields;
    private Map<String, Class<?>> datatypeAllFields;

    public static final Map<Class<?>, TypeInitializationWriter> INITIALIZATION_WRITERS;

    static {
        Map<Class<?>, TypeInitializationWriter> initializationWriters = new HashMap<Class<?>, TypeInitializationWriter>();

        initializationWriters.put(byte.class, new CommonInitializationWriter());
        initializationWriters.put(short.class, new CommonInitializationWriter());
        initializationWriters.put(int.class, new CommonInitializationWriter());
        initializationWriters.put(long.class, new LongInitializationWriter());
        initializationWriters.put(float.class, new FloatInitializationWriter());
        initializationWriters.put(double.class, new CommonInitializationWriter());
        initializationWriters.put(boolean.class, new CommonInitializationWriter());

        initializationWriters.put(Byte.class, new CommonInitializationWriter());
        initializationWriters.put(Short.class, new CommonInitializationWriter());
        initializationWriters.put(Integer.class, new CommonInitializationWriter());
        initializationWriters.put(Long.class, new LongInitializationWriter());
        initializationWriters.put(Float.class, new FloatInitializationWriter());
        initializationWriters.put(Double.class, new CommonInitializationWriter());
        initializationWriters.put(Boolean.class, new CommonInitializationWriter());
        initializationWriters.put(Date.class, new DateInitializationWriter());

        initializationWriters.put(Number.class, new NumberInitializationWriter());

        initializationWriters.put(String.class, new StringInitializationWriter());
        initializationWriters.put(Enum.class, new EnumInitializationWriter());
        initializationWriters.put(char.class, new CharInitializationWriter());
        initializationWriters.put(Character.class, new CharInitializationWriter());
        initializationWriters.put(Object.class, new DefaultInitializationWriter());
        initializationWriters.put(Object[].class, new ArrayInitializationWriter());

        INITIALIZATION_WRITERS = Collections.unmodifiableMap(initializationWriters);
    }

    public SimpleBeanJavaGenerator(Class<?> datatypeClass) {
        super(datatypeClass);
        this.datatypeDeclaredFields = new LinkedHashMap<String, Class<?>>(
            getFieldsDescription(datatypeClass.getDeclaredFields()));
        this.datatypeAllFields = new LinkedHashMap<String, Class<?>>(getFieldsDescription(getAllFields(datatypeClass)));
    }

    private Field[] getAllFields(Class<?> cls) {
        final List<Field> allFields = new ArrayList<Field>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                allFields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allFields.toArray(new Field[] {});
    }

    private Map<String, Class<?>> getFieldsDescription(Field[] fields) {
        Map<String, Class<?>> fieldsDescriprtion = new LinkedHashMap<String, Class<?>>();
        for (Field field : fields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                fieldsDescriprtion.put(field.getName(), field.getType());
            }
        }
        return fieldsDescriprtion;
    }

    private void addJAXBAnnotations(StringBuilder buf) {

        addImport(buf, "javax.xml.bind.annotation.XmlElement");
        addImport(buf, "javax.xml.bind.annotation.XmlRootElement");
        addImport(buf, "javax.xml.bind.annotation.XmlType");

        Class<?> clazz = getClassForGeneration();
        String packageName = ClassUtils.getPackageName(clazz);

        String[] packageParts = packageName.split("\\.");
        StringBuilder namespace = new StringBuilder("http://");
        for (int i = packageParts.length - 1; i >= 0; i--) {
            namespace.append(packageParts[i]);
            if (i != 0) {
                namespace.append(".");
            }
        }

        XmlRootElement rootEl = clazz.getAnnotation(XmlRootElement.class);
        if (rootEl != null) {
            buf.append("\n@XmlRootElement(");
            appendNotDefault(buf, "namespace", rootEl.namespace(), "##default");
            appendNotDefault(buf, "name", rootEl.name(), "##default");
            buf.append(")");
        }
        XmlType type = clazz.getAnnotation(XmlType.class);
        if (type != null) {
            buf.append("\n@XmlType(");
            appendNotDefault(buf, "namespace", type.namespace(), "##default");
            appendNotDefault(buf, "name", type.name(), "##default");
            buf.append(")");
        }
    }

    public String generateJavaClass() {

        StringBuilder buf = new StringBuilder(10000);

        addComment(buf);

        addPackage(buf);

        addImports(buf);

        addJAXBAnnotations(buf);

        addClassDeclaration(buf,
            ClassUtils.getShortClassName(getClassForGeneration()),
            ClassUtils.getShortClassName(getClassForGeneration().getSuperclass()));

        addFieldsDeclaration(buf);

        addConstructors(buf);

        addMethods(buf);

        buf.append("\n}");

        return buf.toString();
    }

    private void addMethods(StringBuilder buf) {
        Method[] methods = getClassForGeneration().getDeclaredMethods();
        Arrays.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        for (Method method : methods) {
            if (method.getName().startsWith(JavaGenerator.GET)) {

                if (getterExists(method, datatypeAllFields.keySet())) {
                    XmlElement annotation = method.getAnnotation(XmlElement.class);
                    if (annotation != null) {
                        buf.append("\n  @XmlElement(");
                        appendNotDefault(buf, "name", annotation.name(), "##default");
                        appendNotDefault(buf, "nillable", annotation.nillable(), false);
                        appendNotDefault(buf, "defaultValue", annotation.defaultValue(), "\u0000");
                        buf.append(")");
                    }
                    addGetter(buf, method, datatypeAllFields.keySet());
                }
            } else if (method.getName().startsWith(JavaGenerator.SET)) {
                addSetter(buf, method, datatypeAllFields.keySet());
            } else if (method.getName().equals(JavaGenerator.EQUALS)) {
                buf.append(JavaClassGeneratorHelper.getEqualsMethod(getClassForGeneration().getSimpleName(),
                    datatypeAllFields.keySet()));
            } else if (method.getName().startsWith(JavaGenerator.HASH_CODE)) {
                buf.append(JavaClassGeneratorHelper.getHashCodeMethod(datatypeAllFields.keySet()));
            } else if (method.getName().equals(JavaGenerator.TO_STRING)) {
                buf.append(JavaClassGeneratorHelper.getToStringMethod(getClassForGeneration().getSimpleName(),
                    datatypeAllFields));
            }
        }
    }

    private void appendNotDefault(StringBuilder buf, String name, Object value, Object defValue) {
        if (value != null && !value.equals(defValue) ) {
            if (buf.charAt(buf.length() - 1) != '(') {
                buf.append(", ");
            }
            buf.append(name).append('=');
            boolean isString = value instanceof String;
            if (isString) {
                buf.append('"');
            }
            buf.append(value);
            if (isString) {
                buf.append('"');
            }
        }

    }
    private void addConstructors(StringBuilder buf) {
        /** Write default constructor */
        buf.append(JavaClassGeneratorHelper.getDefaultConstructor(getClassForGeneration().getSimpleName()));

        /** Write constructor with parameters */
        Map<String, Class<?>> fieldsForConstructor = new LinkedHashMap<String, Class<?>>();
        int numberOfParamsForSuperConstructor = 0;

        /** Check if the super class is a type differ from Object */
        if (!getClassForGeneration().getSuperclass().equals(Object.class)) {
            /** Add call for super class constructor */
            numberOfParamsForSuperConstructor = datatypeAllFields.size() - datatypeDeclaredFields.size();

            /** Gets the parent constructor with fields */
            Constructor<?> superConstructorWithFields = JavaClassGeneratorHelper.getConstructorByFieldsCount(
                getClassForGeneration().getSuperclass(),
                numberOfParamsForSuperConstructor);
            if (superConstructorWithFields != null) {
                int i = 0;
                for (Entry<String, Class<?>> field : datatypeAllFields.entrySet()) {
                    if (field.getValue() == superConstructorWithFields.getParameterTypes()[i]) {
                        fieldsForConstructor.put(field.getKey(), field.getValue());
                    } else {
                        // can not associate fields with parent constructor
                        fieldsForConstructor.clear();
                        numberOfParamsForSuperConstructor = 0;
                        break;
                    }
                    i++;
                    if (i == numberOfParamsForSuperConstructor) {
                        break;
                    }
                }
            } else {
                numberOfParamsForSuperConstructor = 0;
            }
        }
        /** Put all the fields declared in current datatype */
        fieldsForConstructor.putAll(datatypeDeclaredFields);
        buf.append(JavaClassGeneratorHelper.getConstructorWithFields(getClassForGeneration().getSimpleName(),
            fieldsForConstructor,
            numberOfParamsForSuperConstructor));
    }

    private void addFieldsDeclaration(StringBuilder buf) {
        Object datatypeInstance = null;
        try {
            datatypeInstance = getClassForGeneration().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        Field[] fields = getClassForGeneration().getDeclaredFields();
        Arrays.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            Object fieldValue = getFieldValue(datatypeInstance, field.getName());

            // field value contains default value
            //
            if (fieldValue != null) {
                // get the appropriate initialization writer for the type of
                // field.
                //
                Class<?> fieldValueClass = field.getType();
                TypeInitializationWriter writer = geTypeInitializationWriter(fieldValueClass);
                if (writer == null) {
                    // error message if can`t process value of given type.
                    //
                    log.error("Can`t write value for {} field of type {}", fieldValue, fieldType.getName());
                } else {
                    // write value initialization to bean class
                    String valueInitialzation = writer.getInitialization(fieldValue);
                    buf.append(JavaClassGeneratorHelper.getProtectedFieldInitialzation(JavaClassGeneratorHelper
                        .filterTypeSimpleName(fieldType), field.getName(), valueInitialzation));
                }
            } else {
                // write field declaration
                buf.append(JavaClassGeneratorHelper.getProtectedFieldDeclaration(
                    JavaClassGeneratorHelper.filterTypeSimpleName(fieldType),
                    field.getName()));
            }
        }
    }

    public static TypeInitializationWriter geTypeInitializationWriter(Class<?> type) {
        TypeInitializationWriter writer = SimpleBeanJavaGenerator.INITIALIZATION_WRITERS.get(type);
        if (writer != null) {
            // As is
        } else if (Number.class.isAssignableFrom(type)) {
            writer = INITIALIZATION_WRITERS.get(Number.class);
        } else if (type.isEnum()) {
            writer = INITIALIZATION_WRITERS.get(Enum.class);
        } else if (type.isArray()) {
            writer = INITIALIZATION_WRITERS.get(Object[].class);
        } else {
            writer = INITIALIZATION_WRITERS.get(Object.class);
        }
        return writer;
    }

    private Object getFieldValue(Object datatypeInstance, String fieldName) {
        Object fieldValue = null;
        try {
            Field field = getClassForGeneration().getDeclaredField(fieldName);
            field.setAccessible(true);
            fieldValue = field.get(datatypeInstance);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return fieldValue;
    }
}
