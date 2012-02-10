package org.openl.rules.webstudio.web;

import org.openl.message.OpenLMessage;
import org.openl.util.StringTool;

public class DependencyModuleUrlStub {
    
    private DependencyModuleUrlStub(){};
    
    public static String getUrlForError(OpenLMessage message) {        
        return "message.xhtml" + "?type" + "=" + message.getSeverity().name() + "&summary" + "=" + StringTool.encodeURL(String.format("Dependency error: %s",
            message.getSummary()));
    }
}
