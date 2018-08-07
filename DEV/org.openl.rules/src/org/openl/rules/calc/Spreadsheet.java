package org.openl.rules.calc;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.meta.TableMetaInfo;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

@Executable
public class Spreadsheet extends ExecutableRulesMethod {

    public static final String SPREADSHEETRESULT_TYPE_PREFIX = "SpreadsheetResult";

    private IResultBuilder resultBuilder;

    private SpreadsheetCell[][] cells;

    /**
     * Top left cell of the whole Spreadsheet is not included.
     * So the row names starts from [1, 0] in the Spreadsheet table body
     */
    private String[] rowNames;

    /**
     * Top left cell is not included.
     * So the column names starts from [0, 1] in the Spreadsheet table body
     */
    private String[] columnNames;
    
    private String[] rowTitles;

    private String[] columnTitles;

    /**
     * Type of the Spreadsheet with all its fields Is some type of internal. Is
     * used on calculating the results of the cells.
     */
    private SpreadsheetOpenClass spreadsheetType;

    /**
     * Invoker for current method.
     */
    private volatile Invokable invoker;

    /**
     * Custom return type of the spreadsheet method. Is a public type of the
     * spreadsheet
     */
    private CustomSpreadsheetResultOpenClass spreadsheetCustomType;

    /**
     * Whether <code>spreadsheetCustomType</code> should be generated or not.
     */
    private boolean customSpreadsheetType;

    public Spreadsheet() {
        super(null, null);
    }
    
    public Spreadsheet(IOpenMethodHeader header, SpreadsheetBoundNode boundNode, boolean customSpreadsheetType) {
        super(header, boundNode);
        initProperties(getSyntaxNode().getTableProperties());
        this.customSpreadsheetType = customSpreadsheetType;
    }

    public Spreadsheet(IOpenMethodHeader header, SpreadsheetBoundNode boundNode) {
        this(header, boundNode, false);
    }

    Constructor<?> constructor;

    public synchronized Constructor<?> getResultConstructor() throws SecurityException, NoSuchMethodException {
        if (constructor == null)
            constructor = this.getType().getInstanceClass()
                    .getConstructor(Object[][].class, String[].class, String[].class, String[].class, String[].class, Map.class);
        return constructor;
    }

    @Override
    public IOpenClass getType() {
        if (isCustomSpreadsheetType()) {
            return getCustomSpreadsheetResultType();
        } else {
            return super.getType();
        }
    }

    public boolean isCustomSpreadsheetType() {
        return customSpreadsheetType;
    }

    private synchronized CustomSpreadsheetResultOpenClass getCustomSpreadsheetResultType() {
        if (spreadsheetCustomType == null) {
            initCustomSpreadsheetResultType();
        }
        return spreadsheetCustomType;
    }
    
    private void initCustomSpreadsheetResultType() {
        Map<String, IOpenField> spreadsheetOpenClassFields = getSpreadsheetType().getFields();
        spreadsheetOpenClassFields.remove("this");
        String typeName = SPREADSHEETRESULT_TYPE_PREFIX + getName();
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(typeName, getRowNames(), getColumnNames(), getRowTitles(), getColumnTitles());
        customSpreadsheetResultOpenClass.setMetaInfo(new TableMetaInfo("Spreadsheet", getName(), getSourceUrl()));
        for (IOpenField field : spreadsheetOpenClassFields.values()) {
            CustomSpreadsheetResultField customSpreadsheetResultField = new CustomSpreadsheetResultField(spreadsheetCustomType, field.getName(), field.getType());
            customSpreadsheetResultOpenClass.addField(customSpreadsheetResultField);
        }
        spreadsheetCustomType = customSpreadsheetResultOpenClass;
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        getBoundNode().updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    public IResultBuilder getResultBuilder() {
        return resultBuilder;
    }

    public String getSourceUrl() {
        TableSyntaxNode syntaxNode = getSyntaxNode();
        return syntaxNode == null ? null : syntaxNode.getUri();
    }

    public SpreadsheetOpenClass getSpreadsheetType() {
        return spreadsheetType;
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
    
    public void setRowTitles(String[] rowTitles) {
        this.rowTitles = rowTitles;
    }
    
    public String[] getRowTitles() {
        return rowTitles;
    }
    
    public void setColumnTitles(String[] columnTitles) {
        this.columnTitles = columnTitles;
    }
    
    public String[] getColumnTitles() {
        return columnTitles;
    }

    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public int getWidth() {
        return cells.length == 0 ? 0 : cells[0].length;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

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
    public int height() {
        return getHeight();
    }

    @Deprecated
    public int width() {
        return getWidth();
    }

    public void setInvoker(SpreadsheetInvoker invoker) {
        this.invoker = invoker;
    }
    
    Map<String, Point> fieldsCoordinates = null;

    public synchronized Map<String, Point> getFieldsCoordinates() {
        if (fieldsCoordinates == null) {
            if (isCustomSpreadsheetType()) {
                fieldsCoordinates = getCustomSpreadsheetResultType().getFieldsCoordinates();
            } else {
                fieldsCoordinates = DefaultResultBuilder.getFieldsCoordinates(this.getSpreadsheetType().getFields());
            }
        }
        return fieldsCoordinates;
    }
}
