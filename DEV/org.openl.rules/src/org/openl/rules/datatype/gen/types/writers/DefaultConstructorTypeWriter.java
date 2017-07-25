package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * Generates the byte code, that calls the default constructor
 * of the class {@link org.openl.rules.datatype.gen.DefaultFieldDescription#getType()}
 *
 * e.g. if the type of the field is org.openl.rules.Driver -> generates 'new org.openl.rules.Driver()'
 *
 * @author Denis Levchuk
 */
public class DefaultConstructorTypeWriter implements TypeWriter {

    public int getConstantForVarInsn() {
        return Opcodes.ALOAD;
    }

    public int getConstantForReturn() {
        return Opcodes.ARETURN;
    }

    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription field) {
        String internalName = Type.getInternalName(field.getType());
        methodVisitor.visitTypeInsn(Opcodes.NEW, internalName);
        methodVisitor.visitInsn(Opcodes.DUP);

        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>",
                "()V");
    }
}
