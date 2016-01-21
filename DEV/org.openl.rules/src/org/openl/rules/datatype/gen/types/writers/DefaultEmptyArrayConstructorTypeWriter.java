package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * @author Marat Kamalov.
 */
public class DefaultEmptyArrayConstructorTypeWriter implements TypeWriter {

    public int getConstantForVarInsn() {
        return Opcodes.ALOAD;
    }

    public int getConstantForReturn() {
        return Opcodes.ARETURN;
    }

    public int writeFieldValue(MethodVisitor methodVisitor, FieldDescription field) {
        if (!field.getType().isArray()) {
            throw new IllegalArgumentException("Field type is not an array!");
        }

        Class<?> type = field.getType().getComponentType();
        if (!type.isArray()) { // one dim
            String internalName = Type.getInternalName(type);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalName);
        } else { // multi dim
            int level = 1;
            while (type.isArray()) {
                type = type.getComponentType();
                level++;
            }
            String internalName = Type.getDescriptor(field.getType());
            for (int i = 0; i < level; i++) {
                methodVisitor.visitInsn(Opcodes.ICONST_0);
            }
            methodVisitor.visitMultiANewArrayInsn(internalName, level);
        }

        return 5;
    }
}
