package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.openl.rules.datatype.gen.FieldDescription;

public interface TypeWriter {
    
    int getConstantForVarInsn();
    
    int getConstantForReturn();

    int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType);

}
