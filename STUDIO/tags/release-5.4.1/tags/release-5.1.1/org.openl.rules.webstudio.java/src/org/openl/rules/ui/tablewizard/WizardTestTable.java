package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TestTableBuilder;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * @author Aliaksandr Antonik.
 */
public class WizardTestTable extends WizardBase {
    private SelectItem[] tableItems;
    private int selectedTable;

    protected void onFinish(boolean cancelled) {
        tableItems = null;
        reset();
    }

    protected void onStart() {
        selectedTable = 0;

        TableSyntaxNode[] syntaxNodes = getSyntaxNodes();
        List<SelectItem> result = new ArrayList<SelectItem>();

        for (int i = 0; i < syntaxNodes.length; i++) {
            TableSyntaxNode node = syntaxNodes[i];
            if (ITableNodeTypes.XLS_DT.equals(node.getType())) {
                result.add(new SelectItem(i, node.getMember().getName()));
            }
        }

        tableItems = result.toArray(new SelectItem[result.size()]);
        Arrays.sort(tableItems, new Comparator<SelectItem>() {
            public int compare(SelectItem o1, SelectItem o2) {
                return (o1.getValue().toString()).compareTo(o2.getValue().toString());
            }
        });
    }

    @Override
    protected void onStepFirstVisit(int step) {
        if (step == 2) {
            initWorkbooks();
        }
    }

    private TableSyntaxNode[] getSyntaxNodes() {
        return getMetaInfo().getXlsModuleNode().getXlsTableSyntaxNodes();
    }

    public SelectItem[] getDecisionTables() {
        return tableItems;
    }

    public int getSelectedTable() {
        return selectedTable;
    }

    public void setSelectedTable(int selectedTable) {
        this.selectedTable = selectedTable;
    }

    @Override
    public String getName() {
        return "newTestTable";
    }

    public String save() {
        try {
            doSave();
            return "done";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Could not create table: ", e.getMessage()));
        }
        return null;
    }

    private void doSave() throws CreateTableException, IOException {
        TableSyntaxNode[] nodes = getSyntaxNodes();
        if (selectedTable < 0 || selectedTable >= nodes.length)
            throw new IllegalStateException("not table is selected");

        TableSyntaxNode node = nodes[selectedTable];
        String header = TestTableBuilder.getHeader(node);
        Map<String, String> params = TestTableBuilder.getParams(node);

        TestTableBuilder builder = new TestTableBuilder(
                new XlsSheetGridModel(getDestinationSheet()));
        builder.beginTable(params.size() + 1, 4);
        builder.writeHeader(header, null);
        builder.writeParams(params, null);
        builder.endTable();
    }
}
