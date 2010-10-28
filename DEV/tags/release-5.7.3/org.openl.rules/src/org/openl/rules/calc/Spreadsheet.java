package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

@Executable
public class Spreadsheet extends AMethod implements IMemberMetaInfo {    

    private SpreadsheetBoundNode node;

    protected IResultBuilder resultBuilder;

    private SpreadsheetCell[][] cells;
    private String[] rowNames;
    private String[] columnNames;

    private SpreadsheetOpenClass spreadsheetType;
    private Map<String, Object> properties;
    
    /**
     * Invoker for current method.
     */
    private Invokable invoker;

    public Spreadsheet(IOpenMethodHeader header, SpreadsheetBoundNode boundNode) {
        super(header);
        this.node = boundNode;
        properties = ((TableSyntaxNode) node.getSyntaxNode()).getTableProperties().getAllProperties();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setBoundNode(SpreadsheetBoundNode node) {
        this.node = node;
    }

    public SpreadsheetBoundNode getBoundNode() {
        return node;
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    public BindingDependencies getDependencies() {
        return null;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public IResultBuilder getResultBuilder() {
        return resultBuilder;
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) node.getSyntaxNode()).getUri();
    }

    public SpreadsheetOpenClass getSpreadsheetType() {
        return spreadsheetType;
    }

    public TableSyntaxNode getSyntaxNode() {
        return node.getTableSyntaxNode();
    }

    public int getHeight() {
        return cells.length;
    }

    public void setCells(SpreadsheetCell[][] cells) {
        this.cells = cells;
    }

    public void setColumnNames(String[] colNames) {
        this.columnNames = colNames;
    }

    public void setResultBuilder(IResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public int getWidth() {
        return cells[0].length;
    }
    
    public String[] getRowNames() {
        return rowNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            invoker = new SpreadsheetInvoker(this);
        } 
        return invoker.invoke(target, params, env);
    }
    
    public List<SpreadsheetCell> listNonEmptyCells(SpreadsheetHeaderDefinition definition) {
        
        List<SpreadsheetCell> list = new ArrayList<SpreadsheetCell>();
        
        int row = definition.getRow();
        int col = definition.getColumn();

        if (row >= 0) {
            for (int i = 0; i < getWidth(); ++i) {
                if (!cells[row][i].isEmpty()) {
                    list.add(cells[row][i]);
                }
            }
        } else {
            for (int i = 0; i < getHeight(); ++i) {
                if (!cells[i][col].isEmpty()) {
                    list.add(cells[i][col]);
                }
            }
        }

        return list;
    }
    
    @Deprecated 
    public int height()
    {
        return getHeight();
    }
    
    @Deprecated
    public int width()
    {
        return getWidth();
    }

}
