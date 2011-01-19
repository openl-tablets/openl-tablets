package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.FieldDescription;

public class FloatTypeWriter implements TypeWriter {

    public int getConstantForVarInsn() {
        return Constants.FLOAD;
    }

    public int getConstantForReturn() {
        return Constants.FRETURN;
    }

    public int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType) {
        codeVisitor.visitLdcInsn(fieldType.getDefaultValue());
        return 2;
    }

}
