package org.openl.rules.calc.result.gen;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.table.Point;
import org.openl.util.NumberUtils;
import org.openl.util.generation.JavaClassGeneratorHelper;

/**
 * Writes the setters for the Custom Spreadsheet Results.
 * 
 * Setter pattern: <br>
 *  <code> public void set[FIELD_NAME]([FIELD_TYPE] localName) { <br>
 *              Point p = new Point([FIELD_COLUMN], [FIELD_ROW]); <br>
 *              addFieldCoordinates([FIELD_NAME], p); <br>      
 *              setValue(p.getRow(), p.getColumn(), value); <br>
 *          } </code>
 * 
 * 
 * @author DLiauchuk
 *
 */
public class SettersWriter extends org.openl.rules.datatype.gen.bean.writers.SettersWriter {
    
    private static final String ADD_FIELD_COORDINATES_METHOD = "addFieldCoordinates";

    private static final String SUPER_CLASS_SETTER_METHOD = "setValue";
    
    private Map<String, Point> fieldCoordinates;
    
    public SettersWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields, 
            Map<String, Point> fieldCoordinates) {
        super(beanNameWithPackage, allFields);  
        this.fieldCoordinates = new HashMap<String, Point>(fieldCoordinates);
    }

    @Override
    protected void generateSetter(String beanNameWithPackage, ClassWriter classWriter,  Entry<String, FieldDescription> field) {
        String fieldName = field.getKey();
        FieldDescription fieldType = field.getValue(); 
        
        MethodVisitor methodVisitor;
        
        /** if the field is a primitive, its type should be changed for wrapper type
         *  as the method SUPER_CLASS_SETTER is expecting the Object.
         */
        if (fieldType.getType().isPrimitive()) {
            fieldType = new FieldDescription(NumberUtils.getWrapperType(fieldType.getCanonicalTypeName()));
        }
        
        methodVisitor = writeMethodSignature(classWriter, fieldType, fieldName);
        
        methodVisitor.visitTypeInsn(Opcodes.NEW, JavaClassGeneratorHelper.replaceDots(Point.class.getCanonicalName()));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, fieldCoordinates.get(fieldName).getColumn());
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, fieldCoordinates.get(fieldName).getRow());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, JavaClassGeneratorHelper.replaceDots(Point.class.getCanonicalName()), 
            "<init>", "(II)V");
        methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
        
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitLdcInsn(fieldName);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, beanNameWithPackage, ADD_FIELD_COORDINATES_METHOD, 
            "(Ljava/lang/String;Lorg/openl/rules/table/Point;)V");
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JavaClassGeneratorHelper.replaceDots(Point.class.getCanonicalName()), 
            "getRow", "()I");
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JavaClassGeneratorHelper.replaceDots(Point.class.getCanonicalName()), 
            "getColumn", "()I");
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, beanNameWithPackage, SUPER_CLASS_SETTER_METHOD, "(IILjava/lang/Object;)V");
        
        methodVisitor.visitInsn(Opcodes.RETURN);
        
        methodVisitor.visitMaxs(4, 3);
        
        methodVisitor.visitEnd();
    }
}
