package org.openl.rules.utils.exception;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class ExceptionUtils {
    
    private ExceptionUtils(){}
    
    /**
     * Extract not informative messages from some exception classes. Make it more uderstandable, and wrap to
     * {@link SyntaxNodeException}.
     * Supports:<br>
     * {@link NumberFormatException}  
     * 
     * @param cell
     * @param t
     * @throws SyntaxNodeException
     */
    public static void processError(ILogicalTable cell, Throwable t) throws SyntaxNodeException {        
        processNumberFormatException(cell, t);
        throw SyntaxNodeExceptionUtils.createError(null, t, null, new GridCellSourceCodeModule(cell.getGridTable()));
    }
    
    /**
     * NumberFormatException error is not informative for common users. 
     * Update it`s message.
     * 
     * @param cell
     * @param t
     * @throws SyntaxNodeException
     */
    private static void processNumberFormatException(ILogicalTable cell, Throwable t) throws SyntaxNodeException {
        if (t instanceof NumberFormatException) {
            String message = parseNumberFormatExceptionMessage(t);
            if (message == null) {
                message = "Cannot convert String to numeric type";
            }
            throw SyntaxNodeExceptionUtils.createError(message, null, null, 
                new GridCellSourceCodeModule(cell.getGridTable()));
        }
    }

    private static String parseNumberFormatExceptionMessage(Throwable t) {
        String[] splittedMessage = t.getMessage().split(" ");            
        if (splittedMessage.length > 1) {
            String inputString = splittedMessage[splittedMessage.length - 1];
            return String.format("Cannot convert String '%s' to numeric type", inputString);
        }
        return null;
    }

}
