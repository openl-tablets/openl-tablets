package com.example.java

import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.interceptors.RulesType
import org.openl.rules.ruleservice.storelogdata.annotation.*;

interface RatingService {

    @PrepareStoreLogData(CollectAfter.class)
    SpreadsheetResult DeterminePolicyRates(@RulesType("Policy") Object policy);

}
