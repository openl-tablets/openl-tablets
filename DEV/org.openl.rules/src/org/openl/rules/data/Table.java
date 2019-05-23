package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.util.BiMap;
import org.openl.vm.IRuntimeEnv;

public class Table implements ITable {

    private ILogicalTable logicalTable;
    private ITableModel dataModel;

    private String tableName;
    private TableSyntaxNode tableSyntaxNode;

    private Object dataArray;

    private BiMap<Integer, Object> rowIndexMap;
    private BiMap<Integer, String> primaryIndexMap;
    private Map<Integer, Integer> dataIdxToTableRowNum;

    public Table(ITableModel dataModel, ILogicalTable data) {
        this.dataModel = dataModel;
        this.logicalTable = data;
    }

    public Table(String tableName, TableSyntaxNode tsn) {
        this.tableName = tableName;
        this.tableSyntaxNode = tsn;
    }

    @Override
    public void setData(ILogicalTable dataWithHeader) {
        logicalTable = dataWithHeader;
    }

    @Override
    public ILogicalTable getData() {
        return logicalTable;
    }

    @Override
    public void setModel(ITableModel dataModel) {
        this.dataModel = dataModel;
    }

    @Override
    public String getColumnDisplay(int n) {
        return dataModel.getDescriptor(n).getDisplayName();
    }

    @Override
    public int getColumnIndex(String columnName) {
        for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
            if (descriptor.getName().equals(columnName)) {
                return descriptor.getColumnIdx();
            }
        }

        return -1;
    }

    @Override
    public String getColumnName(int n) {
        ColumnDescriptor columnDescriptor = dataModel.getDescriptor(n);
        return columnDescriptor != null ? columnDescriptor.getName() : null;
    }

    @Override
    public IOpenClass getColumnType(int n) {
        ColumnDescriptor descriptor = dataModel.getDescriptor(n);

        if (!descriptor.isConstructor()) {
            return descriptor.getType();
        }

        return null;
    }

    @Override
    public Object getData(int row) {
        return Array.get(dataArray, row);
    }

    @Override
    public Object getDataArray() {
        return dataArray;
    }

    @Override
    public ITableModel getDataModel() {
        return dataModel;
    }

    @Override
    public IGridTable getHeaderTable() {
        return logicalTable.getRow(0).getSource();
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public int getNumberOfColumns() {
        return dataModel.getDescriptors().length;
    }

    @Override
    public ColumnDescriptor getColumnDescriptor(int i) {
        return dataModel.getDescriptor(i);
    }

    @Override
    public int getNumberOfRows() {
        return logicalTable.getHeight() - 1;
    }

    @Override
    public synchronized String getPrimaryIndexKey(int row) {
        if (primaryIndexMap == null) {
            return null;
        }
        return primaryIndexMap.get(row);
    }

    @Override
    public Integer getRowIndex(Object target) {
        return rowIndexMap.getKey(target);
    }

    @Override
    public IGridTable getRowTable(int row) {
        return logicalTable.getRow(row + 1).getSource();
    }

    @Override
    public int getSize() {
        return Array.getLength(dataArray);
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    @Override
    public Map<String, Integer> getUniqueIndex(int columnIndex) throws SyntaxNodeException {
        ColumnDescriptor descriptor = dataModel.getDescriptor(columnIndex);

        return descriptor.getUniqueIndex(this, columnIndex);
    }

    @Override
    public Object getValue(int col, int row) {
        int startRows = getStartRowForData();
        int idx = row - startRows;
        Object rowObject = rowIndexMap == null ? Array.get(dataArray, idx) : rowIndexMap.get(idx);

        return dataModel.getDescriptor(col).getColumnValue(rowObject);
    }

    @Override
    public Map<String, Integer> makeUniqueIndex(int colIdx) throws SyntaxNodeException {
        Map<String, Integer> index = new HashMap<>();

        if (dataIdxToTableRowNum == null || dataIdxToTableRowNum.isEmpty()) {
            return Collections.emptyMap();
        }

        for (Map.Entry<Integer, Integer> entry : dataIdxToTableRowNum.entrySet()) {
            IGridTable gridTable = logicalTable.getSubtable(colIdx, entry.getValue(), 1, 1).getSource();
            String key = gridTable.getCell(0, 0).getStringValue();

            if (key == null) {
                throw SyntaxNodeExceptionUtils.createError("Empty key in an unique index",
                    new GridCellSourceCodeModule(gridTable));
            }

            key = key.trim();

            if (index.containsKey(key)) {
                throw SyntaxNodeExceptionUtils.createError("Duplicated key in an unique index: " + key,
                    new GridCellSourceCodeModule(gridTable));
            }

            index.put(key, entry.getKey());
        }

        return Collections.unmodifiableMap(index);
    }

    @Override
    public List<Object> getUniqueValues(int colIdx) throws SyntaxNodeException {

        List<Object> values = new ArrayList<>();

        if (dataIdxToTableRowNum == null || dataIdxToTableRowNum.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<Integer, Integer> entry : dataIdxToTableRowNum.entrySet()) {

            IGridTable gridTable = logicalTable.getSubtable(colIdx, entry.getValue(), 1, 1).getSource();
            Object value = gridTable.getCell(0, 0).getObjectValue();

            if (value == null) {
                throw SyntaxNodeExceptionUtils.createError("Empty key in an unique index",
                    new GridCellSourceCodeModule(gridTable));
            }

            if (values.contains(value)) {
                throw SyntaxNodeExceptionUtils.createError("Duplicated key in an unique index: " + value,
                    new GridCellSourceCodeModule(gridTable));
            }

            values.add(value);
        }

        return values;
    }

    @Override
    public void populate(IDataBase dataBase, IBindingContext bindingContext) throws Exception {

        int rows = logicalTable.getHeight();
        int columns = logicalTable.getWidth();

        Collection<SyntaxNodeException> errorSyntaxNodeExceptions = new ArrayList<>(0);

        int dataArrayLength = Array.getLength(dataArray);
        for (int i = 0; i < dataArrayLength; i++) {
            Object target = Array.get(dataArray, i);

            int rowNum = dataIdxToTableRowNum.get(i);
            //calculate height
            int height;
            if (i + 1 < dataArrayLength) {
                height = dataIdxToTableRowNum.get(i + 1) - rowNum;
            } else {
                height = rows - rowNum;
            }

            for (int j = 0; j < columns; j++) {

                ColumnDescriptor descriptor = dataModel.getDescriptor(j);

                if (descriptor instanceof ForeignKeyColumnDescriptor) {
                    ForeignKeyColumnDescriptor fkDescriptor = (ForeignKeyColumnDescriptor) descriptor;

                    if (fkDescriptor.isReference()) {
                        try {
                            if (descriptor.isConstructor()) {
                                target = fkDescriptor.getLiteralByForeignKey(dataModel.getType(),
                                    logicalTable.getSubtable(j, rowNum, 1, height),
                                    dataBase,
                                    bindingContext);
                            } else {
                                fkDescriptor.populateLiteralByForeignKey(target,
                                    logicalTable.getSubtable(j, rowNum, 1, height),
                                    dataBase,
                                    bindingContext);
                            }
                        } catch (SyntaxNodeException e) {
                            boolean found = false;
                            for (SyntaxNodeException syntaxNodeException : errorSyntaxNodeExceptions) {
                                if (syntaxNodeException.getMessage()
                                    .equals(
                                        e.getMessage()) && syntaxNodeException.getSyntaxNode() == e.getSyntaxNode()) {
                                    found = true;
                                }
                            }
                            if (!found) {
                                errorSyntaxNodeExceptions.add(e);
                            }
                        }
                    }
                }
            }
        }
        if (!errorSyntaxNodeExceptions.isEmpty()) {
            throw new CompositeSyntaxNodeException("Parsing Error:",
                errorSyntaxNodeExceptions.toArray(new SyntaxNodeException[0]));
        }
    }

    @Override
    public void preLoad(OpenlToolAdaptor openlAdapter) throws Exception {
        int rows = logicalTable.getHeight();
        int startRow = getStartRowForData();

        if (tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_DATA && isSupportMultirow()) {
            //process not merged rows as merged if they have the same value in first column
            List<Object> resultContainer = new ArrayList<>();

            processMultirowDataTable(resultContainer, openlAdapter, startRow, rows);

            dataArray = Array.newInstance(dataModel.getInstanceClass(), resultContainer.size());
            for (int i = 0; i < resultContainer.size(); i++) {
                Array.set(dataArray, i, resultContainer.get(i));
            }
        } else {
            dataArray = Array.newInstance(dataModel.getInstanceClass(), rows - startRow);
            for (int rowNum = startRow; rowNum < rows; rowNum++) {
                processRow(openlAdapter, startRow, rowNum);
            }
        }
    }

    private boolean isSupportMultirow() {
        if (dataModel.getDescriptors().length > 0) {
            for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
                if (descriptor.isSupportMultirows()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processMultirowDataTable(List<Object> resultContainer,
                                          OpenlToolAdaptor openlAdapter,
                                          int startRow, int rows) throws OpenLCompilationException {

        //group descriptors by KEY
        Map<ColumnDescriptor.ColumnGroupKey, List<ColumnDescriptor>> descriptorGroups = new TreeMap<>();
        for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
            ColumnDescriptor.ColumnGroupKey key = descriptor.getGroupKey();
            List<ColumnDescriptor> descriptorsByKey = descriptorGroups.computeIfAbsent(key, k -> new ArrayList<>());
            if (descriptor.getField() != null && !descriptor.isReference()) {
                descriptorsByKey.add(descriptor);
            }
        }

        IRuntimeEnv env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        try {
            parseRowsAndPopulateRootLiteral(resultContainer, new ArrayList<>(descriptorGroups.values()), openlAdapter, env, startRow, rows);
        } catch (SyntaxNodeException e) {
            tableSyntaxNode.addError(e);
            openlAdapter.getBindingContext().addError(e);
        }
    }

    private void parseRowsAndPopulateRootLiteral(List<Object> resultContainer,
                                    List<List<ColumnDescriptor>> allDescriptors,
                                    OpenlToolAdaptor openlAdapter,
                                    IRuntimeEnv env, int startRow, int rows) throws OpenLCompilationException {

        List<ColumnDescriptor> descriptors = allDescriptors.get(0);

        Object[][] rowValues = new Object[rows - startRow][descriptors.size()];
        for (int rowNum = startRow; rowNum < rows; rowNum++) {
            for (int colNum = 0; colNum < descriptors.size(); colNum++) {
                ColumnDescriptor descriptor = descriptors.get(colNum);
                ILogicalTable valuesTable = LogicalTableHelper.make1ColumnTable(logicalTable.getSubtable(descriptor.getColumnIdx(), rowNum, 1, 1));
                Object prevRes = ColumnDescriptor.PREV_RES_EMPTY;
                for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
                    ILogicalTable logicalTable = LogicalTableHelper
                            .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                            .getSubtable(0, 0, 1, 1);
                    Object res = descriptor.parseCellValue(logicalTable, openlAdapter);
                    if (!(descriptor.isSameValue(res, prevRes))) {
                        rowValues[rowNum - startRow][colNum] = res;
                        prevRes = res;
                    }
                }
            }
        }

        for (int rowNum = 0; rowNum < rowValues.length; rowNum++) {
            int height = 1;
            Object[] thisRow = rowValues[rowNum];
            if (thisRow == null) {
                continue;
            }
            Object literal = createLiteral();
            addToRowIndex(rowNum, literal);
            for (int j = rowNum + 1; j < rowValues.length; j++) {
                Object[] nextRow = rowValues[j];
                boolean isSameRow = true;
                for (int k = 0; k < thisRow.length; k++) {
                    isSameRow = descriptors.get(k).isSameValue(nextRow[k], thisRow[k]);
                    if (!isSameRow) {
                        break;
                    }
                }
                if (isSameRow) {
                    rowValues[j] = null;
                    addToRowIndex(j, literal);
                    height++;
                } else {
                    break;
                }
            }

            DatatypeArrayMultiRowElementContext context = new DatatypeArrayMultiRowElementContext();
            env.pushLocalFrame(new Object[] { context });
            env.pushThis(literal);
            try {
                for (List<ColumnDescriptor> allDescriptor : allDescriptors) {
                    parseRowsAndPopulateLiteral(literal, allDescriptor, openlAdapter, env, rowNum + startRow, height);
                }
                bindDataIndexWithTableRowNum(resultContainer.size(), rowNum + startRow);
                resultContainer.add(literal);
            } finally {
                env.popThis();
                env.popLocalFrame();
            }
        }
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private void parseRowsAndPopulateLiteral(Object literal,
                                             List<ColumnDescriptor> descriptors,
                                             OpenlToolAdaptor openlAdapter,
                                             IRuntimeEnv env, int rowNum, int height) throws OpenLCompilationException {

        if (descriptors.isEmpty()) {
            return;
        }
        DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];

        Object[][] rowValues = null;
        for (int colNum = 0; colNum < descriptors.size(); colNum++) {
            ColumnDescriptor descriptor = descriptors.get(colNum);
            ILogicalTable valuesTable = LogicalTableHelper.make1ColumnTable(logicalTable.getSubtable(descriptor.getColumnIdx(), rowNum, 1, height));
            if (rowValues == null) {
                rowValues = new Object[valuesTable.getSource().getHeight()][descriptors.size()];
            }
            for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
                ILogicalTable logicalTable = LogicalTableHelper
                        .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                        .getSubtable(0, 0, 1, 1);
                rowValues[i][colNum] = descriptor.parseCellValue(logicalTable, openlAdapter);
            }
        }

        ColumnDescriptor pkDescriptor = descriptors.get(0);

        Object[] prevRow = null;
        for (int i = 0; i < rowValues.length; i++) {
            boolean isSameRow;
            Object[] thisRow = rowValues[i];
            context.setRow(i);
            if (prevRow == null) {
                isSameRow = false;
            } else {
                if (pkDescriptor.isPrimaryKey()) {
                    isSameRow = pkDescriptor.isSameValue(thisRow[0], prevRow[0]);
                } else {
                    isSameRow = true;
                    for (int k = 0; k < thisRow.length; k++) {
                        isSameRow = descriptors.get(k).isSameValue(thisRow[k], prevRow[k]);
                        if (!isSameRow) {
                            break;
                        }
                    }
                }
            }
            context.setRowValueIsTheSameAsPrevious(isSameRow);
            for (int k = 0; k < thisRow.length; k++) {
                ColumnDescriptor descriptor = descriptors.get(k);
                Object thisValue = thisRow[k];
                if (descriptor.isValuesAnArray()) {
                    Object currentValue = descriptor.getFieldValue(literal, env);
                    int thisLen = Array.getLength(thisValue);
                    if (currentValue == null || Array.getLength(currentValue) == 0) {
                        descriptor.setFieldValue(literal, thisLen == 0 ? null : thisValue, env);
                    } else if (thisLen != 0) {
                        int currentLen = Array.getLength(currentValue);
                        Object newArray = Array.newInstance(thisValue.getClass().getComponentType(), currentLen + thisLen);
                        System.arraycopy(currentValue, 0, newArray, 0, currentLen);
                        System.arraycopy(thisValue, 0, newArray, currentLen, thisLen);
                        descriptor.setFieldValue(literal, newArray, env);
                    }
                } else {
                    descriptor.setFieldValue(literal, thisValue, env);
                }
            }

            prevRow = thisRow;
        }

    }

    private Object createLiteral() throws OpenLCompilationException {
        if (dataModel.getInstanceClass().isArray()) {
            int dim = 0;
            Class<?> type = dataModel.getInstanceClass();
            while (type.isArray()) {
                type = type.getComponentType();
                dim++;
            }
            return Array.newInstance(type, new int[dim]);
        } else {
            Object literal = dataModel.newInstance();
            if (literal == null) {
                throw new OpenLCompilationException(String.format("Can`t create instance of %s", dataModel.getName()));
            }
            return literal;
        }
    }

    private void processRow(OpenlToolAdaptor openlAdapter, int startRow, int rowNum) throws OpenLCompilationException {

        boolean constructor = isConstructor();
        Object literal = null;

        int rowIndex = rowNum - startRow;

        if (!constructor) {
            literal = createLiteral();
            addToRowIndex(rowIndex, literal);
        }

        IRuntimeEnv env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        env.pushLocalFrame(new Object[] { new DatatypeArrayMultiRowElementContext() });
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            literal = processColumn(columnDescriptor, openlAdapter, constructor, rowNum, literal, env);
        }
        env.popLocalFrame();
        if (literal == null) {
            literal = dataModel.getType().nullObject();
        }

        int idx = rowNum - startRow;
        bindDataIndexWithTableRowNum(idx, rowNum);
        Array.set(dataArray, idx, literal);
    }

    private void bindDataIndexWithTableRowNum(Integer idx, Integer rowNum) {
        if (dataIdxToTableRowNum == null) {
            dataIdxToTableRowNum = new HashMap<>();
        }
        dataIdxToTableRowNum.put(idx, rowNum);
    }

    private Object processColumn(ColumnDescriptor columnDescriptor, OpenlToolAdaptor openlAdapter,
            boolean constructor,
            int rowNum,
            Object literal,
            IRuntimeEnv env) throws SyntaxNodeException {

        if (columnDescriptor != null && !columnDescriptor.isReference()) {
            if (constructor) {
                literal = columnDescriptor
                    .getLiteral(dataModel.getType(), logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1), openlAdapter);
            } else {
                try {
                    ILogicalTable lTable = logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1);
                    if (!(lTable.getHeight() == 1 && lTable.getWidth() == 1) || lTable.getCell(0, 0)
                        .getStringValue() != null) { // EPBDS-6104. For empty values should be used data type default
                        // value.
                        return columnDescriptor.populateLiteral(literal, lTable, openlAdapter, env);
                    }
                } catch (SyntaxNodeException ex) {
                    tableSyntaxNode.addError(ex);
                    openlAdapter.getBindingContext().addError(ex);
                }
            }
        }

        return literal;
    }

    @Override
    public synchronized void setPrimaryIndexKey(int row, String value) {
        if (primaryIndexMap == null) {
            primaryIndexMap = new BiMap<>();
        }
        Integer oldRow = primaryIndexMap.getKey(value);
        if (oldRow != null && row != oldRow) {
            throw new OpenLRuntimeException("Duplicated key: " + value + " in rows " + oldRow + " and " + row);
        }
        primaryIndexMap.put(row, value);
    }

    @Override
    public Object findObject(int columnIndex, String skey, IBindingContext cxt) throws SyntaxNodeException {
        Map<String, Integer> index = getUniqueIndex(columnIndex);

        Integer found = index.get(skey);

        if (found == null) {
            return null;
        }

        return Array.get(dataArray, found);
    }

    private void addToRowIndex(int rowIndex, Object target) {
        if (rowIndexMap == null) {
            rowIndexMap = new BiMap<>();
        }
        rowIndexMap.put(rowIndex, target);
    }

    /**
     * @return Start row for data rows from Data_With_Titles rows. It depends on if table has or no column title row.
     */
    private int getStartRowForData() {
        if (dataModel.hasColumnTitleRow()) {
            return 1;
        }

        return 0;
    }

    private boolean isConstructor() {
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            if (columnDescriptor.isConstructor()) {
                return true;
            }
        }
        return false;
    }

}
