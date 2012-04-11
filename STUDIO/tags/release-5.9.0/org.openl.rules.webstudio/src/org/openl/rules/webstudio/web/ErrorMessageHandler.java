package org.openl.rules.webstudio.web;

import org.openl.exception.OpenLException;
import org.openl.main.SourceCodeURLTool;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;

public class ErrorMessageHandler extends MessageHandler {    
    
    protected String getUri(OpenLMessage message) {
        OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
        OpenLException error = errorMessage.getError();
        return SourceCodeURLTool.makeSourceLocationURL(error.getLocation(), error.getSourceModule(), "");        
    }

}
