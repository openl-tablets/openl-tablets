package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleBeanJavaGenerator {
    
    private static final Log LOG = LogFactory.getLog(SimpleBeanJavaGenerator.class);
    
    private static final String SET = "set";
    private static final String GET = "get";
    private static final String TO_STRING = "toString";
    private static final String HASH_CODE = "hashCode";
    private static final String EQUALS = "equals";
    
    private String datatypeName;
    private Class<?> datatypeClass;
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
        this.datatypeName = datatypeClass.getName();
        this.datatypeClass = datatypeClass;
        this.datatypeDeclaredFields = prepareFieldNames(declaredFields);
        this.datatypeAllFields = prepareFieldNames(allFields);        
    }
    
    public LinkedHashMap<String, Class<?>> prepareFieldNames(Map<String, Class<?>> fields){
        LinkedHashMap<String, Class<?>> preparedFields = new LinkedHashMap<String, Class<?>>();
        for(Entry<String, Class<?>> field : fields.entrySet()){
            String fieldName = field.getKey();
            String processedFieldName = String.format("%s%s", fieldName.substring(0, 1).toLowerCase(), fieldName.substring(1));
            preparedFields.put(processedFieldName, field.getValue());
        }
        return preparedFields;
    }
    
    public String generateJavaClass() {
        
        StringBuffer buf = new StringBuffer(10000);
        
        addComment(buf);
        
        addPackage(buf);
        
        addImports(buf);
        
        addClassDeclaration(buf, ClassUtils.getShortClassName(datatypeName), ClassUtils.getShortClassName(datatypeClass.getSuperclass()));
        
        addFieldsDeclaration(buf);
        
        addMethods(buf);
        
        buf.append("\n}");
        
        return buf.toString();
        
    }

    private void addMethods(StringBuffer buf) {
        addConstructors(buf);
        for (Method method : datatypeClass.getDeclaredMethods()) {
            if (method.getName().startsWith(GET)) {
                addGetter(buf, method);
            } else if (method.getName().startsWith(SET)) {
                addSetter(buf, method);
            } else if (method.getName().equals(EQUALS)) {
                buf.append(JavaClassGeneratorHelper.getEqualsMethod(datatypeClass.getSimpleName(), datatypeAllFields));
            } else if (method.getName().startsWith(HASH_CODE)) {
                buf.append(JavaClassGeneratorHelper.getHashCodeMethod(datatypeAllFields));
            } else if (method.getName().equals(TO_STRING)) {
                buf.append(JavaClassGeneratorHelper.getToStringMethod(datatypeClass.getSimpleName(), datatypeAllFields));
            }
        }
    }
    
    private void addConstructors(StringBuffer buf){
        buf.append(JavaClassGeneratorHelper.getDefaultConstructor(datatypeClass.getSimpleName()));
        Map<String, Class<?>> fieldsForConstructor = new LinkedHashMap<String, Class<?>>();
        int numberOfParamsForSuperConstructor = 0;
        if (!datatypeClass.getSuperclass().equals(Object.class)) {
            numberOfParamsForSuperConstructor = datatypeAllFields.size() - datatypeDeclaredFields.size();
            Constructor<?> superConstructorWithFields = JavaClassGeneratorHelper.getBeanConstructorWithAllFields(
                    datatypeClass.getSuperclass(), numberOfParamsForSuperConstructor);
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
        fieldsForConstructor.putAll(datatypeDeclaredFields);
        buf.append(JavaClassGeneratorHelper.getConstructorWithFields(datatypeClass.getSimpleName(),
                fieldsForConstructor, numberOfParamsForSuperConstructor));
    }
    
    private void addSetter(StringBuffer buf, Method method) {
        String fieldName = getFieldName(method.getName());
        buf.append(JavaClassGeneratorHelper.getPublicSetterMethod(JavaClassGeneratorHelper.filterTypeName(method.getParameterTypes()[0]), fieldName));
    }

    private void addGetter(StringBuffer buf, Method method) {
        String fieldName = getFieldName(method.getName());
        buf.append(JavaClassGeneratorHelper.getPublicGetterMethod(JavaClassGeneratorHelper.filterTypeName(method.getReturnType()), fieldName));
    }

    private String getFieldName(String methodName) {
        return String.format("%s%s", methodName.substring(3,4).toLowerCase(), methodName.substring(4));
    }

    private void addFieldsDeclaration(StringBuffer buf) {
        Object datatypeInstance = null;
        try {
            datatypeInstance = datatypeClass.newInstance();
        } catch (Exception e) {
            LOG.error(e);
        } 
        
        for (Field field : datatypeClass.getDeclaredFields()) {
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
            Field field = datatypeClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            fieldValue = field.get(datatypeInstance);            
        } catch (Exception e) {                    
            LOG.error(e);
        } 
        return fieldValue;
    }

    private void addClassDeclaration(StringBuffer buf, String className, String superClass) {        
        buf.append(JavaClassGeneratorHelper.getSimplePublicClassDeclaration(className));
        if (superClass != null && !"Object".equals(superClass)) {
            buf.append(" extends ");
            buf.append(superClass);
        }
        buf.append(JavaClassGeneratorHelper.getOpenBracket());
    }

    private void addImports(StringBuffer buf) {     
        for (String importStr : gatherImports()) {
            addImport(buf, importStr);
        }
    }

    private void addImport(StringBuffer buf, String importStr) {
        buf.append(JavaClassGeneratorHelper.getImportText(importStr));
    }

    private Set<String> gatherImports() {
        Set<String> importsSet = new HashSet<String>();
        
        for (Method method : datatypeClass.getDeclaredMethods()) {
            if (method.getName().startsWith(GET)) {
                Class<?> methodReturnType = method.getReturnType();
                if (!methodReturnType.isPrimitive() && !(methodReturnType.isArray() && methodReturnType.getComponentType().isPrimitive())) {
                    importsSet.add(filterTypeNameForImport(methodReturnType));
                }
            } 
            if (method.getName().equals(EQUALS)) {
                importsSet.add(filterTypeNameForImport(EqualsBuilder.class));
            }
            if (method.getName().startsWith(HASH_CODE)) {
                importsSet.add(filterTypeNameForImport(HashCodeBuilder.class));
            }
            if (method.getName().startsWith(TO_STRING)) {
                importsSet.add(filterTypeNameForImport(ArrayUtils.class));
            }
        }
        
        for (Constructor<?> constructor : datatypeClass.getDeclaredConstructors()) {
            for (Class<?> paramType : constructor.getParameterTypes()) {
                if (!paramType.isPrimitive() && !(paramType.isArray() && paramType.getComponentType().isPrimitive())) {
                    importsSet.add(filterTypeNameForImport(paramType));
                }
            }
        }
        
        Class<?> superClass = datatypeClass.getSuperclass();
        if (superClass != Object.class) {
            importsSet.add(filterTypeNameForImport(superClass));
        }
        return importsSet;
    }

    private String filterTypeNameForImport(Class<?> type) {
        String typeName = JavaClassGeneratorHelper.filterTypeName(type);
        int index = typeName.indexOf("[");
        if (index > 0 ) {
            return typeName.substring(0, index);
        } else {
            return typeName;
        }       
    }    

    private void addComment(StringBuffer buf) {
        buf.append(JavaClassGeneratorHelper.getCommentText("This class has been generated. Do not change it."));
    }
    
    private void addPackage(StringBuffer buf) {        
        buf.append(JavaClassGeneratorHelper.getPackageText(ClassUtils.getPackageName(datatypeClass)));
    }
}
