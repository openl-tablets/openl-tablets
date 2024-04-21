package com.example.java;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.interceptors.RulesType;

public interface RatingService {

    SpreadsheetResult DeterminePolicyRates(@RulesType("Policy") Object policy);

}
