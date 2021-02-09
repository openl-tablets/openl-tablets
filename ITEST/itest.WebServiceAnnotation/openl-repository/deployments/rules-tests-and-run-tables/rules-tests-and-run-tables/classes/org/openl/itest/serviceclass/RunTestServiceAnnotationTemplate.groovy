package org.openl.itest.serviceclass;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;

interface RunTestServiceAnnotationTemplate {
    @ServiceCallAfterInterceptor
    SpreadsheetResult calculatePremium(String covName, Double amount);
}
