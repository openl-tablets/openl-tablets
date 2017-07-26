package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.FieldDescription;

public class LongTypeWriter implements TypeWriter {

    public int getConstantForVarInsn() {
        return Opcodes.LLOAD;
    }

    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        methodVisitor.visitLdcInsn(fieldType.getDefaultValue());
    }

}
