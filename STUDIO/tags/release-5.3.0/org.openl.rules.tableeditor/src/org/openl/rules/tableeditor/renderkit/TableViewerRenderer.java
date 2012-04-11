package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.openl.rules.table.ITable;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.tableeditor.util.Constants;

public class TableViewerRenderer extends BaseRenderer {

    @SuppressWarnings("unchecked")
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        Map params = component.getAttributes();
        ITable table = (ITable) params.get(Constants.ATTRIBUTE_TABLE);
        IGridFilter filter = (IGridFilter) params.get(Constants.ATTRIBUTE_FILTER);
        String view = (String) params.get(Constants.ATTRIBUTE_VIEW);
        boolean showFormulas = toBoolean(params.get(Constants.ATTRIBUTE_SHOW_FORMULAS));
        boolean collapseProps = toBoolean(params.get(Constants.ATTRIBUTE_COLLAPSE_PROPS));
        String editorId = new HtmlOutputText().getClientId(context);
        new HTMLRenderer().render(table, view, editorId, filter, showFormulas, collapseProps);
    }

    @Override
    public void encodeChildren(FacesContext arg0, UIComponent arg1) throws IOException {
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

}
