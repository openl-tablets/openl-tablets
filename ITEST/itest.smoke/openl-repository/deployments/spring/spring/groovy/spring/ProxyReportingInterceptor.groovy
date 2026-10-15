package spring

import java.lang.reflect.Method

import org.springframework.aop.framework.AopContext
import org.springframework.aop.support.AopUtils

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice

/**
 * A Groovy interceptor compiled apart from SpringConfig, in another class loader. It is proxied by class and reports
 * whether it is called through the proxy.
 */
class ProxyReportingInterceptor extends AbstractServiceMethodAfterReturningAdvice<String> {

    @Override
    String afterReturning(Method interfaceMethod, Object result, Object... args) {
        "${result}; interceptor proxied by class: ${AopUtils.isCglibProxy(AopContext.currentProxy())}"
    }
}
