package org.openl.rules.demo;

public interface AutoPolicyCalculation {
    String DriverRisk(Integer numDUI, Integer numAccidents, Integer numMovingViolations);

    Double AccidentPremium();

    String DriverAgeType(String gender, Integer age);
}
