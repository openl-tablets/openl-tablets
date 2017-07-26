package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * @author Marat Kamalov.
 */
public class DefaultEmptyArrayConstructorTypeWriter implements TypeWriter {

    private int findOptcodesForType(Class<?> type) {
        Type t = Type.getType(type);
        if (Type.BOOLEAN_TYPE.equals(t)) {
            return Opcodes.T_BOOLEAN;
        } else if (Type.BYTE_TYPE.equals(t)) {
            return Opcodes.T_BYTE;
        } else if (Type.CHAR_TYPE.equals(t)) {
            return Opcodes.T_CHAR;
        } else if (Type.DOUBLE_TYPE.equals(t)) {
            return Opcodes.T_DOUBLE;
        } else if (Type.FLOAT_TYPE.equals(t)) {
            return Opcodes.T_FLOAT;
        } else if (Type.INT_TYPE.equals(t)) {
            return Opcodes.T_INT;
        } else if (Type.LONG_TYPE.equals(t)) {
            return Opcodes.T_LONG;
        } else if (Type.SHORT_TYPE.equals(t)) {
            return Opcodes.T_SHORT;
        } 
        throw new IllegalStateException("Primitive type wasn't found for code generation!");
    }

    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription field) {
        if (!field.getType().isArray()) {
            throw new IllegalArgumentException("Field type is not an array!");
        }

        Class<?> type = field.getType().getComponentType();
        if (!type.isArray()) { // one dim
            String internalName = Type.getInternalName(type);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            if (type.isPrimitive()) {
                methodVisitor.visitIntInsn(Opcodes.NEWARRAY, findOptcodesForType(type));
            } else {
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalName);
            }
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
    }
}
