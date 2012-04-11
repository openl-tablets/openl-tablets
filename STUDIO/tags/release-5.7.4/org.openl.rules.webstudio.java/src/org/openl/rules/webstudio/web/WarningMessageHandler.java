package org.openl.rules.webstudio.web;

import org.openl.main.SourceCodeURLTool;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;

import org.openl.syntax.ISyntaxNode;

public class WarningMessageHandler extends MessageHandler {

    protected String getUri(OpenLMessage message) {
        OpenLWarnMessage warnMessage = (OpenLWarnMessage) message;
        ISyntaxNode syntaxNode = warnMessage.getSource();

        return SourceCodeURLTool.makeSourceLocationURL(syntaxNode.getSourceLocation(), syntaxNode.getModule(), "");        
    }

}
