package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class SimpleBeanJavaGenerator {
    
    private String datatypeName;
    private Class<?> datatypeClass;
    private Map<String, Class<?>> datatypeFields;
    
    public SimpleBeanJavaGenerator(Class<?> datatypeClass, Map<String, Class<?>> fields) {
        this.datatypeName = datatypeClass.getName();
        this.datatypeClass = datatypeClass;
        this.datatypeFields = prepareFieldNames(fields);
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
            if (method.getName().startsWith("get")) {
                addGetter(buf, method);
            } else if (method.getName().startsWith("set")) {
                addSetter(buf, method);
            } else if (method.getName().equals("equals")) {
                buf.append(JavaClassGeneratorHelper.getEqualsMethod(datatypeClass.getSimpleName(), datatypeFields));
            } else if (method.getName().startsWith("hashCode")) {
                buf.append(JavaClassGeneratorHelper.getHashCodeMethod(datatypeFields));
            } else if (method.getName().equals("toString")) {
                buf.append(JavaClassGeneratorHelper.getToStringMethod(datatypeClass.getSimpleName(), datatypeFields));
            }
        }
    }
    
    private void addConstructors(StringBuffer buf){
        buf.append(JavaClassGeneratorHelper.getDefaultConstructor(datatypeClass.getSimpleName()));
        Map<String, Class<?>> fieldsForConstructor = new LinkedHashMap<String, Class<?>>();
        int numberOfParamsForSuperConstructor = 0;
        if (!datatypeClass.getSuperclass().equals(Object.class)) {
            Constructor<?> superConstructorWithFields = getSuperConstructorWithFields();
            int i = 0;
            for (Class<?> paramClass : superConstructorWithFields.getParameterTypes()) {
                fieldsForConstructor.put("arg" + i, paramClass);
                i++;
            }
            numberOfParamsForSuperConstructor = superConstructorWithFields.getParameterTypes().length;
        }
        fieldsForConstructor.putAll(datatypeFields);
        buf.append(JavaClassGeneratorHelper.getConstructorWithFields(datatypeClass.getSimpleName(),
                fieldsForConstructor, numberOfParamsForSuperConstructor));
    }
    
    private Constructor<?> getSuperConstructorWithFields() {
        Class<?> superClass = datatypeClass.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            for (Constructor<?> constructor : superClass.getConstructors()) {
                if (constructor.getParameterTypes().length > 0) {
                    return constructor;
                }
            }
        }
        return null;
    }
    
    private void addSetter(StringBuffer buf, Method method) {
        String fieldName = getFieldName(method.getName());
        buf.append(JavaClassGeneratorHelper.getPublicSetterMethod(filterTypeName(method.getParameterTypes()[0]), fieldName));
    }

    private void addGetter(StringBuffer buf, Method method) {
        String fieldName = getFieldName(method.getName());
        buf.append(JavaClassGeneratorHelper.getPublicGetterMethod(filterTypeName(method.getReturnType()), fieldName));
    }

    private String getFieldName(String methodName) {
        return String.format("%s%s", methodName.substring(3,4).toLowerCase(), methodName.substring(4));
    }

    private void addFieldsDeclaration(StringBuffer buf) {
        for (Method method : datatypeClass.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                buf.append(JavaClassGeneratorHelper.getProtectedFieldDeclaration(filterTypeName(method.getReturnType()), getFieldName(method.getName())));
            } 
        }
        buf.append("\n");
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
            if (method.getName().startsWith("get")) {
                Class<?> methodReturnType = method.getReturnType();
                if (!methodReturnType.isPrimitive()) {
                    importsSet.add(filterTypeNameForImport(methodReturnType));
                }
            } 
            if (method.getName().equals("equals")) {
                importsSet.add(filterTypeNameForImport(EqualsBuilder.class));
            }
            if (method.getName().startsWith("hashCode")) {
                importsSet.add(filterTypeNameForImport(HashCodeBuilder.class));
            }
            if (method.getName().startsWith("toString")) {
                importsSet.add(filterTypeNameForImport(ArrayUtils.class));
            }
        }
        
        for (Constructor<?> constructor : datatypeClass.getDeclaredConstructors()) {
            for (Class<?> paramType : constructor.getParameterTypes()) {
                if (!paramType.isPrimitive()) {
                    importsSet.add(filterTypeNameForImport(paramType));
                }
            }
        }
        return importsSet;
    }

    private String filterTypeNameForImport(Class<?> type) {
        String typeName = filterTypeName(type);
        int index = typeName.indexOf("[");
        if (index > 0 ) {
            return typeName.substring(0, index);
        } else {
            return typeName;
        }
                
    }

    private String filterTypeName(Class<?> type) {
        if (!type.isPrimitive()) {
            return String.format("%s.%s", ClassUtils.getPackageName(type), ClassUtils.getShortClassName(type));
        } else {
            return ClassUtils.getShortClassName(type);
        }
    }

    private void addComment(StringBuffer buf) {
        buf.append(JavaClassGeneratorHelper.getCommentText("This class has been generated. Do not change it."));
    }
    
    private void addPackage(StringBuffer buf) {        
        buf.append(JavaClassGeneratorHelper.getPackageText(ClassUtils.getPackageName(datatypeClass)));
    }
}
