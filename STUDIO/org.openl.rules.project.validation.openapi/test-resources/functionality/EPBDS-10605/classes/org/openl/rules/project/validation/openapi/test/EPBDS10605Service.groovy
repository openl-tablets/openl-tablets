package org.openl.rules.project.validation.openapi.test

import org.openl.rules.calc.SpreadsheetResult
import org.openl.rules.ruleservice.core.interceptors.NoTypeConversion

interface EPBDS10605Service {
    Double GetCoveragePremium(@NoTypeConversion SpreadsheetResult spreadsheetResult)

    Double GetRiskItemPremium(@NoTypeConversion SpreadsheetResult spreadsheetResult)
}

