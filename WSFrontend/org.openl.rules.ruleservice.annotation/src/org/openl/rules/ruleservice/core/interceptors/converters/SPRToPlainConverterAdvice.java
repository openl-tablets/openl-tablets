package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Method;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.interceptors.annotations.TypeResolver;
import org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType;

@UseOpenMethodReturnType(TypeResolver.IF_SPR_TO_PLAIN)
public class SPRToPlainConverterAdvice extends AbstractSPRToPlainConverterAdvice<Object> {

    @Override
    public Object afterReturning(Method interfaceMethod, Object result, Object... args) {
        return SpreadsheetResult.convertSpreadsheetResult(getModule(), result, getConvertToType());
    }
}
