package com.example.java

import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.interceptors.RulesType

interface RatingService {

    SpreadsheetResult DeterminePolicyRates(@RulesType("Policy") Object policy);

}
