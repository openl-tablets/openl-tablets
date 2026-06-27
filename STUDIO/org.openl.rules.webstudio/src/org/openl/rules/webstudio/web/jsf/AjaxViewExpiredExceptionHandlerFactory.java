package org.openl.rules.webstudio.web.jsf;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

/**
 * Installs {@link AjaxViewExpiredExceptionHandler} so AJAX requests recover gracefully from an expired view.
 */
public class AjaxViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {

    public AjaxViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new AjaxViewExpiredExceptionHandler(getWrapped().getExceptionHandler());
    }
}
