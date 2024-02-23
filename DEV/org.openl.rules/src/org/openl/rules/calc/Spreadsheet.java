package org.openl.rules.calc;

import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

@Executable
public class Spreadsheet extends ExecutableRulesMethod {

    public static final String SPREADSHEETRESULT_TYPE_PREFIX = "SpreadsheetResult";
    public static final String SPREADSHEETRESULT_SHORT_TYPE_PREFIX = "SR";

    private IResultBuilder resultBuilder;

    private SpreadsheetCell[][] cells;

    /**
     * Top left cell of the whole Spreadsheet is not included. So the row names starts from [1, 0] in the Spreadsheet
     * table body
     */
    private String[] rowNames;
    private BidiMap<Integer, Integer> rowOffsets;
    private BidiMap<Integer, Integer> rowDescriptions;
    /**
     * Top left cell is not included. So the column names starts from [0, 1] in the Spreadsheet table body
     */
    private String[] columnNames;
    private BidiMap<Integer, Integer> columnOffsets;
    private BidiMap<Integer, Integer> columnDescriptions;

    private String[] rowNamesForResultModel;

    private String[] columnNamesForResultModel;

    /**
     * Type of the Spreadsheet with all its fields Is some type of internal. Is used on calculating the results of the
     * cells.
     */
    private SpreadsheetOpenClass spreadsheetType;

    /**
     * Invoker for current method.
     */
    private volatile Invokable invoker;

    /**
     * Custom return type of the spreadsheet method. Is a public type of the spreadsheet
     */
    private CustomSpreadsheetResultOpenClass customSpreadsheetResultType;

    public Spreadsheet() {
        super(null, null);
    }

    public Spreadsheet(IOpenMethodHeader header, SpreadsheetBoundNode boundNode) {
        super(header, boundNode);
        initProperties(getSyntaxNode().getTableProperties());
    }

    @Override
    public IOpenClass getType() {
        return customSpreadsheetResultType == null ? super.getType() : customSpreadsheetResultType;
    }

    public void setCustomSpreadsheetResultType(CustomSpreadsheetResultOpenClass spreadsheetCustomResultType) {
        this.customSpreadsheetResultType = spreadsheetCustomResultType;
    }

    @Override
    public SpreadsheetBoundNode getBoundNode() {
        return (SpreadsheetBoundNode) super.getBoundNode();
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    @Override
    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        getBoundNode().updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    public IResultBuilder getResultBuilder() {
        return resultBuilder;
    }

    @Override
    public String getSourceUrl() {
        TableSyntaxNode syntaxNode = getSyntaxNode();
        return syntaxNode == null ? null : syntaxNode.getUri();
    }

    public SpreadsheetOpenClass getSpreadsheetType() {
        return spreadsheetType;
    }

    public int getHeight() {
        return getRowNames().length;
    }

    public void setCells(SpreadsheetCell[][] cells) {
        this.cells = cells;
    }

    public void setColumnNames(String[] colNames) {
        this.columnNames = colNames;
    }

    public void setColumnOffsets(BidiMap<Integer, Integer> columnOffsets) {
        this.columnOffsets = columnOffsets;
    }

    public void setResultBuilder(IResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public void setRowOffsets(BidiMap<Integer, Integer> rowOffsets) {
        this.rowOffsets = rowOffsets;
    }

    public String[] getRowNamesForResultModel() {
        return rowNamesForResultModel;
    }

    public void setRowNamesForResultModel(String[] rowNamesForResultModel) {
        this.rowNamesForResultModel = rowNamesForResultModel;
    }

    public String[] getColumnNamesForResultModel() {
        return columnNamesForResultModel;
    }

    public void setColumnNamesForResultModel(String[] columnNamesForResultModel) {
        this.columnNamesForResultModel = columnNamesForResultModel;
    }

    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public int getWidth() {
        return getColumnNames().length;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public BidiMap<Integer, Integer> getRowOffsets() {
        return rowOffsets;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public BidiMap<Integer, Integer> getColumnOffsets() {
        return columnOffsets;
    }

    public BidiMap<Integer, Integer> getColumnDescriptions() {
        return columnDescriptions;
    }

    public void setColumnDescriptions(Map<Integer, Integer> columnDescriptions) {
        this.columnDescriptions = new DualLinkedHashBidiMap<>(columnDescriptions);
    }

    public BidiMap<Integer, Integer> getRowDescriptions() {
        return rowDescriptions;
    }

    public void setRowDescriptions(Map<Integer, Integer> rowDescriptions) {
        this.rowDescriptions = new DualLinkedHashBidiMap<>(rowDescriptions);
    }

    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        return getInvoker().invoke(target, params, env);
    }

    protected Invokable createInvoker() {
        return new SpreadsheetInvoker(this);
    }

    protected Invokable getInvoker() {
        if (invoker == null) {
            synchronized (this) {
                if (invoker == null) {
                    invoker = createInvoker();
                }
            }
        }
        return invoker;

    }

    public void setInvoker(SpreadsheetInvoker invoker) {
        this.invoker = invoker;
    }

    volatile Map<String, Point> fieldsCoordinates;

    public Map<String, Point> getFieldsCoordinates() {
        if (fieldsCoordinates == null) {
            synchronized (this) {
                if (fieldsCoordinates == null) {
                    fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates2(columnNames, rowNames, columnNamesForResultModel, rowNamesForResultModel);
                }
            }
        }
        return fieldsCoordinates;
    }

}
