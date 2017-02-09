package org.openl.rules.maven.gen;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.generation.JavaClassGeneratorHelper;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

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

    public Set<String> gatherImports() {
        Set<String> importsSet = new HashSet<String>();

        for (Method method : classForGeneration.getDeclaredMethods()) {
            if (method.getName().startsWith(GET)) {
                Class<?> methodReturnType = method.getReturnType();
                if (!methodReturnType.isPrimitive()
                        && !(methodReturnType.isArray() && methodReturnType.getComponentType().isPrimitive())) {
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
        importsSet.add(filterTypeNameForImport(Serializable.class));
        return importsSet;
    }

    protected String filterTypeNameForImport(Class<?> type) {
        String typeName = JavaClassGeneratorHelper.filterTypeName(type);
        int index = typeName.indexOf("[");
        if (index > 0) {
            return typeName.substring(0, index);
        } else {
            return typeName;
        }
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
            String getter = JavaClassGeneratorHelper.getPublicGetterMethod(
                    JavaClassGeneratorHelper.filterTypeName(method.getReturnType()), fieldName);
            if (StringUtils.isNotBlank(getter)) {
                exists = true;
            }
        }
        return exists;
    }

    public void addGetter(StringBuilder buf, Method method, Set<String> allDatatypeFieldNames) {
        String fieldName = getFieldName(method.getName(), allDatatypeFieldNames);
        if (StringUtils.isNotBlank(fieldName)) {
            String getter = JavaClassGeneratorHelper.getPublicGetterMethod(
                    JavaClassGeneratorHelper.filterTypeName(method.getReturnType()), fieldName);
            if (StringUtils.isNotBlank(getter)) {
                buf.append(getter);
            }
        }
    }

    public void addSetter(StringBuilder buf, Method method, Set<String> allDatatypeFieldNames) {
        String fieldName = getFieldName(method.getName(), allDatatypeFieldNames);
        if (StringUtils.isNotBlank(fieldName)) {
            buf.append(JavaClassGeneratorHelper.getPublicSetterMethod(
                    JavaClassGeneratorHelper.filterTypeName(method.getParameterTypes()[0]), fieldName));
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
