package org.openl.rules.webstudio.web.test;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ui.Explanator;

public class TestResultsHelper {
    private TestResultsHelper() {
    }

    public static ExplanationNumberValue<?> getExplanationValueResult(Object result) {
        if (result instanceof ExplanationNumberValue<?>) {
            return (ExplanationNumberValue<?>) result;
        }

        return null;
    }

    @Deprecated
    public static String getNullResult() {
        return "null";
    }

    public static SpreadsheetResult getSpreadsheetResult(Object result) {
        if (result instanceof SpreadsheetResult) {
            return (SpreadsheetResult) result;
        }
        return null;
    }
}
