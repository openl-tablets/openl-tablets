package org.openl.rules.calc.result.gen;

import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.bean.writers.MethodWriter;
import org.openl.util.NumberUtils;
import org.openl.util.generation.JavaClassGeneratorHelper;

/**
 * Writes byte code to the given classWriter.<br> 
 * Adds the decorator functions for the given fields. Call the provided method with the field name as the String argument.<br>
 * Consider that the provided method will return <code>Object</code>. Casts the result to the type of the field.<br><br>
 * 
 * Example: fieldForDecorating: key = <code>myField</code>, value = <code>DoubleValue.class</code>. prefixForDecorator = <code>get</code><br>
 * nameOfTheMethodToCall = <code>getFieldValue</code>
 * as a result of calling {@link #generateDecorator(ClassWriter, String, Entry)} will be <br>
 *      <code>public String getmyField() {
 *                  return (String)getFieldValue("myField");
 *            }</code>
 *  
 * 
 * @author DLiauchuk
 *
 */
public class DecoratorMethodWriter extends MethodWriter {
    
    private static final String COMMENT_SYMBOLS = "//";
    private static final String SPACE_SYMBOL = " ";

    /**
     * Constructor for the DecoratorMethodWriter
     * 
     * @param beanNameWithPackage name of the bean with package, e.g. my/test/ClassTest
     * @param fieldsForDecorating fields that will be decorated with new function
     * @param nameOfTheMethodToCall name of the method that should be called in decorator implementation
     * @param prefixForDecorator prefix for newly created decorator methods
     */
    public DecoratorMethodWriter(String beanNameWithPackage, Map<String, FieldDescription> fieldsForDecorating, 
            String nameOfTheMethodToCall, String prefixForDecorator) {
        super(beanNameWithPackage, fieldsForDecorating);
        this.nameOfTheMethodToCall = nameOfTheMethodToCall;
        this.prefixForDecorator = prefixForDecorator;
    }

    private String nameOfTheMethodToCall;
    private String prefixForDecorator;

    @Override
    public void write(ClassWriter classWriter) {
        for(Map.Entry<String, FieldDescription> field : getAllFields().entrySet()) {
            /** ignore those fields that are of void type. In java it is impossible
                but possible in Openl, e.g. spreadsheet cell with void type.*/
            if (!field.getValue().getCanonicalTypeName().equals("void")) {
                generateDecorator(classWriter, getBeanNameWithPackage(), field);
            }          
        }
    }

    private void generateDecorator(ClassWriter classWriter, String beanNameWithPackage, Entry<String, FieldDescription> field) {
        MethodVisitor methodVisitor;
        String fieldName = field.getKey();
        
        /** Generate methods only for fields without restricted symbols.
            In future should be updated to use this fields too somehow*/
        if (!containRestrictedSymbols(fieldName)) {
            FieldDescription fieldType = field.getValue();
            
            /** create method name for decorator*/
            String methodName = String.format("%s%s", prefixForDecorator, fieldName);
                    
            methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,  methodName, String.format("()%s",
                ByteCodeGeneratorHelper.getJavaType(fieldType)), null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            
            methodVisitor.visitLdcInsn(fieldName);
            
            /** call method **/
            ByteCodeGeneratorHelper.invokeVirtual(methodVisitor, SpreadsheetResult.class, nameOfTheMethodToCall, new Class[]{String.class});
            
            String typeNameForCast = null;
            
            /** need to perform special processing for type name for cast operation*/
            typeNameForCast = getTypeNameForCast(fieldType);
            
            /** perform cast to the field type*/
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, typeNameForCast);        
            
            /** if the field is primitive, the cast to wrapper type should be performed, and call for instance intValue() for return*/
            if (fieldType.getType() != null && fieldType.getType().isPrimitive()) {
                String nameOftheWrapperMethod = String.format("%sValue", fieldType.getCanonicalTypeName());
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(NumberUtils.getWrapperType(fieldType.getCanonicalTypeName())), 
                    nameOftheWrapperMethod, String.format("()%s", ByteCodeGeneratorHelper.getJavaType(fieldType)));            
            }
            
            methodVisitor.visitInsn(ByteCodeGeneratorHelper.getConstantForReturn(fieldType.getType()));        
            methodVisitor.visitMaxs(2, 1);
            methodVisitor.visitEnd();
        }
    }
    
    private boolean containRestrictedSymbols(String fieldName) {
        if (fieldName.contains(SPACE_SYMBOL) || fieldName.contains(COMMENT_SYMBOLS)) {
            return true;
        }
        return false;
    }
    
    /**
     * Gets the type name for the cast. Not general helper method because contains custom logic for
     * current implementation.<br>
     * 
     * Algorithm:<br>
     * 1) If the name of the field type is a single primitive (not an array), use wrapper type name for return.<br>
     * 2) If the name of the field is an array, return type name for cast using next pattern: <code>[Lmy/test/JavaClass;</code> <br>
     * 3) If the name of the field is a single, return type name for cast will be <code>my/test/JavaClass</code><br>
     *  
     */
    public static String getTypeNameForCast(FieldDescription fieldType) {
        /** representation of the type name in canonical view (See java specification), e.g. my.test.JavaClass, my.test.JavaClass[]*/
        String fieldCanonicalTypeName = fieldType.getCanonicalTypeName();
        
        if (fieldType.getType() != null && fieldType.getType().isPrimitive()) {
            Class<?> wrapperType = NumberUtils.getWrapperType(fieldCanonicalTypeName);
            fieldCanonicalTypeName = wrapperType.getCanonicalName();
        }
        
        String typeName;
        if (JavaClassGeneratorHelper.isArray(fieldCanonicalTypeName)) {
            /** when the type is array, should be returned <code>[Lmy/test/JavaClass;</code>*/
            typeName = JavaClassGeneratorHelper.getJavaType(fieldCanonicalTypeName);
        } else {
            /** when simple type, should be returned <code>my/test/JavaClass</code>*/
            typeName = JavaClassGeneratorHelper.replaceDots(fieldCanonicalTypeName);
        }
        return typeName;
    }    
}
