package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.FieldDescription;

public class NumericTypeWriter extends CommonTypeWriter {
    
    @Override
    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, ((Number)fieldType.getDefaultValue()).intValue());
    }

}
