package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.util.Constants;

public class TableViewerRenderer extends BaseRenderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        IGridTable table = (IGridTable) component.getAttributes().get(
                Constants.ATTRIBUTE_TABLE);
        String editorId = new HtmlOutputText().getClientId(context);
        new HTMLRenderer().render(table, editorId);
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext arg0, UIComponent arg1)
            throws IOException {
    }

}
