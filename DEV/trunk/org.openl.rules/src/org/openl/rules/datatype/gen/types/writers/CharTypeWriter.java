package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.openl.rules.datatype.gen.FieldDescription;

public class CharTypeWriter extends CommonTypeWriter {
    
    @Override
    public int writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        methodVisitor.visitLdcInsn(fieldType.getDefaultValue());
        return 2;
    }
}
