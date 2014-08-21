package org.openl.rules.webstudio.web.test;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.ui.Explanator;
import org.openl.util.formatters.IFormatter;

public class TestResultsHelper {
    private TestResultsHelper(){}
    
    public static ExplanationNumberValue<?> getExplanationValueResult(Object result) {
        if (result instanceof ExplanationNumberValue<?>) {
            return (ExplanationNumberValue<?>) result;
        }
        
        return null;
    }
    
    public static int getExplanatorId(ExplanationNumberValue<?> explanationValue) {        
        return Explanator.getCurrent().getUniqueId(explanationValue);
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
    
    public static String format(Object value) {
        IFormatter formatter = FormattersManager.getFormatter(value);
        return formatter.format(value);
    }
}
