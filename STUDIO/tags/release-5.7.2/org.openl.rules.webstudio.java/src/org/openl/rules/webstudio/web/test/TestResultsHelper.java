package org.openl.rules.webstudio.web.test;

import java.util.Collections;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.xls.formatters.IFormatter;
import org.openl.rules.table.xls.formatters.XlsFormattersManager;
import org.openl.rules.table.xls.formatters.XlsStringFormatter;
import org.openl.rules.ui.Explanator;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.print.Formatter;

public class TestResultsHelper {
    private TestResultsHelper(){}
    
    public static DoubleValue getDoubleValueResult(Object result) {
        if (result instanceof DoubleValue) {
            return (DoubleValue) result;
        }
        
        return null;
    }
    
    public static int getExplanatorId(DoubleValue doubleValue) {        
        return Explanator.getCurrent().getUniqueId(doubleValue);
    }
    
    public static List<OpenLMessage> getErrors(Object result) {
        if (result instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable) result);
        }

        return Collections.emptyList();
    }

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
    
    public static String getFormattedResult(Object value) {
        if (value != null) {
            IFormatter formatter = XlsFormattersManager.getFormatter(value.getClass());
            if (formatter instanceof XlsStringFormatter) { // this is formatter used by default, we don`t like it, 
                // so we try to format the value by other way.
                // it is temporary. need to gather together all formatters functionality.
                return Formatter.format(value, INamedThing.REGULAR, new StringBuffer()).toString();
            }
            return formatter.format(value);
        }
        return Formatter.format(value, INamedThing.REGULAR, new StringBuffer()).toString();
    }
}
