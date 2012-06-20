package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

public class ObjectTypeWriter implements TypeWriter {

    public int getConstantForVarInsn() {        
        return Opcodes.ALOAD;
    }

    public int getConstantForReturn() {
        return Opcodes.ARETURN;
    }

    public int writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        // try to process object field with String constructor.
        Class<?> fieldClass = FieldDescription.getJavaClass(fieldType);
        String fieldinternalName = Type.getInternalName(fieldClass);
        methodVisitor.visitTypeInsn(Opcodes.NEW, fieldinternalName); 
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(fieldType.getDefaultValueAsString());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, fieldinternalName, "<init>", 
            "(Ljava/lang/String;)V"); 
        return 5;
    }

}
