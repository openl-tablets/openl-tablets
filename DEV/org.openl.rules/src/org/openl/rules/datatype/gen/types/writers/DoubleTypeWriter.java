package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.openl.rules.datatype.gen.FieldDescription;

public class DoubleTypeWriter implements TypeWriter {

    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        methodVisitor.visitLdcInsn(fieldType.getDefaultValue()); 
    }

}
