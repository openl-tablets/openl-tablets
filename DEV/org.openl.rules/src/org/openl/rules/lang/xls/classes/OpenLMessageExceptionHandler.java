package org.openl.rules.lang.xls.classes;

import java.io.IOException;

import org.openl.message.OpenLMessagesUtils;

public class OpenLMessageExceptionHandler extends AbstractLocatorExceptionHandler {

    @Override
    public void handleURLParseException(Exception e) {
        OpenLMessagesUtils.addError(e);
    }

    @Override
    public void handleIOException(IOException e) {
        OpenLMessagesUtils.addError(e);
    }
}
