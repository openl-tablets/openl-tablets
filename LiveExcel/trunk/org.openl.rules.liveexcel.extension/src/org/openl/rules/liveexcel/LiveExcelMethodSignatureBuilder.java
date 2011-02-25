package org.openl.rules.liveexcel;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.types.IMethodSignature;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;

import com.exigen.le.smodel.Function;

public class LiveExcelMethodSignatureBuilder {
    
    public static IMethodSignature build(Function function, RulesModuleBindingContext bindingContext){
        ParameterDeclaration[] parameterDeclarations = new ParameterDeclaration[function.getArguments().size()];
        for(int i = 0; i < parameterDeclarations.length; i ++) {
            parameterDeclarations[i] = new ParameterDeclaration(TypeUtils.getOpenClass(function.getArguments().get(i), bindingContext),getParameterName(function, i));
        }
        return new MethodSignature(parameterDeclarations);
    }
    
    private static String getParameterName(Function function, int i) {
        String paramName = TypeUtils.convertName(function.getArguments().get(i).getDescription());
        if (StringUtils.isBlank(paramName)) {
            return "param" + i;
        }
        return paramName;
    }

}
