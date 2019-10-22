package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.table.xls.builder.TestTableBuilder;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.util.StringUtils;

/**
 * @author Aliaksandr Antonik.
 */
public class TestTableCreationWizard extends TableCreationWizard {

    private SelectItem[] tableItems;

    /**
     * index of the selected item, when selecting table name to test.
     */
    private int selectedTableNameIndex;

    /**
     * Technical name of newly created test table.
     */
    @NotBlank(message = "Cannot be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = INVALID_NAME_MESSAGE)
    private String technicalName;

    private List<TableSyntaxNode> executableTables;

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
        return TestTableBuilder.getDefaultTechnicalName(node);
    }

    /**
     *
     * @return <code>TableSyntaxNode</code> from model, by the technical name of the table we have selected.
     */
    protected TableSyntaxNode getSelectedNode() {
        List<TableSyntaxNode> nodes = getSyntaxNodes();
        if (selectedTableNameIndex < 0 || selectedTableNameIndex >= nodes.size()) {
            throw new IllegalStateException("Not table is selected");
        }
        return nodes.get(selectedTableNameIndex);
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        TableSyntaxNode node = getSelectedNode();

        String header = TestTableBuilder.getHeader(node, technicalName);

        Map<String, String> params = TestTableBuilder.getParams(node);

        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        TestTableBuilder builder = new TestTableBuilder(gridModel);

        Map<String, Object> properties = buildProperties();

        int width = params.size() + 1;
        if (width < TableBuilder.PROPERTIES_MIN_WIDTH && !properties.isEmpty()) {
            width = TableBuilder.PROPERTIES_MIN_WIDTH;
        }
        int height = TableBuilder.HEADER_HEIGHT + 2 + properties.size();
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

    private List<TableSyntaxNode> getSyntaxNodes() {
        if (executableTables == null) {
            TableSyntaxNode[] syntaxNodes = WizardUtils.getXlsModuleNode().getXlsTableSyntaxNodesWithoutErrors();

            executableTables = new ArrayList<>();
            for (TableSyntaxNode syntaxNode : syntaxNodes) {
                if (isExecutableAndTestableNode(syntaxNode)) {
                    executableTables.add(syntaxNode);
                }
            }
        }
        return executableTables;
    }

    @Override
    protected void onCancel() {
        tableItems = null;
        reset();
    }

    @Override
    protected void onStart() {
        selectedTableNameIndex = 0;

        List<TableSyntaxNode> syntaxNodes = getSyntaxNodes();
        List<SelectItem> result = new ArrayList<>();
        String itemName;

        for (int i = 0; i < syntaxNodes.size(); i++) {
            TableSyntaxNode node = syntaxNodes.get(i);
            itemName = node.getMember().getName();

            if (!StringUtils.containsIgnoreCase(itemName, DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME)) {
                result.add(new SelectItem(i, getNodeName(node)));
            }
        }

        tableItems = result.toArray(new SelectItem[result.size()]);
        Arrays.sort(tableItems, (o1, o2) -> o1.getValue().toString().compareTo(o2.getValue().toString()));
    }

    private String getNodeName(TableSyntaxNode syntaxNode) {
        String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        ITableProperties tableProps = syntaxNode.getTableProperties();

        String nodeName = syntaxNode.getMember().getName();
        StringBuilder dimensionBuilder = new StringBuilder();

        if (tableProps != null) {
            for (String dimensionProp : dimensionProps) {
                String propValue = tableProps.getPropertyValueAsString(dimensionProp);

                if (propValue != null && !propValue.isEmpty()) {
                    dimensionBuilder.append(dimensionBuilder.length() == 0 ? "" : ", ")
                        .append(dimensionProp)
                        .append(" = ")
                        .append(propValue);
                }
            }
        }
        if (dimensionBuilder.length() > 0) {
            return nodeName + "[" + dimensionBuilder.toString() + "]";
        } else {
            return nodeName;
        }
    }

    /**
     * Checks if it is possible to create test for current table(table is executable at runtime), and checks if return
     * type of the table is not void.
     */
    private boolean isExecutableAndTestableNode(TableSyntaxNode node) {
        return node.isExecutableNode() && !void.class.equals(node.getMember().getType().getInstanceClass());
    }

    @Override
    protected void onStepFirstVisit(int step) {
        if (step == 2) {
            initWorkbooks();
        }
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableId(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    public void setSelectedTable(int selectedTableNameIndex) {
        this.selectedTableNameIndex = selectedTableNameIndex;
        this.technicalName = getDefaultTechnicalName();
    }

}
