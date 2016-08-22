package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBeanJavaGenerator extends JavaGenerator {

    private final Logger log = LoggerFactory.getLogger(SimpleBeanJavaGenerator.class);

    private Map<String, Class<?>> datatypeDeclaredFields;
    private Map<String, Class<?>> datatypeAllFields;

    private static Map<Class<?>, TypeInitializationWriter> initializationWriters;

    static {
        initializationWriters = new HashMap<Class<?>, TypeInitializationWriter>();

        initializationWriters.put(byte.class, new CommonInitializationWriter());
        initializationWriters.put(short.class, new CommonInitializationWriter());
        initializationWriters.put(int.class, new CommonInitializationWriter());
        initializationWriters.put(long.class, new CommonInitializationWriter());
        initializationWriters.put(float.class, new CommonInitializationWriter());
        initializationWriters.put(double.class, new CommonInitializationWriter());
        initializationWriters.put(boolean.class, new CommonInitializationWriter());

        initializationWriters.put(Byte.class, new CommonInitializationWriter());
        initializationWriters.put(Short.class, new CommonInitializationWriter());
        initializationWriters.put(Integer.class, new CommonInitializationWriter());
        initializationWriters.put(Long.class, new CommonInitializationWriter());
        initializationWriters.put(Float.class, new CommonInitializationWriter());
        initializationWriters.put(Double.class, new CommonInitializationWriter());
        initializationWriters.put(Boolean.class, new CommonInitializationWriter());
        initializationWriters.put(Date.class, new DateInitializationWriter());

        initializationWriters.put(Number.class, new NumberInitializationWriter());

        initializationWriters.put(String.class, new StringInitializationWriter());
        initializationWriters.put(char.class, new CharInitializationWriter());
        initializationWriters.put(Character.class, new CharInitializationWriter());
        initializationWriters.put(MarkerClass.class, new DefaultConstructorInitWriter());
        initializationWriters.put(EmptyArrayMarkerClass.class, new DefaultEmptyArrayConstructorInitWriter());
    }

    public SimpleBeanJavaGenerator(Class<?> datatypeClass) {
        super(datatypeClass);
        this.datatypeDeclaredFields = new LinkedHashMap<String, Class<?>>(getFieldsDescription(datatypeClass.getDeclaredFields()));
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
        return allFields.toArray(new Field[]{});
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
        addImport(buf, filterTypeNameForImport(XmlRootElement.class));
        addImport(buf, filterTypeNameForImport(XmlElement.class));
        addImport(buf, filterTypeNameForImport(XmlType.class));

        String packageName = ClassUtils.getPackageName(getClassForGeneration());

        String[] packageParts = packageName.split("\\.");
        StringBuilder namespace = new StringBuilder("http://");
        for (int i = packageParts.length - 1; i >= 0; i--) {
            namespace.append(packageParts[i]);
            if (i != 0) {
                namespace.append(".");
            }
        }

        buf.append("\n@XmlRootElement(namespace=\"").append(namespace.toString()).append("\")");
        buf.append("\n@XmlType(namespace=\"").append(namespace.toString()).append("\")");
    }

    public String generateJavaClass() {

        StringBuilder buf = new StringBuilder(10000);

        addComment(buf);

        addPackage(buf);

        addImports(buf);
        
        addJAXBAnnotations(buf);

        addClassDeclaration(buf, ClassUtils.getShortClassName(getClassForGeneration()),
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
                String fieldName = getFieldName(method.getName(), datatypeAllFields.keySet());
               
                String defaultFieldValue = null;
                Field field = null;
                try {
                    field = getClassForGeneration().getDeclaredField(fieldName);
                    DefaultValue defaultValueAnnotation = field.getAnnotation(DefaultValue.class);
                    if (defaultValueAnnotation != null){
                        defaultFieldValue = defaultValueAnnotation.value();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                if (getterExists(method, datatypeAllFields.keySet())) {
                    if (defaultFieldValue != null) {
                        if (DefaultValue.DEFAULT.equals(defaultFieldValue)) {
                            buf.append("\n  @XmlElement(name=\"").append(fieldName).append("\", nillable=false)");
                        } else {
                            buf.append("\n  @XmlElement(name=\"")
                                .append(fieldName)
                                .append("\", defaultValue=\"")
                                .append(defaultFieldValue)
                                .append("\")");
                        }
                    } else {
                        buf.append("\n  @XmlElement(name=\"").append(fieldName).append("\", nillable=true)");
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
                    getClassForGeneration().getSuperclass(), numberOfParamsForSuperConstructor);
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
                fieldsForConstructor, numberOfParamsForSuperConstructor));
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
                TypeInitializationWriter writer = getFieldValueWriter(field);
                if (writer == null) {
                    // error message if can`t process value of given type.
                    //
                    log.error("Can`t write value for {} field of type {}", fieldValue, fieldType.getName());
                } else {
                    // write value initialization to bean class
                    String valueInitialzation = writer.getInitialization(fieldValue);
                    buf.append(JavaClassGeneratorHelper.getProtectedFieldInitialzation(
                            JavaClassGeneratorHelper.filterTypeName(fieldType), field.getName(), valueInitialzation));
                }
            } else {
                // write field declaration
                buf.append(JavaClassGeneratorHelper.getProtectedFieldDeclaration(
                        JavaClassGeneratorHelper.filterTypeName(fieldType), field.getName()));
            }
        }
    }

    private TypeInitializationWriter getFieldValueWriter(Field field) {
        Class<?> fieldValueClass = field.getType();
        TypeInitializationWriter writer = initializationWriters.get(fieldValueClass);
        if (writer == null) {
            if (ClassUtils.isAssignable(fieldValueClass, Number.class)) {
                writer = initializationWriters.get(Number.class);
            }
            DefaultValue defaultValueAnnotation = field.getAnnotation(DefaultValue.class);
            String defaultFieldValue = null;
            if (defaultValueAnnotation != null){
                defaultFieldValue = defaultValueAnnotation.value();
            }
            if (DefaultValue.DEFAULT.equals(defaultFieldValue)) {
                if (fieldValueClass.isArray()){
                    writer = initializationWriters.get(EmptyArrayMarkerClass.class);
                }else{
                    writer = initializationWriters.get(MarkerClass.class);
                }
            }
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

    private final class MarkerClass{}
    private final class EmptyArrayMarkerClass{};
}
