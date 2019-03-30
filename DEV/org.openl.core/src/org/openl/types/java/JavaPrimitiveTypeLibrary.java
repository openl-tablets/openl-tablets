/*
 * Created on Jun 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 *
 */
public class JavaPrimitiveTypeLibrary implements ITypeLibrary {

    static final Map<String, JavaOpenClass> classMap;

    static {
        classMap = new HashMap<String, JavaOpenClass>();
        classMap.put("int", JavaOpenClass.INT);
        classMap.put("long", JavaOpenClass.LONG);
        classMap.put("char", JavaOpenClass.CHAR);
        classMap.put("short", JavaOpenClass.SHORT);
        classMap.put("byte", JavaOpenClass.BYTE);
        classMap.put("double", JavaOpenClass.DOUBLE);
        classMap.put("float", JavaOpenClass.FLOAT);
        classMap.put("boolean", JavaOpenClass.BOOLEAN);
        classMap.put("void", JavaOpenClass.VOID);
    }
    public IOpenClass getType(String typename) {
        return classMap.get(typename);
    }
}
