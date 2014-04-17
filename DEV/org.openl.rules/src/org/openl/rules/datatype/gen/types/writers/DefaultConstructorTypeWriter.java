package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * Created by dl on 4/15/14.
 */
public class DefaultConstructorTypeWriter implements TypeWriter {

    public int getConstantForVarInsn() {
        return Opcodes.ALOAD;
    }

    public int getConstantForReturn() {
        return Opcodes.ARETURN;
    }

    public int writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        Class<?> fieldClass = FieldDescription.getJavaClass(fieldType);
        String fieldinternalName = Type.getInternalName(fieldClass);
        methodVisitor.visitTypeInsn(Opcodes.NEW, fieldinternalName);
        methodVisitor.visitInsn(Opcodes.DUP);

        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, fieldinternalName, "<init>",
                "()V");
        return 5;
    }
}
