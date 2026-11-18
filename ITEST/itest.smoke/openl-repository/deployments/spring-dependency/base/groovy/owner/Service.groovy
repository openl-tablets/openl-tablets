package owner

import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor

/**
 * The annotation template of the services using this project. A class of a dependency is still found by them.
 */
interface Service {

    @ServiceCallAfterInterceptor(OwnerInterceptor)
    String owner()
}
