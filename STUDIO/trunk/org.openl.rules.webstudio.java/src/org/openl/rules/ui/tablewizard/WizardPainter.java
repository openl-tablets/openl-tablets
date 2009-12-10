package org.openl.rules.ui.tablewizard;

import java.util.List;
import java.util.Map.Entry;

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

    private DecisionTableCreationWizard wizardDecisionTable;

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
        int systemPropHeight = wizardDecisionTable.getSystemProperties().size();
        int conditionsWidth = countColumns(wizardDecisionTable.getConditions());
        int actionsWidth = countColumns(wizardDecisionTable.getActions());
        int returnWidth = wizardDecisionTable.getReturn().getParamsCount();
        int widthWithProp = 0;
        int width = conditionsWidth + actionsWidth + returnWidth;
        if (width < 3 && !wizardDecisionTable.getSystemProperties().isEmpty()) {
            widthWithProp = 3;
        } else {
            widthWithProp = width;
        }        
        int height = 4 + 1 + 3 + systemPropHeight; // technical info + columns headers + data rows + system properties

        tableModel = new TableModel(widthWithProp, height, null);
        addCell(0, 0, widthWithProp, getTableTitle());
        
        int row = 1;
        for (Entry<String, Object> sysProp : wizardDecisionTable.getSystemProperties().entrySet()) {            
            addCell(row, 0, 1, "properties");
            addCell(row, 1, 1, sysProp.getKey());
            addCell(row, 2, 1, sysProp.getValue().toString());            
            row++;
        }  
        
        int col = 0;
        for (TableCondition cond : wizardDecisionTable.getConditions()) {
            int colSpan = cond.getParamsCount();
            addCell(row+1, col, colSpan, cond.getName());
            addCell(row+2, col, colSpan, cond.getLogic());
            for (Parameter p : cond.getParameters()) {
                addCell(row+3, col, 1, p.getType() + " " + p.getName());
                addCell(row+4, col++, 1, p.getBusinessName());
            }
        }
        for (TableArtifact action : wizardDecisionTable.getActions()) {
            int colSpan = action.getParamsCount();
            addCell(row+1, col, colSpan, action.getName());
            addCell(row+2, col, colSpan, action.getLogic());
            for (Parameter p : action.getParameters()) {
                addCell(row+3, col, 1, p.getType() + " " + p.getName());
                addCell(row+4, col++, 1, p.getBusinessName());
            }
        }
        addCell(row+1, col, returnWidth, "RET1");
        addCell(row+2, col, returnWidth, wizardDecisionTable.getReturn().getLogic());
        for (Parameter p : wizardDecisionTable.getReturn().getParameters()) {
            addCell(row+3, col, 1, p.getType() + " " + p.getName());
            addCell(row+4, col++, 1, p.getBusinessName());
        }

        for (int rowN = row+5; rowN < height; ++rowN) {
            for (col = 0; col < width; ++col) {
                addCell(rowN, col, 1, "Data");
            }
        }

        //FIXME: should formulas be displayed?
        return new HTMLRenderer.TableRenderer(tableModel).render(false);
    }

    private String getTableTitle() {
        StringBuilder sb = new StringBuilder("Rules ");
        sb.append(wizardDecisionTable.getReturnType()).append(" ").append(wizardDecisionTable.getTableName());
        appendParams(sb, wizardDecisionTable.getParameters());
        return sb.toString();
    }

    public void setWizardBean(DecisionTableCreationWizard wizardDecisionTable) {
        this.wizardDecisionTable = wizardDecisionTable;
    }
}
