package org.openl.gen;

/**
 * Set of utilities used for the byte code generation
 *
 * @author Yury Molchan
 */
public final class ByteCodeUtils {

    /**
     * Creates a namespace string for the XML Bind annotations.
     */
    public static String getNamespace(String beanNameWithPackage) {
        String[] parts = beanNameWithPackage.split("/");
        StringBuilder builder = new StringBuilder("http://");
        for (int i = parts.length - 2; i >= 0; i--) {
            builder.append(parts[i]);
            if (i != 0) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    /**
     * Converts a Java type name to the JVM type descriptor.
     *
     * @see org.objectweb.asm.Type
     */
    public static String toTypeDescriptor(String typeName) {
        switch (typeName) {
            case "byte":
                return "B";
            case "short":
                return "S";
            case "int":
                return "I";
            case "long":
                return "J";
            case "float":
                return "F";
            case "double":
                return "D";
            case "boolean":
                return "Z";
            case "char":
                return "C";
            case "void":
                return "V";
            default:
                if (typeName.endsWith("[]")) {
                    // Canonical name like int[][]
                    var pos = typeName.indexOf('[');
                    return "[".repeat((typeName.length() - pos) / 2) + toTypeDescriptor(typeName.substring(0, pos));
                }
                String internal = typeName;
                if (typeName.charAt(0) != '[') {
                    // base name like java.lang.Object
                    internal = 'L' + internal + ';';
                }
                return internal.replace('.', '/');
        }
    }
}
