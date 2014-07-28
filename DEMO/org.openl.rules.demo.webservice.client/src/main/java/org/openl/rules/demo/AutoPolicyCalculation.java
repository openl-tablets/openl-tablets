package org.openl.rules.demo;

import org.openl.meta.DoubleValue;

public interface AutoPolicyCalculation {
    String DriverRisk(Integer numDUI, Integer numAccidents, Integer numMovingViolations);

    DoubleValue AccidentPremium();

    String DriverAgeType(String gender, Integer age);
}
