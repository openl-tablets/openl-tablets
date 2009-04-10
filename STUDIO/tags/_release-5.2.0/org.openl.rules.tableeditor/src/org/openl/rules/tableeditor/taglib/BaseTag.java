package org.openl.rules.tableeditor.taglib;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.webapp.UIComponentTag;

public abstract class BaseTag extends UIComponentTag  {

    @SuppressWarnings("unchecked")
    public void setObject(UIComponent component, String name, Object expr) {
        if (expr == null) {
            return;
        } else {
            component.getAttributes().put(name, expr);
        }
    }

    @SuppressWarnings("unchecked")
    public void setBoolean(UIComponent component, String name, String expr) {
        if (expr == null) {
            return;
        } else if (UIComponentTag.isValueReference(expr)) {
            FacesContext context = FacesContext.getCurrentInstance();
            Application app = context.getApplication();
            ValueBinding binding = app.createValueBinding(expr);
            component.setValueBinding(name, binding);
        } else
            component.getAttributes().put(name, new Boolean(expr));
    }

}