package org.openl.rules.datatype.gen;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;

public class ByteCodeGeneratorHelper {


    private ByteCodeGeneratorHelper() {}

    /**
     * Gets Java type corresponding to the given field type.<br>
     * 
     * @param field
     * @return Java type corresponding to the given field type. (e.g. <code>Lmy/test/TestClass;</code>)
     */
    public static String getJavaType(FieldDescription field) {
        if (field instanceof RecursiveFieldDescription) {
            return getJavaType(field.getTypeName());
        }
        Class<?> fieldClass = field.getType();
        /** gets the type by its class*/
        return Type.getDescriptor(fieldClass);
    }

    public static int getConstantForVarInsn(FieldDescription field) {
        Class<?> retClass = field.getType();
        Type type = Type.getType(retClass);
        return type.getOpcode(Opcodes.ILOAD);
    }

    /**
     * Generate the Java type corresponding to the given canonical type name.
     * Support array types.<br>
     * (e.g. <code>my.test.Vehicle[][]</code>)
     *
     * @param canonicalTypeName name of the type (e.g.
     *            <code>my.test.TestClass</code>)
     * @return Java type corresponding to the given type name. (e.g.
     *         <code>Lmy/test/TestClass;</code>)
     */
    public static String getJavaType(String canonicalTypeName) {
        if (canonicalTypeName != null && canonicalTypeName.indexOf('[') >= 0) {
            String[] tokens = canonicalTypeName.split("\\[");
            StringBuilder strBuf = new StringBuilder();
            for (int i = 0; i < tokens.length - 1; i++) {
                strBuf.append("[");
            }
            strBuf.append(getJavaTypeWithPrefix(tokens[0]));
            return strBuf.toString();
        } else {
            return getJavaTypeWithPrefix(canonicalTypeName);
        }
    }

    /**
     * Gets the corresponding java type name by the given canonical type
     * name(without array brackets).<br>
     * Supports primitives.
     *
     * @param canonicalTypeName name of the type (e.g.
     *            <code>my.test.TestClass</code>)
     * @return Java type corresponding to the given type name. (e.g.
     *         <code>Lmy/test/TestClass;</code>)
     */
    private static String getJavaTypeWithPrefix(String canonicalTypeName) {
        if (NumberUtils.isPrimitive(canonicalTypeName)) {
            if ("byte".equals(canonicalTypeName)) {
                return "B";
            } else if ("short".equals(canonicalTypeName)) {
                return "S";
            } else if ("int".equals(canonicalTypeName)) {
                return "I";
            } else if ("long".equals(canonicalTypeName)) {
                return "J";
            } else if ("float".equals(canonicalTypeName)) {
                return "F";
            } else if ("double".equals(canonicalTypeName)) {
                return "D";
            } else if ("boolean".equals(canonicalTypeName)) {
                return "Z";
            } else if ("char".equals(canonicalTypeName)) {
                return "C";
            }
        }
        if (StringUtils.isNotBlank(canonicalTypeName)) {
            return String.format("L%s;", canonicalTypeName.replace('.', '/'));
        }
        return StringUtils.EMPTY;
    }

}
