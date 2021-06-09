import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice
import org.openl.rules.ruleservice.core.interceptors.annotations.NotConvertor

@NotConvertor
class NoConvertorServiceMethodAfterAdvice extends AbstractServiceMethodAfterReturningAdvice<Object> {
    @Override
    Object afterReturning(Method interfaceMethod, Object result, Object... args) {
        return result;
    }
}
