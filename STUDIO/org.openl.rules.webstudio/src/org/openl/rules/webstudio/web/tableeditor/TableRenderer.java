package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.ui.CellModel;
import org.openl.rules.ui.ICellModel;
import org.openl.rules.ui.TableModel;
import org.openl.rules.table.IGridTable;

/**
 * Render HTML table by table model.
 *
 * @author Andrey Naumenko
 */
public class TableRenderer {
    public static String render(TableModel tableModel, String extraTDText, boolean embedCellURI) {
        String tdPrefix = "<td";
        if (extraTDText != null) {
            tdPrefix += " ";
            tdPrefix += extraTDText;
        }

        IGridTable table = tableModel.getGridTable();
        StringBuffer s = new StringBuffer();
        s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

        for (int i = 0; i < tableModel.getCells().length; i++) {
            s.append("<tr>\n");
            for (int j = 0; j < tableModel.getCells()[i].length; j++) {
                ICellModel cell = tableModel.getCells()[i][j];
                if ((cell == null) || !cell.isReal()) {
                    continue;
                }

                s.append(tdPrefix);
                if (cell instanceof CellModel) {
                    ((CellModel) (cell)).atttributesToHtml(s, tableModel);
                }

                StringBuilder id = new StringBuilder();
                id.append("cell-").append(String.valueOf(i + 1)).append(":").append(j + 1);

                s.append(" id=\"").append(id).append("\">");
                if (embedCellURI) {
                    s.append("<input name=\"uri\" type=\"hidden\" value=\"").append(table.getUri(j, i)).append("\">");
                }
                s.append(cell.getContent()).append("</td>\n");
            }
            s.append("</tr>\n");
        }
        s.append("</table>");
        return s.toString();
    }

    public static String render(TableModel tableModel) {
        return render(tableModel, null, false);
    }

    public static String renderWithMenu(TableModel tableModel) {
        return render(tableModel, "onmouseover=\"try {cellMouseOver(this,event)} catch (e){}\" onmouseout='try {cellMouseOut(this)} catch(e){}'", true);
    }
}

