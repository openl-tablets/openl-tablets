package org.openl.itest.serviceclass;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptors;

public interface RunTestServiceAnnotationTemplate {
    @ServiceCallAfterInterceptors
    SpreadsheetResult calculatePremium(String covName, Double amount);
}
