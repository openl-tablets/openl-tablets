package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public abstract class JavaGenerator {
    
    public static final String SET = "set";
    public static final String GET = "get";
    public static final String TO_STRING = "toString";
    public static final String HASH_CODE = "hashCode";
    public static final String EQUALS = "equals";
    
    private Class<?> classForGeneration;
    
    public abstract String generateJavaClass();
    
    public JavaGenerator(Class<?> classForGeneration) {
        this.classForGeneration = classForGeneration;
    }
    
    public Class<?> getClassForGeneration() {
        return classForGeneration;
    }
    
    public String getClassNameForGeneration() {
        return classForGeneration.getName();
    }
    
    public void addComment(StringBuffer buf) {
        buf.append(JavaClassGeneratorHelper.getCommentText("This class has been generated. Do not change it."));
    }
    
    public void addPackage(StringBuffer buf) {        
        buf.append(JavaClassGeneratorHelper.getPackageText(ClassUtils.getPackageName(classForGeneration)));
    }
    
    public void addImports(StringBuffer buf) {     
        for (String importStr : gatherImports()) {
            addImport(buf, importStr);
        }
    }
    
    public void addImport(StringBuffer buf, String importStr) {
        buf.append(JavaClassGeneratorHelper.getImportText(importStr));
    }
    
    public Set<String> gatherImports() {
        Set<String> importsSet = new HashSet<String>();
        
        for (Method method : classForGeneration.getDeclaredMethods()) {
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
        
        for (Constructor<?> constructor : classForGeneration.getDeclaredConstructors()) {
            for (Class<?> paramType : constructor.getParameterTypes()) {
                if (!paramType.isPrimitive() && !(paramType.isArray() && paramType.getComponentType().isPrimitive())) {
                    importsSet.add(filterTypeNameForImport(paramType));
                }
            }
        }
        
        Class<?> superClass = getClassForGeneration().getSuperclass();
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
    
    public void addClassDeclaration(StringBuffer buf, String className, String superClass) {        
        buf.append(JavaClassGeneratorHelper.getSimplePublicClassDeclaration(className));
        if (superClass != null && !"Object".equals(superClass)) {
            buf.append(" extends ");
            buf.append(superClass);
        }
        buf.append(JavaClassGeneratorHelper.getOpenBracket());
    }
    
    public void addGetter(StringBuffer buf, Method method) {
        String fieldName = getFieldName(method.getName());
        buf.append(JavaClassGeneratorHelper.getPublicGetterMethod(JavaClassGeneratorHelper.filterTypeName(method.getReturnType()), fieldName));
    }
    
    public String getFieldName(String methodName) {
        return String.format("%s%s", methodName.substring(3,4).toLowerCase(), methodName.substring(4));
    }
}
