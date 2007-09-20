package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.ui.CellModel;
import org.openl.rules.ui.ICellModel;
import org.openl.rules.ui.TableModel;


/**
 * Render HTML table by table model.
 *
 * @author Andrey Naumenko
 */
public class TableRenderer {
    public static String render(TableModel tableModel) throws Exception {
        StringBuffer s = new StringBuffer();
        s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

        for (int i = 0; i < tableModel.getCells().length; i++) {
            s.append("<tr>\n");
            for (int j = 0; j < tableModel.getCells()[i].length; j++) {
                ICellModel cell = tableModel.getCells()[i][j];
                if ((cell == null) || !cell.isReal()) {
                    continue;
                }

                s.append("<td");
                if (cell instanceof CellModel) {
                    ((CellModel) (cell)).atttributesToHtml(s, tableModel);
                }

                StringBuffer id = new StringBuffer();
                for (int k = 0; k < cell.getRowspan(); k++) {
                    for (int l = 0; l < cell.getColspan(); l++) {
                        id.append("cell-" + String.valueOf(i + k + 1) + "-"
                            + String.valueOf(j + l + 1) + "_");
                    }
                }

                s.append(" title=\"" + id + "\">\n" + cell.getContent() + "</td>\n");
            }
            s.append("</tr>\n");
        }
        s.append("</table>");
        return s.toString();
    }
}
