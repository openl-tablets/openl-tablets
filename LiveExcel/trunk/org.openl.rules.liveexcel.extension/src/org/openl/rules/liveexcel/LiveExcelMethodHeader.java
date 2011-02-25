package org.openl.rules.liveexcel;

import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;

import com.exigen.le.smodel.Function;

public class LiveExcelMethodHeader implements IOpenMethodHeader {

    private Function function;

    private IMethodSignature methodSignature;

    private IOpenClass declaringclass = null;

    private IOpenClass returnType;

    public LiveExcelMethodHeader(Function function, RulesModuleBindingContext bindingContext) {
        this.function = function;
        this.methodSignature = LiveExcelMethodSignatureBuilder.build(function, bindingContext);
        this.returnType = TypeUtils.getOpenClass(function.getReturnTypeName(), bindingContext);
    }

    public IMethodSignature getSignature() {
        return methodSignature;
    }

    public IOpenClass getDeclaringClass() {
        return declaringclass;
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public IOpenClass getType() {
        return returnType;
    }

    public boolean isStatic() {
        return false;
    }

    public String getDisplayName(int mode) {
        return TypeUtils.convertName(function.getDeclaredName());
    }

    public String getName() {
        return TypeUtils.convertName(function.getDeclaredName());
    }

}
