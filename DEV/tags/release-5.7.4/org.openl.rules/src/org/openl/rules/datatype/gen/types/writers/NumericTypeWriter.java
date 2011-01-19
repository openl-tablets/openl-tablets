package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.FieldDescription;

public class NumericTypeWriter extends CommonTypeWriter {
    
    @Override
    public int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType) {
        codeVisitor.visitIntInsn(Constants.BIPUSH, ((Number)fieldType.getDefaultValue()).intValue());
        return 2;
    }

}
