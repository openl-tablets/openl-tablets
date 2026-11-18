package owner

import java.lang.reflect.Method

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice

/**
 * Answers the project whose Spring configuration created the context of the service, or the result of the rule when
 * the service has no project Spring configuration.
 */
class OwnerInterceptor extends AbstractServiceMethodAfterReturningAdvice<String> {

    @Autowired(required = false)
    @Qualifier("springConfigOwner")
    String springConfigOwner

    @Override
    String afterReturning(Method interfaceMethod, Object result, Object... args) {
        springConfigOwner ?: result
    }
}
