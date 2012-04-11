package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;

import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TestTableBuilder;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * @author Aliaksandr Antonik.
 */
public class TestTableCreationWizard extends BusinessTableCreationWizard {

    private SelectItem[] tableItems;

    /**
     * index of the selected item, when selecting table name to test.
     */
    private int selectedTableNameIndex;
    
    /**
     * Technical name of newly created test table.
     */
    @NotEmpty(message="Technical name can not be empty")
    @Pattern(regexp="([a-zA-Z_][a-zA-Z_0-9]*)?", message="Invalid technical name")
    private String technicalName;

    /**
     * @return Technical name of newly created test table.
     */
    public String getTechnicalName() {
        return technicalName;
    }

    /**
     * @param technicalName Technical name of newly created test table.
     */
    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }

    /**
     * @return see {@link TestTableBuilder#getDefaultTechnicalName(TableSyntaxNode)} 
     */
    protected String getDefaultTechnicalName() {
        TableSyntaxNode node = getSelectedNode();
        String defaultName = TestTableBuilder.getDefaultTechnicalName(node);
        return defaultName;
    }
    
    /**
     * 
     * @return <code>TableSyntaxNode</code> from model, by the 
     * technical name of the table we have selected. 
     */
    protected TableSyntaxNode getSelectedNode() {   
        TableSyntaxNode[] nodes = getSyntaxNodes();
        if (selectedTableNameIndex < 0 || selectedTableNameIndex >= nodes.length) {
            throw new IllegalStateException("not table is selected");
        }
        return nodes[selectedTableNameIndex];          
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        TableSyntaxNode node = getSelectedNode();
        
        String header = TestTableBuilder.getHeader(node, technicalName);
        
        Map<String, String> params = TestTableBuilder.getParams(node);

        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        TestTableBuilder builder = new TestTableBuilder(gridModel);

        Map<String, Object> properties = buildProperties();

        int width = params.size() + 1;
        if (width < 3 && !properties.isEmpty()) {
            width = 3;  // Properties require 3 columns
        }
        int height = 3 + properties.size(); // 3 required rows + Properties
        builder.beginTable(width, height);

        builder.writeHeader(header, null);

        builder.writeProperties(properties, null);

        builder.writeParams(params, null);

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

        return uri;
    }

    public SelectItem[] getDecisionTables() {
        return tableItems;
    }

    @Override
    public String getName() {
        return "newTestTable";
    }

    public int getSelectedTable() {
        return selectedTableNameIndex;
    }

    private TableSyntaxNode[] getSyntaxNodes() {
        return getMetaInfo().getXlsModuleNode().getXlsTableSyntaxNodesWithoutErrors();
    }

    @Override
    protected void onCancel() {
        tableItems = null;
        reset();
    }

    @Override
    protected void onStart() {
        selectedTableNameIndex = 0;

        TableSyntaxNode[] syntaxNodes = getSyntaxNodes();
        List<SelectItem> result = new ArrayList<SelectItem>();

        for (int i = 0; i < syntaxNodes.length; i++) {
            TableSyntaxNode node = syntaxNodes[i];
            if (isExecutableAndTestableNode(node)) {
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
    
    /**
     * Checks if it is possible to create test for current table(table is executable at runtime), and checks if
     * return type of the table is not void.
     * 
     * @param node
     * @return
     */
    private boolean isExecutableAndTestableNode(TableSyntaxNode node) {
        if (node.isExecutableNode()) {
            if (!void.class.equals(node.getMember().getType().getInstanceClass())) {
                return true;
            }
        }
        return false;
    }    

    @Override
    protected void onStepFirstVisit(int step) {
        if (step == 3) {
            initWorkbooks();
        }
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableUri(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    public void setSelectedTable(int selectedTableNameIndex) {
        this.selectedTableNameIndex = selectedTableNameIndex;
        this.technicalName = getDefaultTechnicalName();
    }

}
