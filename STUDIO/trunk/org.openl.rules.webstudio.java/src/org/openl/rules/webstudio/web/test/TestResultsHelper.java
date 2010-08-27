package org.openl.rules.webstudio.web.test;

import java.util.Collections;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.DoubleValue;
import org.openl.rules.ui.Explanator;

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
}
