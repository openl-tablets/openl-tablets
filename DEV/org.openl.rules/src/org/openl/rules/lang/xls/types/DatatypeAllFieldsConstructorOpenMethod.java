package org.openl.rules.lang.xls.types;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class DatatypeAllFieldsConstructorOpenMethod implements IOpenMethod, IMethodSignature {

    private final DatatypeOpenClass datatypeOpenClass;

    public DatatypeAllFieldsConstructorOpenMethod(DatatypeOpenClass openClass) {
        this.datatypeOpenClass = Objects.requireNonNull(openClass, "openClass cannot be null");
    }

    @Override
    public boolean isConstructor() {
        return true;
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public IMethodSignature getSignature() {
        return this;
    }

    @Override
    public IOpenClass getType() {
        return datatypeOpenClass;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return datatypeOpenClass;
    }

    @Override
    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    @Override
    public String getName() {
        return "<init>";
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        final Object newTarget = datatypeOpenClass.newInstance(env);
        int i = 0;
        for (IOpenField field : datatypeOpenClass.getFields()) {
            field.set(newTarget, params[i], env);
            i++;
        }
        return newTarget;
    }

    @Override
    public int getNumberOfParameters() {
        return datatypeOpenClass.getFields().size();
    }

    @Override
    public String getParameterName(int i) {
        return null;
    }

    @Override
    public IOpenClass getParameterType(int i) {
        Collection<IOpenField> fields = datatypeOpenClass.getFields();
        Iterator<IOpenField> itr = fields.iterator();
        int j = 0;
        while (itr.hasNext() && j < i) {
            j++;
            itr.next();
        }
        return itr.hasNext() ? itr.next().getType() : null;
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        return datatypeOpenClass.getFields().stream().map(IOpenField::getType).toArray(IOpenClass[]::new);
    }
}
