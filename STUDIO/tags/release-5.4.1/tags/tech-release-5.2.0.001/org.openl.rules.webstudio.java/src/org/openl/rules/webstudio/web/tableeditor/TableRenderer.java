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
    private final TableModel tableModel;
    private String cellIdPrefix;

    public TableRenderer(TableModel tableModel) {
        this.tableModel = tableModel;
    }

    public void setCellIdPrefix(String prefix) {
        cellIdPrefix = prefix;
    }

    public String render(String extraTDText, boolean embedCellURI) {
        String tdPrefix = "<td";
        if (extraTDText != null) {
            tdPrefix += " ";
            tdPrefix += extraTDText;
        }
        final String prefix = cellIdPrefix != null ? cellIdPrefix : "cell-";

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
                id.append(prefix).append(String.valueOf(i + 1)).append(":").append(j + 1);

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

    public String render() {
        return render(null, false);
    }

    public String renderWithMenu() {
        return render("onmouseover=\"try {cellMouseOver(this,event)} catch (e){}\" onmouseout='try {cellMouseOut(this)} catch(e){}'", true);
    }
}

