package org.openl.jsf;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;

import org.apache.myfaces.el.ValueBindingImpl;

public class HtmlInputTextActivator implements ICellEditorActivator {

    public UIComponent createInstance(Object value, Object metadata) {
        //
        HtmlInputText result = new HtmlInputText();
        result.setOnkeyup("javascript:if(13==event.keyCode){stopEditing2();};");
        //result.setValue(value);
        return result;
    }
}