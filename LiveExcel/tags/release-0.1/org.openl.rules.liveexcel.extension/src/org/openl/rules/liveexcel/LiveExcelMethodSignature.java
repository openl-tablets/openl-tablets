package org.openl.rules.liveexcel;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.liveexcel.formula.ParsedDeclaredFunction;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

public class LiveExcelMethodSignature implements IMethodSignature {
    
    private ParsedDeclaredFunction declaredFunction;
    
    public LiveExcelMethodSignature(ParsedDeclaredFunction declaredFunction) {
        this.declaredFunction = declaredFunction;
    }

    public int getNumberOfArguments() {
        return declaredFunction.getParameters().size();
    }

    public int getParameterDirection(int i) {
        return IParameterDeclaration.IN;
    }

    public String getParameterName(int i) {
        String paramName = TypeUtils.convertName(declaredFunction.getParameters().get(i).getParamName());
        if (StringUtils.isBlank(paramName)) {
            return "param" + i;
        }
        return paramName;
    }

    public IOpenClass[] getParameterTypes() {
        IOpenClass[] params = new IOpenClass[declaredFunction.getParameters().size()];
        for (int i = 0; i < params.length; i ++) {
            params[i] = TypeUtils.getParameterClass(declaredFunction.getParameters().get(i));
        }
        return params;
    }

}
