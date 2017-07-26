package org.openl.rules.datatype.gen;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.rules.datatype.gen.types.writers.*;
import org.openl.types.IOpenField;
import org.openl.util.NumberUtils;
import org.openl.util.StringUtils;
import org.openl.util.generation.JavaClassGeneratorHelper;

public class ByteCodeGeneratorHelper {
    
    public static final String JAVA_LANG_OBJECT = "java/lang/Object";
    
    private static Map<Class<?>, TypeWriter> typeWriters = new HashMap<Class<?>, TypeWriter>();
    
    static {
        typeWriters.put(byte.class, new NumericTypeWriter());
        typeWriters.put(short.class, new NumericTypeWriter());
        typeWriters.put(int.class, new NumericTypeWriter());
        typeWriters.put(boolean.class, new BooleanTypeWriter());
        typeWriters.put(char.class, new CharTypeWriter());
        typeWriters.put(Character.class, new CharacterTypeWriter());

        typeWriters.put(long.class, new LongTypeWriter());
        typeWriters.put(float.class, new FloatTypeWriter());
        typeWriters.put(double.class, new DoubleTypeWriter());
        typeWriters.put(Date.class, new DateTypeWriter());
        typeWriters.put(String.class, new StringTypeWriter());
        typeWriters.put(Object.class, new ObjectTypeWriter());
    }
    
    private ByteCodeGeneratorHelper() {}

    /**
     * Gets Java type corresponding to the given field type.<br>
     * 
     * @param field
     * @return Java type corresponding to the given field type. (e.g. <code>Lmy/test/TestClass;</code>)
     */
    public static String getJavaType(FieldDescription field) {
        if (field instanceof RecursiveFieldDescription) {
            return getJavaType(field.getCanonicalTypeName());
        }
        Class<?> fieldClass = field.getType();
        /** gets the type by its class*/
        return Type.getDescriptor(fieldClass);
    }

    public static TypeWriter getTypeWriter(FieldDescription field) {
        if (field.hasDefaultKeyWord()) {
           if (field.getType().isArray()){
               return new DefaultEmptyArrayConstructorTypeWriter();
           }else{
               return new DefaultConstructorTypeWriter();
           }
        }
        Class<?> clazz = field.getType();
        return getTypeWriter(clazz);
    }
    
    private static TypeWriter getTypeWriter(Class<?> clazz) {
        TypeWriter typeWriter = typeWriters.get(clazz);
        if (typeWriter == null && clazz instanceof Object) {
            return typeWriters.get(Object.class);
        } else  {
            return typeWriter;
        }
    }
    
    public static int getConstantForVarInsn(FieldDescription field) {
        Class<?> retClass = field.getType();
        return getConstantForVarInsn(retClass);
    }

    public static int getConstantForVarInsn(Class<?> fieldClass) {
        TypeWriter typeWriter = getTypeWriter(fieldClass);
        if (typeWriter != null){
            return typeWriter.getConstantForVarInsn();
        } 
        return 0;
    }
    
    public static String getMethodSignatureForByteCode(Map<String, FieldDescription> params, Class<?> returnType){
        StringBuilder signatureBuilder = new StringBuilder("(");
        for (Map.Entry<String, FieldDescription> field : params.entrySet()) {
            String javaType = ByteCodeGeneratorHelper.getJavaType(field.getValue());
            signatureBuilder.append(javaType);
        }
        signatureBuilder.append(")");
        if(returnType == null){
            signatureBuilder.append("V");
        }else{
            signatureBuilder.append(Type.getDescriptor(returnType));
        }
        return signatureBuilder.toString();
    }
    
    public static Map<String, FieldDescription> convertFields(Map<String, IOpenField> fieldsToConvert) {
        LinkedHashMap<String, FieldDescription> fields = new LinkedHashMap<String, FieldDescription>();
        for (Entry<String, IOpenField> field : fieldsToConvert.entrySet()) {
            fields.put(field.getKey(), new DefaultFieldDescription(field.getValue()));
        }
        return fields;
    }
    
    public static int getConstantForReturn(FieldDescription field) {
        Class<?> retClass = field.getType();
        return getConstantForReturn(retClass);
    }
    
    /**
     * Returns the constant for return type. Each primitive type has its constant.
     * 
     * @param fieldClass
     * @return
     */
    public static int getConstantForReturn(Class<?> fieldClass) {
        TypeWriter typeWriter = getTypeWriter(fieldClass);
        if (typeWriter != null) {
            return typeWriter.getConstantForReturn();
        } 
        return 0;
    }

    public static String getSignature(Class<?> methodOwner, String methodName, Class<?>[] paramTypes) {
        Method matchingMethod = MethodUtil.getMatchingAccessibleMethod(methodOwner, methodName, paramTypes);
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append('(');
        for(Class<?> paramType : matchingMethod.getParameterTypes()){
            signatureBuilder.append(Type.getDescriptor(paramType));
        }
        signatureBuilder.append(')');
        signatureBuilder.append(Type.getDescriptor(matchingMethod.getReturnType()));
        return signatureBuilder.toString();
    }
    
    public static void invokeStatic(MethodVisitor methodVisitor, Class<?> methodOwner, String methodName, Class<?>[] paramTypes) {        
        String signatureBuilder = getSignature(methodOwner, methodName, paramTypes);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(methodOwner), methodName, signatureBuilder);
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
        if (JavaClassGeneratorHelper.isArray(canonicalTypeName)) {
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
            return String.format("L%s;", JavaClassGeneratorHelper.replaceDots(canonicalTypeName));
        }
        return StringUtils.EMPTY;
    }
}
