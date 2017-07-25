package org.openl.rules.calc.result.gen;

import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.table.Point;

/**
 * Creates default constructor for Custom Spreadsheet Result with constants for parent with height and width. <br>
 * Constructor pattern: <br>
 *  <code> public [SPREADSHEET_RESULT_NAME]() {super([HEIGHT], [WIDTH]);} </code> <br>
 *  
 * @author DLiauchuk
 *
 */
public class DefaultConstructorWriter extends org.openl.rules.datatype.gen.bean.writers.DefaultConstructorWriter {
    
    /** coordinates of mac column and row*/
    private Point maxColumnAndRow;
    
    public DefaultConstructorWriter(String beanNameWithPackage, Class<?> parentClass, Point maxColumnAndRow) {
        super(beanNameWithPackage, parentClass, new HashMap<String, FieldDescription>());
        this.maxColumnAndRow = maxColumnAndRow;
    }
    
    @Override
    public void write(ClassWriter classWriter) {
        // creates a MethodWriter for the (implicit) constructor
        MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        // pushes the 'this' variable
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        /** increments by 1 because max coordinates are started from 0*/
        mv.visitIntInsn(Opcodes.BIPUSH, maxColumnAndRow.getRow() + 1);
        mv.visitIntInsn(Opcodes.BIPUSH, maxColumnAndRow.getColumn() + 1);
        // invokes the super class constructor
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, getParentInternalName(), "<init>", "(II)V");
        
        mv.visitInsn(Opcodes.RETURN);
        
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

}
