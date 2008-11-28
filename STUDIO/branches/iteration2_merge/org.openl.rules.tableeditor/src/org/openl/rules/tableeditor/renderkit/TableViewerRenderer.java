package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.model.ui.TableRenderer;

public class TableViewerRenderer extends BaseRenderer {

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.write("<div class='_te'>");
        encodeCSS(component, writer, "css/common.css");
        encodeTableViewer(component, writer);
        writer.write("</div>");
    }

    protected void encodeTableViewer(UIComponent component,
            ResponseWriter writer)throws IOException {
        IGridTable table = (IGridTable) component.getAttributes().get("table");
        TableModel tableModel = TableModel.initializeTableModel(table);
        if (tableModel != null) {
            String htmlTable = new TableRenderer(tableModel).render();
            writer.write(htmlTable);
        }
    }

}
