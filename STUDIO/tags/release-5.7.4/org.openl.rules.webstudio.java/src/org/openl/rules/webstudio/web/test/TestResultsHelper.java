package org.openl.rules.webstudio.web.test;

import java.util.Collections;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.xls.formatters.FormattersManager;
import org.openl.rules.ui.Explanator;
import org.openl.rules.webstudio.web.util.Constants;
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
    
    public static List<OpenLMessage> getErrors(Object result) {
        if (result instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable) result);
        }

        return Collections.emptyList();
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
    
    public static void initExplanator() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR, explanator);
        }
        Explanator.setCurrent(explanator);
    } 
        
    public static String format(Object value) {
        IFormatter formatter = FormattersManager.getFormatter(value);
        return formatter.format(value);
    }
}
