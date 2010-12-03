package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

public class ObjectTypeWriter implements TypeWriter {

    @Override
    public int getConstantForVarInsn() {        
        return Constants.ALOAD;
    }

    @Override
    public int getConstantForReturn() {
        return Constants.ARETURN;
    }

    @Override
    public int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType) {
        // try to process object field with String constructor.
        Class<?> fieldClass = FieldDescription.getJavaClass(fieldType);
        String fieldinternalName = Type.getInternalName(fieldClass);
        codeVisitor.visitTypeInsn(Constants.NEW, fieldinternalName); 
        codeVisitor.visitInsn(Constants.DUP);
        codeVisitor.visitLdcInsn(fieldType.getDefaultValueAsString());
        codeVisitor.visitMethodInsn(Constants.INVOKESPECIAL, fieldinternalName, "<init>", 
            "(Ljava/lang/String;)V"); 
        return 5;
    }

}
