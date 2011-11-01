package org.openl.rules.datatype.gen;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.rules.datatype.gen.types.writers.BooleanTypeWriter;
import org.openl.rules.datatype.gen.types.writers.CharTypeWriter;
import org.openl.rules.datatype.gen.types.writers.DoubleTypeWriter;
import org.openl.rules.datatype.gen.types.writers.FloatTypeWriter;
import org.openl.rules.datatype.gen.types.writers.LongTypeWriter;
import org.openl.rules.datatype.gen.types.writers.NumericTypeWriter;
import org.openl.rules.datatype.gen.types.writers.ObjectTypeWriter;
import org.openl.rules.datatype.gen.types.writers.TypeWriter;
import org.openl.types.IOpenField;
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
        
        typeWriters.put(long.class, new LongTypeWriter());
        typeWriters.put(float.class, new FloatTypeWriter());
        typeWriters.put(double.class, new DoubleTypeWriter());
        typeWriters.put(Object.class, new ObjectTypeWriter());
    }
    
    private ByteCodeGeneratorHelper() {}

    /**
     * Gets Java type corresponding to the given field type.<br>
     * The algorithm depends on the existence of a class object for given field. If no, 
     * it means we are working with datatype (there is no already generated java class).
     * 
     * @param fieldType
     * @return Java type corresponding to the given field type. (e.g. <code>Lmy/test/TestClass;</code>)
     */
    public static String getJavaType(FieldDescription fieldType) {
        Class<?> fieldClass = fieldType.getType();
        if (fieldClass != null) {
            /** gets the type by its class*/
            return ByteCodeGeneratorHelper.getJavaType(fieldClass);
        } else {
            /** gets the type by the canonical name of the class*/
            return JavaClassGeneratorHelper.getJavaType(fieldType.getCanonicalTypeName());
        }
    }

    /**
     * Returns the Java type corresponding to the given class.
     * 
     * @param fieldClass
     * @return the Java type corresponding to the given class.
     */
    public static String getJavaType(Class<?> fieldClass) {
        return String.valueOf(Type.getType(fieldClass));
    }
    
    public static TypeWriter getTypeWriter(FieldDescription fieldType) {
        Class<?> javaFieldClass = FieldDescription.getJavaClass(fieldType);
        return getTypeWriter(javaFieldClass);
    }
    
    public static TypeWriter getTypeWriter(Class<?> fieldClass) {
        TypeWriter typeWriter = typeWriters.get(fieldClass);
        if (typeWriter == null && fieldClass instanceof Object) {
            return typeWriters.get(Object.class);
        } else  {
            return typeWriter;
        }
    }
    
    public static int getConstantForVarInsn(FieldDescription fieldType) {
        Class<?> retClass = fieldType.getType();
        if (retClass != null) {
            return getConstantForVarInsn(retClass);
        } else {
            return Opcodes.ALOAD;
        }
    }

    public static int getConstantForVarInsn(Class<?> fieldClass) {
        TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(fieldClass);
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
            signatureBuilder.append(ByteCodeGeneratorHelper.getJavaType(returnType));
        }
        return signatureBuilder.toString();
    }
    
    public static Map<String, FieldDescription> convertFields(Map<String, IOpenField> fieldsToConvert) {
        LinkedHashMap<String, FieldDescription> fields = new LinkedHashMap<String, FieldDescription>();
        for (Entry<String, IOpenField> field : fieldsToConvert.entrySet()) {
            fields.put(field.getKey(), new FieldDescription(field.getValue()));
        }
        return fields;
    }
    
    public static int getConstantForReturn(FieldDescription fieldType) {
        Class<?> retClass = fieldType.getType();
        if (retClass != null) {
            return getConstantForReturn(retClass);
        } else {
            return Opcodes.ARETURN;
        }
    }
    
    /**
     * Returns the constant for return type. Each primitive type has its constant.
     * 
     * @param fieldClass
     * @return
     */
    public static int getConstantForReturn(Class<?> fieldClass) {
        TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(fieldClass);        
        if (typeWriter != null) {
            return typeWriter.getConstantForReturn();
        } 
        return 0;
    }
    
    public static void invokeVirtual(MethodVisitor methodVisitor, Class<?> methodOwner, String methodName, Class<?>[] paramTypes) {
        String signatureBuilder = getSignature(methodOwner, methodName, paramTypes);        
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(methodOwner), methodName, signatureBuilder);
    }
    
    public static String getSignature(Class<?> methodOwner, String methodName, Class<?>[] paramTypes) {
        Method matchingMethod = MethodUtil.getMatchingAccessibleMethod(methodOwner, methodName, paramTypes, false);
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append('(');
        for(Class<?> paramType : matchingMethod.getParameterTypes()){
            signatureBuilder.append(ByteCodeGeneratorHelper.getJavaType(paramType));
        }
        signatureBuilder.append(')');
        signatureBuilder.append(ByteCodeGeneratorHelper.getJavaType(matchingMethod.getReturnType()));
        return signatureBuilder.toString();
    }
    
    public static void invokeStatic(MethodVisitor methodVisitor, Class<?> methodOwner, String methodName, Class<?>[] paramTypes) {        
        String signatureBuilder = getSignature(methodOwner, methodName, paramTypes);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(methodOwner), methodName, signatureBuilder);
    }
    
    public static int getTwoStackElementFieldsCount(Map<String, FieldDescription> fields) {
        int twoStackElementsCount = 0;
        for (FieldDescription fieldType : fields.values()) {
            if (long.class.equals(fieldType.getType()) || double.class.equals(fieldType.getType())) {
                twoStackElementsCount++;
            }
        }
        return twoStackElementsCount;
    }

}
