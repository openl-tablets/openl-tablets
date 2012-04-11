package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TestTableBuilder;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.webstudio.properties.SystemValuesManager;

/**
 * @author Aliaksandr Antonik.
 */
public class TestTableCreationWizard extends WizardBase {
    private SelectItem[] tableItems;
    private int selectedTable;    
    private String technicalName;

    public String getTechnicalName() {
        return technicalName;
    }

    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }
    
    private String getDefaultTechnicalName() {
        TableSyntaxNode node = getSelectedNode();
        String defaultName = TestTableBuilder.getDefaultTechnicalName(node);
        return defaultName;
    }
    
    /**
     * 
     * @return <code>TableSyntaxNode</code> from model, by the 
     * technical name of the table we have selected. 
     */
    private TableSyntaxNode getSelectedNode() {
        TableSyntaxNode[] nodes = getSyntaxNodes();
        if (selectedTable < 0 || selectedTable >= nodes.length) {
            throw new IllegalStateException("not table is selected");
        }

        TableSyntaxNode node = nodes[selectedTable];
        return node;
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        TableSyntaxNode node = getSelectedNode();
        
        String header = TestTableBuilder.getHeader(node, technicalName);
        
        Map<String, String> params = TestTableBuilder.getParams(node);

        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        TestTableBuilder builder = new TestTableBuilder(gridModel);

        Map<String, Object> systemProps = getSystemProperties();

        int width = params.size() + 1;
        if (width < 3 && !systemProps.isEmpty()) {
            width = 3;  // Properties require 3 columns
        }
        int height = 3 + systemProps.size(); // 3 required rows + Properties
        builder.beginTable(width, height);

        builder.writeHeader(header, null);

        builder.writeProperties(systemProps, null);

        builder.writeParams(params, null);

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();
        builder.save();

        return uri;
    }

    private void doSave() throws CreateTableException {
        XlsSheetSourceCodeModule sourceCodeModule = getDestinationSheet();
        String newTableUri = buildTable(sourceCodeModule);
        setNewTableUri(newTableUri);
    }

    private Map<String, Object> getSystemProperties() {        
        Map<String, Object> result = new HashMap<String, Object>();
            List<TablePropertyDefinition> systemPropDefinitions = TablePropertyDefinitionUtils
                    .getSystemProperties();
            for (TablePropertyDefinition systemPropDef : systemPropDefinitions) {
                if (systemPropDef.getSystemValuePolicy().equals(SystemValuePolicy.IF_BLANK_ONLY)) {
                    Object systemValue = SystemValuesManager.instance().
                        getSystemValue(systemPropDef.getSystemValueDescriptor());
                    if (systemValue != null) {
                        result.put(systemPropDef.getName(), systemValue);                        
                    }
                }
            }
        
        return result;
    }

    public SelectItem[] getDecisionTables() {
        return tableItems;
    }

    @Override
    public String getName() {
        return "newTestTable";
    }

    public int getSelectedTable() {
        return selectedTable;
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

    @Override
    protected void onFinish() throws Exception {
        doSave();
    }

    public void setSelectedTable(int selectedTable) {
        this.selectedTable = selectedTable;
        this.technicalName = getDefaultTechnicalName();
    }
}
