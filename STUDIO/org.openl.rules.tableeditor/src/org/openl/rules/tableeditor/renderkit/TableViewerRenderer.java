package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

public class TableViewerRenderer extends BaseRenderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        TableEditor tableEditor = new TableEditor(context, component);
        if (tableEditor.getTable() == null) { // required attribute
            return;
        }
        writer.write(new HTMLRenderer().render(tableEditor));
    }

    @Override
    public void encodeChildren(FacesContext arg0, UIComponent arg1) throws IOException {
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

}
