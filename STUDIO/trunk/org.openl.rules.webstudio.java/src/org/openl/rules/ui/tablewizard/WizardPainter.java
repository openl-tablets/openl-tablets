package org.openl.rules.ui.tablewizard;

import java.util.List;

import org.openl.rules.tableeditor.model.ui.BorderStyle;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;

/**
 * @author Aliaksandr Antonik.
 */
public class WizardPainter {
    static final BorderStyle BORDER_SOLID = new BorderStyle(1, "solid", null);
    static final BorderStyle BORDER_NONE = new BorderStyle(0, "none", null);

    static final BorderStyle[] BORDER_STYLE = new BorderStyle[] { BORDER_SOLID, BORDER_SOLID, BORDER_SOLID,
            BORDER_SOLID };

    private WizardDecisionTable wizardDecisionTable;

    private TableModel tableModel;
    private static void appendParams(StringBuilder sb, List<TypeNamePair> params) {
        boolean first = true;
        sb.append('(');
        for (TypeNamePair p : params) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p.getType()).append(" ").append(p.getName());
        }
        sb.append(')');
    }
    private void addCell(int row, int col, int colSpan, String value) {
        // todo: escape html
        CellModel cellModel = new CellModel(row, col);

        cellModel.setBorderStyle(BORDER_STYLE);

        cellModel.setColspan(colSpan);
        cellModel.setContent(value);
        tableModel.addCell(cellModel, row, col);
    }

    private int countColumns(List<? extends TableArtifact> artifacts) {
        int res = 0;
        for (TableArtifact artifact : artifacts) {
            res += artifact.getParamsCount();
        }
        return res;
    }

    public String getTableHTML() {
        int conditionsWidth = countColumns(wizardDecisionTable.getConditions());
        int actionsWidth = countColumns(wizardDecisionTable.getActions());
        int returnWidth = wizardDecisionTable.getReturn().getParamsCount();
        int width = conditionsWidth + actionsWidth + returnWidth;
        int height = 4 + 1 + 3; // technical info + columns headers + data rows

        tableModel = new TableModel(width, height, null);
        addCell(0, 0, width, getTableTitle());

        int col = 0;
        for (TableCondition cond : wizardDecisionTable.getConditions()) {
            int colSpan = cond.getParamsCount();
            addCell(1, col, colSpan, cond.getName());
            addCell(2, col, colSpan, cond.getLogic());
            for (Parameter p : cond.getParameters()) {
                addCell(3, col, 1, p.getType() + " " + p.getName());
                addCell(4, col++, 1, p.getBusinessName());
            }
        }
        for (TableArtifact action : wizardDecisionTable.getActions()) {
            int colSpan = action.getParamsCount();
            addCell(1, col, colSpan, action.getName());
            addCell(2, col, colSpan, action.getLogic());
            for (Parameter p : action.getParameters()) {
                addCell(3, col, 1, p.getType() + " " + p.getName());
                addCell(4, col++, 1, p.getBusinessName());
            }
        }
        addCell(1, col, returnWidth, "RET1");
        addCell(2, col, returnWidth, wizardDecisionTable.getReturn().getLogic());
        for (Parameter p : wizardDecisionTable.getReturn().getParameters()) {
            addCell(3, col, 1, p.getType() + " " + p.getName());
            addCell(4, col++, 1, p.getBusinessName());
        }

        for (int row = 5; row < height; ++row) {
            for (col = 0; col < width; ++col) {
                addCell(row, col, 1, "Data");
            }
        }

        return new HTMLRenderer.TableRenderer(tableModel).render();
    }

    private String getTableTitle() {
        StringBuilder sb = new StringBuilder("Rules ");
        sb.append(wizardDecisionTable.getReturnType()).append(" ").append(wizardDecisionTable.getTableName());
        appendParams(sb, wizardDecisionTable.getParameters());
        return sb.toString();
    }

    public void setWizardBean(WizardDecisionTable wizardDecisionTable) {
        this.wizardDecisionTable = wizardDecisionTable;
    }
}
