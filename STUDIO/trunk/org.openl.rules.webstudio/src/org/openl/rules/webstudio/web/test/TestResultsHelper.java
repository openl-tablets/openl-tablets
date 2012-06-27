package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.exception.OpenLException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.rules.ui.Explanator;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
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
    
    public static List<OpenLMessage> getUserMessagesAndErrors(Object result) {
        if (result instanceof Throwable) {
            Throwable exception = (Throwable) result;
            exception = ExceptionUtils.getRootCause(exception);
            List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
            
            if (exception instanceof OpenLUserRuntimeException) {
                OpenLUserRuntimeException userException = (OpenLUserRuntimeException) exception;
                messages.add(new OpenLMessage(userException.getOriginalMessage(), StringUtils.EMPTY));
            } else if (exception instanceof CompositeSyntaxNodeException) {
                CompositeSyntaxNodeException compositeException = (CompositeSyntaxNodeException) exception;
                
                for (OpenLException openLException : compositeException.getErrors()) {
                    if (openLException instanceof OpenLUserRuntimeException) {
                        OpenLUserRuntimeException userException = (OpenLUserRuntimeException) openLException;
                        messages.add(new OpenLMessage(userException.getOriginalMessage(), StringUtils.EMPTY));
                    } else {
                        messages.add(new OpenLErrorMessage(openLException));
                    }
                }
            
            } else {
                if (exception instanceof OpenLException) {
                    messages.add(new OpenLErrorMessage((OpenLException) exception));
                } else {
                    messages.add(new OpenLErrorMessage(ExceptionUtils.getRootCauseMessage(exception), StringUtils.EMPTY));
                }
            }
            
            return messages;
        }

        return Collections.emptyList();
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
