package org.openl.commons.web.jsf.facelets.acegi;

import java.io.IOException;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

/**
 * Component that displayes user info. See javadocs for AuthenticationTag from Spring Security library for more
 * information.
 *
 * @author Andrey Naumenko
 */
public class AuthenticationComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.openl.commons.web.jsf.facelets.acegi.Authentication";
    public static final String COMPONENT_FAMILY = "org.openl.commons.web.jsf.facelets.acegi";
    private String operation;

    public AuthenticationComponent() {
        super();
        setRendererType(null);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (null == operation || "".equals(operation)) {
            return;
        }

        String property = AcegiFunctions.authentication(operation);
        ResponseWriter writer = context.getResponseWriter();
        writer.write(property);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getOperation() {
        if (operation != null) {
            return operation;
        }

        ValueBinding vb = getValueBinding("operation");
        if (vb != null) {
            return (String) vb.getValue(getFacesContext());
        } else {
            return null;
        }
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        operation = (String) values[1];
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = operation;
        return values;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
