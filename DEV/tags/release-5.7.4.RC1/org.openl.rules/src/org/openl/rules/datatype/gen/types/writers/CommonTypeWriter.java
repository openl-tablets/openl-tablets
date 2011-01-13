package org.openl.rules.datatype.gen.types.writers;

import org.objectweb.asm.CodeVisitor;
import org.objectweb.asm.Constants;
import org.openl.rules.datatype.gen.FieldDescription;

/**
 * Common type writer for byte, short, int, boolean, char classes.
 * 
 * @author DLiauchuk
 *
 */
public class CommonTypeWriter implements TypeWriter {

    public int getConstantForReturn() {
        return Constants.IRETURN;
    }

    public int getConstantForVarInsn() {
        return Constants.ILOAD;
    }

    public int writeFieldValue(CodeVisitor codeVisitor, FieldDescription fieldType) {
        throw new UnsupportedOperationException("This operation must be overloaded in childs");
    }
}
