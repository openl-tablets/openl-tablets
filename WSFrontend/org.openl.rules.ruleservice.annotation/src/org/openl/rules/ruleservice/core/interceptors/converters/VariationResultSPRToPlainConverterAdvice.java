package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.variation.VariationsResult;
import org.openl.types.IOpenClass;

public class VariationResultSPRToPlainConverterAdvice extends AbstractServiceMethodAfterReturningAdvice<VariationsResult<Object>> implements IOpenClassAware {

    private XlsModuleOpenClass module;

    @Override
    public void setIOpenClass(IOpenClass openClass) {
        this.module = (XlsModuleOpenClass) openClass;
    }

    @Override
    public VariationsResult<Object> afterReturning(Method interfaceMethod,
            Object result,
            Object... args) throws Exception {
        VariationsResult<SpreadsheetResult> variationsResult = (VariationsResult<SpreadsheetResult>) result;
        VariationsResult<Object> ret = new VariationsResult<>();

        for (Map.Entry<String, SpreadsheetResult> entry : variationsResult.getVariationResults().entrySet()) {
            ret.registerResult(entry.getKey(), SpreadsheetResult.convertSpreadsheetResults(module, entry.getValue()));
        }

        for (Map.Entry<String, String> entry : variationsResult.getVariationFailures().entrySet()) {
            ret.registerFailure(entry.getKey(), entry.getValue());
        }

        return ret;
    }
}