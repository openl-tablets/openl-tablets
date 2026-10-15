package org.openl.itest.proxies;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A Java Spring configuration of the application class path that a service imports. It proxies a Java bean, including
 * its package-private method, and a JDK-class bean by class.
 *
 * <p>It also proxies the service interceptors by their {@code afterReturning} method and exposes the proxy to them.
 */
@Configuration(proxyBeanMethods = false)
public class JavaProxiesConfig {

    @Bean
    static DefaultAdvisorAutoProxyCreator classProxyCreator() {
        var creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        creator.setExposeProxy(true);
        return creator;
    }

    @Bean
    static Advisor passThroughAdvisor() {
        var pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedNames("greet", "get", "punctuation", "afterReturning");
        MethodInterceptor advice = invocation -> "punctuation".equals(invocation.getMethod().getName())
                ? "!"
                : invocation.proceed();
        return new DefaultPointcutAdvisor(pointcut, advice);
    }

    @Bean
    JavaGreeting javaGreeting() {
        return new JavaGreeting();
    }

    @Bean
    Map<String, String> names() {
        return new HashMap<>(Map.of("name", "Java"));
    }

    @Bean
    String report(JavaGreeting greeting, @Qualifier("names") Map<String, String> names) {
        var proxied = Stream.of(greeting, names).filter(AopUtils::isCglibProxy).count();
        return "%s%s, %d beans proxied by class".formatted(greeting.greet(names.get("name")),
                greeting.punctuation(),
                proxied);
    }
}
