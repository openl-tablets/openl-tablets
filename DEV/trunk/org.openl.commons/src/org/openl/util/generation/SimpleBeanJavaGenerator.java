package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleBeanJavaGenerator extends JavaGenerator {
    
    private static final Log LOG = LogFactory.getLog(SimpleBeanJavaGenerator.class);

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
        
        initializationWriters.put(Number.class, new NumberInitializationWriter());
        
        initializationWriters.put(String.class, new StringInitializationWriter());
        initializationWriters.put(char.class, new CharInitializationWriter());
        initializationWriters.put(Character.class, new CharInitializationWriter());
    }
    
    public SimpleBeanJavaGenerator(Class<?> datatypeClass, Map<String, Class<?>> declaredFields, Map<String, Class<?>> allFields) {
        super(datatypeClass);
        this.datatypeDeclaredFields = new LinkedHashMap<String, Class<?>>(declaredFields);
        this.datatypeAllFields = new LinkedHashMap<String, Class<?>>(allFields);        
    }
    
    public String generateJavaClass() {
        
        StringBuffer buf = new StringBuffer(10000);
        
        addComment(buf);
        
        addPackage(buf);
        
        addImports(buf);
        
        addClassDeclaration(buf, ClassUtils.getShortClassName(getClassNameForGeneration()), ClassUtils.getShortClassName(getClassForGeneration().getSuperclass()));
        
        addFieldsDeclaration(buf);
        
        addConstructors(buf);
        
        addMethods(buf);
        
        buf.append("\n}");
        
        return buf.toString();        
    }

    private void addMethods(StringBuffer buf) {        
        for (Method method : getClassForGeneration().getDeclaredMethods()) {
            if (method.getName().startsWith(JavaGenerator.GET)) {
                addGetter(buf, method, datatypeAllFields.keySet());
            } else if (method.getName().startsWith(JavaGenerator.SET)) {
                addSetter(buf, method, datatypeAllFields.keySet());
            } else if (method.getName().equals(JavaGenerator.EQUALS)) {
                buf.append(JavaClassGeneratorHelper.getEqualsMethod(getClassForGeneration().getSimpleName(), datatypeAllFields.keySet()));
            } else if (method.getName().startsWith(JavaGenerator.HASH_CODE)) {
                buf.append(JavaClassGeneratorHelper.getHashCodeMethod(datatypeAllFields.keySet()));
            } else if (method.getName().equals(JavaGenerator.TO_STRING)) {
                buf.append(JavaClassGeneratorHelper.getToStringMethod(getClassForGeneration().getSimpleName(), datatypeAllFields));
            }
        }
    }
    
    private void addConstructors(StringBuffer buf) {
        /** Write default constructor*/
        buf.append(JavaClassGeneratorHelper.getDefaultConstructor(getClassForGeneration().getSimpleName()));
        
        /** Write constructor with parameters*/
        Map<String, Class<?>> fieldsForConstructor = new LinkedHashMap<String, Class<?>>();
        int numberOfParamsForSuperConstructor = 0;
        
        /** Check if the super class is a type differ from Object*/
        if (!getClassForGeneration().getSuperclass().equals(Object.class)) {
            /** Add call for super class constructor*/ 
            numberOfParamsForSuperConstructor = datatypeAllFields.size() - datatypeDeclaredFields.size();
            
            /** Gets the parent constructor with fields*/
            Constructor<?> superConstructorWithFields = JavaClassGeneratorHelper.getBeanConstructorWithAllFields(
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
            }else{
                numberOfParamsForSuperConstructor = 0;
            }
        }
        /** Put all the fields declared in current datatype*/
        fieldsForConstructor.putAll(datatypeDeclaredFields);
        buf.append(JavaClassGeneratorHelper.getConstructorWithFields(getClassForGeneration().getSimpleName(),
                fieldsForConstructor, numberOfParamsForSuperConstructor));
    }

    private void addFieldsDeclaration(StringBuffer buf) {
        Object datatypeInstance = null;
        try {
            datatypeInstance = getClassForGeneration().newInstance();
        } catch (Exception e) {
            LOG.error(e);
        } 
        
        for (Field field : getClassForGeneration().getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            Object fieldValue = getFieldValue(datatypeInstance, field.getName());
            
            // field value contains default value
            //
            if (fieldValue != null) {    
                // get the appropriate initialization writer for the type of field.
                //
                TypeInitializationWriter writer = getFieldValueWriter(fieldType);
                if (writer == null) {
                    // error message if can`t process value of given type.
                    //
                    String errorMessage = String.format("Can`t write value for %s field of type %s", fieldValue, 
                        fieldType.getName());
                    LOG.error(errorMessage);
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
        buf.append("\n");
    }
    
    private TypeInitializationWriter getFieldValueWriter(Class<?> fieldValueClass) {
        TypeInitializationWriter writer = initializationWriters.get(fieldValueClass);
        if (writer == null) {
            if (ClassUtils.isAssignable(fieldValueClass, Number.class)) {
                writer = initializationWriters.get(Number.class);
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
            LOG.error(e);
        } 
        return fieldValue;
    }
}
