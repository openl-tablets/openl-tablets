package org.openl.util.generation;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;

public class SimpleBeanJavaGenerator {
    
    private String datatypeName;
    private Class<?> datatypeClass;
    
    public SimpleBeanJavaGenerator(Class<?> datatypeClass) {
        this.datatypeName = datatypeClass.getName();
        this.datatypeClass = datatypeClass;
    }
    
    public String generateJavaClass() {
        
        StringBuffer buf = new StringBuffer(10000);
        
        addComment(buf);
        
        addPackage(buf);
        
        addImports(buf);
        
        addClassDeclaration(buf, ClassUtils.getShortClassName(datatypeName));
        
        addFieldsDeclaration(buf);
        
        addMethods(buf);
        
        buf.append("\n}");
        
        return buf.toString();
        
    }

    private void addMethods(StringBuffer buf) {
        for (Method method : datatypeClass.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                addGetter(buf, method);
            } else if (method.getName().startsWith("set")) {
                addSetter(buf, method);
            }
        }
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
                buf.append(JavaClassGeneratorHelper.getPrivateFieldDeclaration(filterTypeName(method.getReturnType()), getFieldName(method.getName())));
            } 
        }
        buf.append("\n");
    }

    private void addClassDeclaration(StringBuffer buf, String className) {        
        buf.append(JavaClassGeneratorHelper.getSimplePublicClassDeclaration(className));
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
