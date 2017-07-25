package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * A type writer for the String class.
 *
 * @author Yury Molchan
 */
public class StringTypeWriter extends ObjectTypeWriter {
    
    @Override
    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription field) {
        methodVisitor.visitLdcInsn(field.getDefaultValue());
    }
}
