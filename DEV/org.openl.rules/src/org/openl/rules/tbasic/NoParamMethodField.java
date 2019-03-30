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
        assert theMethodToInvoke.getSignature().getParameterTypes().length == 0;

        fieldName = theFieldName;
        methodToInvoke = theMethodToInvoke;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return methodToInvoke.invoke(target, new Object[] {}, env);
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return methodToInvoke.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return methodToInvoke.getDisplayName(mode);
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public IOpenClass getType() {
        return methodToInvoke.getType();
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isStatic() {
        return methodToInvoke.isStatic();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException(
            String.format("Set operation is not supported for method proxy field \"%s\"", fieldName));
    }

}