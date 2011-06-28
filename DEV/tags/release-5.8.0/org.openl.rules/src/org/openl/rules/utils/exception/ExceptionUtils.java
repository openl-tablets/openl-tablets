package org.openl.rules.utils.exception;

import org.openl.exception.OpenLRuntimeException;

public class ExceptionUtils {
    
    private ExceptionUtils(){}
    
    
    private static String parseNumberFormatExceptionMessage(Throwable t) {
        String[] splittedMessage = t.getMessage().split(" ");            
        if (splittedMessage.length > 1) {
            String inputString = splittedMessage[splittedMessage.length - 1];
            return String.format("Cannot convert String '%s' to numeric type", inputString);
        }
        return null;
    }
    
    /**
     * NumberFormatException error is not informative for common users. 
     * Update it`s message. Wrap the NumberFormatException with new message by OpenLRuntimeException.
     *  
     * @param t
     * @throws OpenLRuntimeException
     */
    public static void processNumberFormatException(Throwable t) throws OpenLRuntimeException {
        if (t instanceof NumberFormatException) {
            String message = parseNumberFormatExceptionMessage(t);
            if (message == null) {
                message = "Cannot convert String to numeric type";
            }
            throw new OpenLRuntimeException(message);
        }
    }
    
    

}
