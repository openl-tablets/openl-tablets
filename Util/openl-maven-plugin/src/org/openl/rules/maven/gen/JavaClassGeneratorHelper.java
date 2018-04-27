package org.openl.rules.maven.gen;

import org.openl.util.ClassUtils;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class JavaClassGeneratorHelper {

    private JavaClassGeneratorHelper() {
    }

    public static String filterTypeName(Class<?> type) {
        if (type != null) {
            return type.getCanonicalName();
        }
        return StringUtils.EMPTY;
    }
    
    public static String filterTypeSimpleName(Class<?> type) {
        if (type != null) {
            return type.getSimpleName();
        }
        return StringUtils.EMPTY;
    }

    /**
     * TODO: check the income package for valid value.
     */
    public static String getPackageText(String packageStr) {

        if (packageStr != null) {
            return String.format("package %s;\n\n", packageStr);
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * TODO: check comment string for valid symbols(escape special comment
     * symbols inside)
     */
    public static String getCommentText(String comment) {
        return String.format("/*\n * %s \n*/\n\n", comment);
    }

    public static String getImportText(String importStr) {
        return String.format("import %s;\n", importStr);
    }

    public static String getSimplePublicClassDeclaration(String className) {
        return String.format("\npublic class %s", className);
    }

    public static String getProtectedFieldDeclaration(String fieldType, String fieldName) {
        return String.format("  protected %s %s;\n\n", fieldType, fieldName);
    }

    public static String getProtectedFieldInitialzation(String fieldType, String fieldName, String fieldValue) {
        return String.format("  protected %s %s = %s;\n\n", fieldType, fieldName, fieldValue);
    }

    public static String getDefaultConstructor(String simpleClassName) {
        return String.format("\n  public %s() {\n    super();\n  }\n", simpleClassName);
    }

    public static String getConstructorWithFields(String simpleClassName, Map<String, Class<?>> fields,
            int numberOfParamsForSuperConstructor) {
        StringBuilder buf = new StringBuilder();
        if (fields.size() < 256 ) {
            // Generate constructor with parameters only in case where there are less than 256 arguments.
            // 255 arguments to the method is a Java limitation
            //
            buf.append(String.format("\n  public %s(", simpleClassName));
            Iterator<Entry<String, Class<?>>> fieldsIterator = fields.entrySet().iterator();
            while (fieldsIterator.hasNext()) {
                Entry<String, Class<?>> field = fieldsIterator.next();
                buf.append(String.format("%s %s", ClassUtils.getShortClassName(field.getValue()), field.getKey()));
                if (fieldsIterator.hasNext()) {
                    buf.append(", ");
                }
            }
            buf.append(") {\n");
            buf.append("    super(");
            fieldsIterator = fields.entrySet().iterator();
            for (int i = 0; i < numberOfParamsForSuperConstructor; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append(fieldsIterator.next().getKey());
            }
            buf.append(");\n");
            while (fieldsIterator.hasNext()) {
                Entry<String, Class<?>> field = fieldsIterator.next();
                buf.append(String.format("    this.%s = %s;\n", field.getKey(), field.getKey()));
            }
            buf.append("  }\n");

        }
        return buf.toString();
    }

    public static String getPublicGetterMethod(String fieldType, String fieldName) {
        String getterName = ClassUtils.getter(fieldName);
        if ("getClass".equals(getterName)) {
            return null;
        }
        return String.format("\n  public %s %s() {\n    return %s;\n  }\n", fieldType, getterName,
                fieldName);
    }

    /**
     * Gets the type name for cast from Object to given class. Support cast to
     * wrapper type of the primitive
     * 
     * @return canonical type name for cast to given class
     */
    public static String getTypeNameForCastFromObject(Class<?> clazz) {
        String canonicalClassName = filterTypeName(clazz);
        if (clazz != null && clazz.isPrimitive()) {
            Class<?> wrapperType = NumberUtils.getWrapperType(canonicalClassName);
            canonicalClassName = filterTypeName(wrapperType);
        }

        return canonicalClassName;
    }

    public static String getGetterWithCastMethod(Class<?> methodType, String methodToDecorate, String fieldName) {
        return String.format("  public %s %s() {\n   %s\n}\n", filterTypeSimpleName(methodType),
                ClassUtils.getter(fieldName), getDecoratorBody(methodType, methodToDecorate, fieldName));
    }

    public static String getDecoratorBody(Class<?> methodType, String methodToDecorate, String fieldName) {
        StringBuilder buf = new StringBuilder(300);
        buf.append("return ");
        if (methodType.isPrimitive()) {
            buf.append("(");
        }
        buf.append(String.format("(%s)%s(\"%s\")", getTypeNameForCastFromObject(methodType), methodToDecorate,
                fieldName));

        if (methodType.isPrimitive()) {
            buf.append(String.format(").%s", getWrapperMethod(methodType)));
        }

        buf.append(";");
        return buf.toString();
    }

    public static Object getWrapperMethod(Class<?> primitiveMethodType) {
        if (primitiveMethodType != null && primitiveMethodType.isPrimitive()) {
            return String.format("%sValue()", primitiveMethodType.getCanonicalName());
        }
        return StringUtils.EMPTY;
    }

    public static String getPublicSetterMethod(String fieldType, String fieldName) {
        return String.format("\n  public void %s(%s %s) {\n    this.%s = %s;\n  }\n", ClassUtils.setter(fieldName),
                fieldType, fieldName, fieldName, fieldName);
    }

    public static String getEqualsMethod(String simpleClassName, Set<String> fields) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n  public boolean equals(Object obj) {\n");
        buf.append("    EqualsBuilder builder = new EqualsBuilder();\n");
        buf.append(String.format("    if (!(obj instanceof %s)) {;\n", simpleClassName));
        buf.append("        return false;\n");
        buf.append("    }\n");
        buf.append(String.format("    %s another = (%s)obj;\n", simpleClassName, simpleClassName));
        for (String field : fields) {
            String getter = ClassUtils.getter(field) + "()";
            buf.append(String.format("    builder.append(another.%s,%s);\n", getter, getter));
        }
        buf.append("    return builder.isEquals();\n");
        buf.append("  }\n");
        return buf.toString();
    }

    public static String getHashCodeMethod(Set<String> fields) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n  public int hashCode() {\n");
        buf.append("    HashCodeBuilder builder = new HashCodeBuilder();\n");
        for (String field : fields) {
            String getter = ClassUtils.getter(field) + "()";
            buf.append(String.format("    builder.append(%s);\n", getter));
        }
        buf.append("    return builder.toHashCode();\n");
        buf.append("  }\n");
        return buf.toString();
    }

    public static String getToStringMethod(String simpleClassName, Map<String, Class<?>> fields) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n  public String toString() {\n");
        buf.append("    StringBuilder builder = new StringBuilder();\n");
        buf.append(String.format("    builder.append(\"%s {\");\n", simpleClassName));
        for (Entry<String, Class<?>> field : fields.entrySet()) {
            buf.append(String.format("    builder.append(\" %s=\");\n", field.getKey()));
            String getter = ClassUtils.getter(field.getKey()) + "()";
            if (field.getValue().isArray()) {
                buf.append(String.format("    builder.append(ArrayUtils.toString(%s));\n", getter));
            } else {
                buf.append(String.format("    builder.append(%s);\n", getter));
            }
        }
        buf.append("    builder.append(\" }\");\n");
        buf.append("    return builder.toString();\n");
        buf.append("  }\n");
        return buf.toString();
    }

    public static String getOpenBracket() {
        return "{\n";
    }

    public static Constructor<?> getConstructorByFieldsCount(Class<?> beanClass, int beanFieldsCount) {
        for (Constructor<?> constructor : beanClass.getConstructors()) {
            if (constructor.getParameterTypes().length == beanFieldsCount) {
                return constructor;
            }
        }
        return null;
    }

    public static Object getInterfaceDeclaration(String className) {
        return String.format("\npublic interface %s", className);
    }
}
