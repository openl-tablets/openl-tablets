package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.FieldDescription;

public class BooleanTypeWriter extends CommonTypeWriter {
    
    @Override
    public int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType) {
        codeVisitor.visitInsn(getValueForBoolean((Boolean)fieldType.getDefaultValue()));
        return 2;
    }
    
    private int getValueForBoolean(Boolean defaultValue) {
        if (defaultValue.equals(Boolean.TRUE)) {
            return Constants.ICONST_1;
        } 
        return Constants.ICONST_0;
    }
}
