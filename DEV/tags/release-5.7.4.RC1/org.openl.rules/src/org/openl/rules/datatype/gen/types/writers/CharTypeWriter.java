package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.openl.rules.datatype.gen.FieldDescription;

public class CharTypeWriter extends CommonTypeWriter {
    
    @Override
    public int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType) {
        codeVisitor.visitLdcInsn(fieldType.getDefaultValue());
        return 2;
    }
}
