package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

public class ObjectTypeWriter implements TypeWriter {

    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription field) {
        // try to process object field with String constructor.
        Class<?> fieldClass = field.getType();
        String fieldinternalName = Type.getInternalName(fieldClass);
        methodVisitor.visitTypeInsn(Opcodes.NEW, fieldinternalName); 
        methodVisitor.visitInsn(Opcodes.DUP);

        String value = updateValue(field);
        methodVisitor.visitLdcInsn(value);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, fieldinternalName, "<init>", 
            "(Ljava/lang/String;)V"); 
    }

    protected String updateValue(FieldDescription fieldType) {
        return String.valueOf(fieldType.getDefaultValue());
    }
}
