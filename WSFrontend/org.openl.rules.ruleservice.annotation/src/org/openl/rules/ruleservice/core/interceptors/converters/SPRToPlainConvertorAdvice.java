package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Method;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.annotations.TypeResolver;
import org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType;
import org.openl.types.IOpenClass;

@UseOpenMethodReturnType(TypeResolver.IF_CSR_TO_PLAIN)
public class SPRToPlainConvertorAdvice extends AbstractServiceMethodAfterReturningAdvice<Object> implements IOpenClassAware {

    private XlsModuleOpenClass module;

    @Override
    public void setIOpenClass(IOpenClass openClass) {
        this.module = (XlsModuleOpenClass) openClass;
    }

    @Override
    public Object afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return SpreadsheetResult.convertSpreadsheetResults(module, result);
    }
}
