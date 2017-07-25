package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * Common type writer for byte, short, int, boolean, char classes.
 * 
 * @author DLiauchuk
 *
 */
public class CommonTypeWriter implements TypeWriter {

    public int getConstantForReturn() {
        return Opcodes.IRETURN;
    }

    public int getConstantForVarInsn() {
        return Opcodes.ILOAD;
    }

    public void writeFieldValue(MethodVisitor methodVisitor, FieldDescription fieldType) {
        throw new UnsupportedOperationException("This operation must be overloaded in childs");
    }
}
