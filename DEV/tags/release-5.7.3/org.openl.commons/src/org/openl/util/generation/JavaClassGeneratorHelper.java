package org.openl.util.generation;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.util.StringTool;

public class JavaClassGeneratorHelper {
    
    public static String getPackageText(String packageStr) {
        if (packageStr != null) {
            return String.format("package %s;\n\n", packageStr);            
        } else {
            return null;
        }
    }
    
    public static String getCommentText(String comment) {
        return String.format("/*\n * %s \n*/\n\n", comment);
    }
    
    public static String getPackage(String classNameWithNamespace) {
        int idx = classNameWithNamespace.lastIndexOf('.');
        if (idx > 0) {
            return  classNameWithNamespace.substring(0, idx);
        } else {
            return null;
        }
    }
    
    /**
     * 
     * @param classNameWithNamespace name of the class with namespace, symbols '/' or '.' are supported to be a separator<br> 
     * (e.g. <code>my/test/TestClass</code> or <code>my.test.TestClass</code>)
     * 
     * @return name of the class file without package (e.g. <code>TestClass</code>) if no one of the 
     * supported symbols found, returns classNameWithNamespace. 
     */
    public static String getShortClassName(String classNameWithNamespace) {         
        if (classNameWithNamespace.contains("/")) {
            String[] path = classNameWithNamespace.split("/");
            return path[path.length - 1];
        } else if (classNameWithNamespace.contains(".")) {
            return ClassUtils.getShortCanonicalName(classNameWithNamespace);
        }
        return classNameWithNamespace;
    }
    
    public static String getImportText(String importStr) {
        return String.format("import %s;\n", importStr);        
    }
    
    public static String getSimplePublicClassDeclaration(String className) {
        return String.format("\npublic class %s", className);
    }
    
    public static String addExtendingClassDeclaration(String className, String extendableClass) {        
        return String.format("%s extends %s", getSimplePublicClassDeclaration(className), extendableClass);
    }
    
    public static String addImplementingInterfToClassDeclaration(String classDeclaration, String[] implementsInterfaces) {
        String interfaces = StringUtils.join(implementsInterfaces, ",");
        return String.format("%s implements %s", classDeclaration, interfaces);
    }
    
    public static String getProtectedFieldDeclaration(String fieldType, String fieldName) {
        return String.format("  protected %s %s;\n\n", fieldType, fieldName);
    }
    
    public static String getDefaultConstructor(String simpleClassName) {
        return String.format("\npublic %s() {\n    super();\n}\n", simpleClassName);
    }

    public static String getConstructorWithFields(String simpleClassName, Map<String, Class<?>> fields, int numberOfParamsForSuperConstructor) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("\npublic %s(", simpleClassName));
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
        buf.append("}\n");
        return buf.toString();
    }
    
    public static String getPublicGetterMethod(String fieldType, String fieldName) {
        return String.format("  public %s %s() {\n   return %s;\n}\n", fieldType, StringTool.getGetterName(fieldName), fieldName);
    }
    
    public static String getPublicSetterMethod(String fieldType, String fieldName) {
        return String.format("  public void %s(%s %s) {\n   this.%s = %s;\n}\n", StringTool.getSetterName(fieldName), 
            fieldType, fieldName, fieldName, fieldName);
    }

    public static String getEqualsMethod(String simpleClassName, Map<String, Class<?>> fields) {
        StringBuilder buf = new StringBuilder();
        buf.append("\npublic boolean equals(Object obj) {\n");
        buf.append("    EqualsBuilder builder = new EqualsBuilder();\n");
        buf.append(String.format("    if (!(obj instanceof %s)) {;\n", simpleClassName));
        buf.append("        return false;\n");
        buf.append("    }\n");
        buf.append(String.format("    %s another = (%s)obj;\n", simpleClassName, simpleClassName));
        for (Entry<String, Class<?>> field : fields.entrySet()) {
            String getter = StringTool.getGetterName(field.getKey()) + "()";
            buf.append(String.format("    builder.append(another.%s,%s);\n", getter, getter));
        }
        buf.append("    return builder.isEquals();\n");
        buf.append("}\n");
        return buf.toString();
    }

    public static String getHashCodeMethod(Map<String, Class<?>> fields) {
        StringBuilder buf = new StringBuilder();
        buf.append("\npublic int hashCode() {\n");
        buf.append("    HashCodeBuilder builder = new HashCodeBuilder();\n");
        for (Entry<String, Class<?>> field : fields.entrySet()) {
            String getter = StringTool.getGetterName(field.getKey()) + "()";
            buf.append(String.format("    builder.append(%s);\n", getter));
        }
        buf.append("    return builder.toHashCode();\n");
        buf.append("}\n");
        return buf.toString();
    }

    public static String getToStringMethod(String simpleClassName, Map<String, Class<?>> fields) {
        StringBuilder buf = new StringBuilder();
        buf.append("\npublic String toString() {\n");
        buf.append("    StringBuilder builder = new StringBuilder();\n");
        buf.append(String.format("    builder.append(\"%s {\");\n", simpleClassName));
        for (Entry<String, Class<?>> field : fields.entrySet()) {
            buf.append(String.format("    builder.append(\" %s=\");\n", field.getKey()));
            String getter = StringTool.getGetterName(field.getKey()) + "()";
            if (field.getValue().isArray()) {
                buf.append(String.format("    builder.append(ArrayUtils.toString(%s));\n", getter));
            } else {
                buf.append(String.format("    builder.append(%s);\n", getter));
            }
        }
        buf.append("    builder.append(\" }\");\n");
        buf.append("    return builder.toString();\n");
        buf.append("}\n");
        return buf.toString();
    }

    public static String getOpenBracket() {
        return "{\n";
    }

    public static Object getDefaultFieldDeclaration(String fieldType, String fieldName) {
        return String.format("  %s %s;\n\n", fieldType, fieldName);
    }

    public static Object getStaticPublicFieldDeclaration(String fieldType, String fieldName) {
        return String.format("  public static %s %s;\n\n", fieldType, fieldName);
    }

    public static String getStaticPublicFieldInitialization(String fieldType, String fieldName, String initializationValue) {
        return String.format("  public static %s %s = %s;\n\n", fieldType, fieldName, initializationValue);
    }
    
    /**
     * 
     * @param name name of the class with package, symbol '/' is used as separator<br> 
     * (e.g. <code>my/test/TestClass</code>)
     * 
     * @return class name without package with <code>.java</code> suffix (e.g. <code>TestClass.java</code>)
     */
    public static String getClassFileName(String name) {
        String className = JavaClassGeneratorHelper.getShortClassName(name);

        return String.format("%s.java", className);
    }
    
    /**
     * Generate the Java type corresponding to the given type name.
     * Support array types.<br> 
     * (e.g. <code>my.test.Vehicle[][]</code>)
     * 
     * @param typeName name of the type (e.g. <code>my.test.TestClass</code>) 
     * @return Java type corresponding to the given type name. (e.g. <code>Lmy/test/TestClass;</code>)
     */
    public static String getJavaType(String typeName) {        
        boolean isArray = StringUtils.contains(typeName, "[");
        if (isArray) {
            return getJavaArrayType(typeName);
        } else {
            return String.format("L%s;", replaceCommas(typeName));
        }
    }
    
    /**
     * Gets the Java array type corresponding to income array type name.
     * 
     * @param arrayTypeName e.g. <code>my.test.TestClass[][]</code>
     * @return e.g. <code>[[Lmy/test/TestClass;</code>
     */
    public static String getJavaArrayType(String arrayTypeName) {   
        String[] tokens = arrayTypeName.split("\\[");
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i< tokens.length - 1; i++) {
            strBuf.append("[");
        }
        return String.format("%sL%s;", strBuf.toString(), replaceCommas(tokens[0]));          
    }
    
    public static String replaceCommas(String typeWithNamespace) {
        return typeWithNamespace.replace('.', '/');
    }
    
    public static Constructor<?> getBeanConstructorWithAllFields(Class<?> beanClass, int beanFieldsCount) {
        for (Constructor<?> constructor : beanClass.getConstructors()) {
            if (constructor.getParameterTypes().length == beanFieldsCount) {
                return constructor;
            }
        }
        return null;
    }
}
