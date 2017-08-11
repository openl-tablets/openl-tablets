package org.openl.rules.maven.gen;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;

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

    public void addComment(StringBuilder buf) {
        buf.append(JavaClassGeneratorHelper.getCommentText("This class has been generated. Do not change it."));
    }

    public void addPackage(StringBuilder buf) {
        buf.append(JavaClassGeneratorHelper.getPackageText(ClassUtils.getPackageName(classForGeneration)));
    }

    public void addImports(StringBuilder buf) {
        for (String importStr : gatherImports()) {
            addImport(buf, importStr);
        }
    }

    public void addImport(StringBuilder buf, String importStr) {
        buf.append(JavaClassGeneratorHelper.getImportText(importStr));
    }

    protected Class<?> extractTypeToImport(Class<?> clazz) {
        Class<?> cl = clazz;
        while (cl.isArray()) {
            cl = cl.getComponentType();
        }
        return cl;
    }

    public Set<String> gatherImports() {
        Set<String> importsSet = new HashSet<String>();
        
        for (Method method : classForGeneration.getDeclaredMethods()) {
            if (method.getName().startsWith(GET)) {
                Class<?> methodReturnType = method.getReturnType();
                Class<?> typeToImport = extractTypeToImport(methodReturnType);
                if (!typeToImport.isPrimitive()) {
                    importsSet.add(filterTypeNameToImport(typeToImport));
                }
            }
            if (method.getName().equals(EQUALS)) {
                importsSet.add(filterTypeNameToImport(EqualsBuilder.class));
            }
            if (method.getName().startsWith(HASH_CODE)) {
                importsSet.add(filterTypeNameToImport(HashCodeBuilder.class));
            }
            if (method.getName().startsWith(TO_STRING)) {
                importsSet.add(filterTypeNameToImport(ArrayUtils.class));
            }
        }

        for (Constructor<?> constructor : classForGeneration.getDeclaredConstructors()) {
            for (Class<?> paramType : constructor.getParameterTypes()) {
                Class<?> typeToImport = extractTypeToImport(paramType);
                if (!typeToImport.isPrimitive()) {
                    importsSet.add(filterTypeNameToImport(typeToImport));
                }
            }
        }

        Class<?> superClass = getClassForGeneration().getSuperclass();
        if (!Object.class.equals(superClass)) {
            importsSet.add(filterTypeNameToImport(superClass));
        }
        importsSet.add(filterTypeNameToImport(Serializable.class));
        return importsSet;
    }

    protected String filterTypeNameToImport(Class<?> type) {
        if (type.isArray()) {
            throw new IllegalStateException();
        }
        return JavaClassGeneratorHelper.filterTypeName(type);
    }

    public void addClassDeclaration(StringBuilder buf, String className, String superClass) {
        buf.append(JavaClassGeneratorHelper.getSimplePublicClassDeclaration(className));
        if (superClass != null && !"Object".equals(superClass)) {
            buf.append(" extends ");
            buf.append(superClass);
        }
        buf.append(" implements Serializable ");
        buf.append(JavaClassGeneratorHelper.getOpenBracket());
    }

    public boolean getterExists(Method method, Set<String> allDatatypeFieldNames) {
        boolean exists = false;
        String fieldName = getFieldName(method.getName(), allDatatypeFieldNames);
        if (StringUtils.isNotBlank(fieldName)) {
            String getter = JavaClassGeneratorHelper
                .getPublicGetterMethod(JavaClassGeneratorHelper.filterTypeSimpleName(method.getReturnType()), fieldName);
            if (StringUtils.isNotBlank(getter)) {
                exists = true;
            }
        }
        return exists;
    }

    public void addGetter(StringBuilder buf, Method method, Set<String> allDatatypeFieldNames) {
        String fieldName = getFieldName(method.getName(), allDatatypeFieldNames);
        if (StringUtils.isNotBlank(fieldName)) {
            String getter = JavaClassGeneratorHelper
                .getPublicGetterMethod(JavaClassGeneratorHelper.filterTypeSimpleName(method.getReturnType()), fieldName);
            if (StringUtils.isNotBlank(getter)) {
                buf.append(getter);
            }
        }
    }

    public void addSetter(StringBuilder buf, Method method, Set<String> allDatatypeFieldNames) {
        String fieldName = getFieldName(method.getName(), allDatatypeFieldNames);
        if (StringUtils.isNotBlank(fieldName)) {
            buf.append(JavaClassGeneratorHelper.getPublicSetterMethod(
                JavaClassGeneratorHelper.filterTypeSimpleName(method.getParameterTypes()[0]),
                fieldName));
        }

    }

    public String getFieldName(String methodName, Set<String> allDatatypeFieldNames) {
        if (methodName != null && allDatatypeFieldNames != null) {
            String fieldNameFromMethod = methodName.substring(3);
            for (String datatypeField : allDatatypeFieldNames) {
                if (fieldNameFromMethod.equalsIgnoreCase(datatypeField)) {
                    /**
                     * return the name of the field from the set, that has a
                     * getter for itself
                     */
                    return datatypeField;
                }
            }
        }
        /**
         * Works when it is not possible to associate methodName with any field
         * name from the bean
         */
        return StringUtils.EMPTY;
    }
}
