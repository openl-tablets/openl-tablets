package org.openl.util.generation;

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
    
    public static String getPrivateFieldDeclaration(String fieldType, String fieldName) {
        return String.format("  private %s %s;\n\n", fieldType, fieldName);
    }
    
    public static String getPublicGetterMethod(String fieldType, String fieldName) {
        return String.format("  public %s %s() {\n   return %s;\n}\n", fieldType, StringTool.getGetterName(fieldName), fieldName);
    }
    
    public static String getPublicSetterMethod(String fieldType, String fieldName) {
        return String.format("  public void %s(%s %s) {\n   this.%s = %s;\n}\n", StringTool.getSetterName(fieldName), 
            fieldType, fieldName, fieldName, fieldName);
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
}
