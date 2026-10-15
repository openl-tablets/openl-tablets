package spring

import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor

/**
 * The annotation template of the service. Its interceptor is compiled together with it, apart from SpringConfig.
 */
interface Service {

    @ServiceCallAfterInterceptor(ProxyReportingInterceptor)
    String proxies()
}
