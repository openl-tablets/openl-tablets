package org.openl.rules.tutorial4;

import org.openl.generated.beans.Driver;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;

public interface Tutorial4Interface {
    @ServiceCallBeforeInterceptor(interceptorClass = InvocationCounter.class)
    String[] getCoverage();

    String[] getTheft_rating();

    @ServiceCallBeforeInterceptor(interceptorClass = DriverValidator.class)
    @ServiceCallAfterInterceptor(interceptorClass = DriverAgeTypeConvertor.class)
    DriverAgeType driverAgeType(Driver driver);
}
