/**
 * 
 */
package org.openl.rules.tbasic;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class NoParamMethodField implements IOpenField {

    private String fieldName;
    private AlgorithmSubroutineMethod methodToInvoke;

    public NoParamMethodField(String theFieldName, AlgorithmSubroutineMethod theMethodToInvoke) {
        assert theMethodToInvoke.getSignature().getParameterTypes().length > 0;

        fieldName = theFieldName;
        methodToInvoke = theMethodToInvoke;
    }

    public Object get(Object target, IRuntimeEnv env) {
        return methodToInvoke.invoke(target, new Object[] {}, env);
    }

    public boolean isConst() {
        return false;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWritable() {
        return false;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException(String.format(
                "Set operation is not supported for method proxy field \"%s\"", fieldName));
    }

    public IOpenClass getDeclaringClass() {
        return methodToInvoke.getDeclaringClass();
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public IOpenClass getType() {
        return methodToInvoke.getType();
    }

    public boolean isStatic() {
        return methodToInvoke.isStatic();
    }

    public String getDisplayName(int mode) {
        return methodToInvoke.getDisplayName(mode);
    }

    public String getName() {
        return fieldName;
    }

}