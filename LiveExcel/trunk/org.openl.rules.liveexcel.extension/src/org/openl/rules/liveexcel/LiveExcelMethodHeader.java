package org.openl.rules.liveexcel;

import org.openl.rules.liveexcel.formula.ParsedDeclaredFunction;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;

public class LiveExcelMethodHeader implements IOpenMethodHeader {

    private ParsedDeclaredFunction declaredFunction;

    private IMethodSignature methodSignature;

    private IOpenClass clazz;

    public LiveExcelMethodHeader(ParsedDeclaredFunction declaredFunction, IOpenClass clazz) {
        this.declaredFunction = declaredFunction;
        methodSignature = new LiveExcelMethodSignature(declaredFunction);
        this.clazz = clazz;
    }

    public IMethodSignature getSignature() {
        return methodSignature;
    }

    public IOpenClass getDeclaringClass() {
        return clazz;
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public IOpenClass getType() {
        return TypeUtils.getParameterClass(declaredFunction.getReturnCell());
    }

    public boolean isStatic() {
        return false;
    }

    public String getDisplayName(int mode) {
        return TypeUtils.convertName(declaredFunction.getDeclFuncName());
    }

    public String getName() {
        return TypeUtils.convertName(declaredFunction.getDeclFuncName());
    }

}
