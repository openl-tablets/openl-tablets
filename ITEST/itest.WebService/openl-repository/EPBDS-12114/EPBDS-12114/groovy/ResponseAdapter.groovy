import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice

import java.lang.reflect.Method

class ResponseAdapter extends AbstractServiceMethodAfterReturningAdvice<Object> {
    @Override
    Object afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return ((SpreadsheetResult) result).getFieldValue('$RET')
    }
}
