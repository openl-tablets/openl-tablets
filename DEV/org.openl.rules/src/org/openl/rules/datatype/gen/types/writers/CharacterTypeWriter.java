package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * A type writer for the String class.
 *
 * @author Yury Molchan
 */
public class CharacterTypeWriter extends ObjectTypeWriter {
    
    @Override
    public int writeFieldValue(MethodVisitor methodVisitor, FieldDescription field) {
        methodVisitor.visitLdcInsn(field.getDefaultValue());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        return 5;
    }
}
