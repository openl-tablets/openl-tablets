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
import org.openl.rules.datatype.gen.types.writers.BooleanTypeWriter;
import org.openl.rules.datatype.gen.types.writers.CharTypeWriter;
import org.openl.rules.datatype.gen.types.writers.DateTypeWriter;
import org.openl.rules.datatype.gen.types.writers.DefaultConstructorTypeWriter;
import org.openl.rules.datatype.gen.types.writers.DefaultEmptyArrayConstructorTypeWriter;
import org.openl.rules.datatype.gen.types.writers.DoubleTypeWriter;
import org.openl.rules.datatype.gen.types.writers.FloatTypeWriter;
import org.openl.rules.datatype.gen.types.writers.LongTypeWriter;
import org.openl.rules.datatype.gen.types.writers.NumericTypeWriter;
import org.openl.rules.datatype.gen.types.writers.ObjectTypeWriter;
import org.openl.rules.datatype.gen.types.writers.StringTypeWriter;
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
            return JavaClassGeneratorHelper.getJavaType(field.getCanonicalTypeName());
        }
        Class<?> fieldClass = field.getType();
        /** gets the type by its class*/
        return ByteCodeGeneratorHelper.getJavaType(fieldClass);
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
    
    public static TypeWriter getTypeWriter(FieldDescription field) {
        if (field.hasDefaultKeyWord()) {
           return new DefaultConstructorTypeWriter();
        }
        if (field.hasEmptyKeyWord() && field.getType().isArray()){
            return new DefaultEmptyArrayConstructorTypeWriter();  
        }
        Class<?> clazz = field.getType();
        return getTypeWriter(clazz);
    }
    
    public static TypeWriter getTypeWriter(Class<?> clazz) {
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
        TypeWriter typeWriter = ByteCodeGeneratorHelper.getTypeWriter(fieldClass);        
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
        for (FieldDescription field : fields.values()) {
            if (long.class.equals(field.getType()) || double.class.equals(field.getType())) {
                twoStackElementsCount++;
            }
        }
        return twoStackElementsCount;
    }

}
