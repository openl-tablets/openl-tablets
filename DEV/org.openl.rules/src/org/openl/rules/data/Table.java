package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.util.BiMap;
import org.openl.util.MessageUtils;
import org.openl.vm.IRuntimeEnv;

public class Table implements ITable {

    private ILogicalTable logicalTable;
    private ITableModel dataModel;

    private String tableName;
    private TableSyntaxNode tableSyntaxNode;

    private Object dataArray;
    private List<DatatypeArrayMultiRowElementContext> dataContextCache;

    private BiMap<Integer, Object> rowIndexMap;
    private BiMap<Integer, String> primaryIndexMap;
    private Map<Integer, Integer> dataIdxToTableRowNum;
    private XlsNodeTypes xlsNodeType;
    private String uri;

    public Table(ITableModel dataModel, ILogicalTable data) {
        this.dataModel = dataModel;
        this.logicalTable = data;
    }

    public Table(String tableName, TableSyntaxNode tsn) {
        this.tableName = tableName;
        this.tableSyntaxNode = tsn;
        this.xlsNodeType = tsn.getNodeType();
        this.uri = tsn.getUri();
    }

    @Override
    public void clearOddDataForExecutionMode() {
        this.tableSyntaxNode = null;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public XlsNodeTypes getXlsNodeType() {
        return xlsNodeType;
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
        var columnDescriptor = dataModel.getDescriptor(n);
        return columnDescriptor != null ? columnDescriptor.getName() : null;
    }

    @Override
    public IOpenClass getColumnType(int n) {
        var descriptor = dataModel.getDescriptor(n);

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
    public Object getValue(int col, int row) {
        var startRows = getStartRowForData();
        var idx = row - startRows;
        Object rowObject = rowIndexMap == null ? Array.get(dataArray, idx) : rowIndexMap.get(idx);

        return dataModel.getDescriptor(col).getColumnValue(rowObject);
    }

    @Override
    public Map<String, Integer> makeUniqueIndex(int colIdx, IBindingContext cxt) {
        var index = new HashMap<String, Integer>();

        if (dataIdxToTableRowNum == null || dataIdxToTableRowNum.isEmpty()) {
            return Collections.emptyMap();
        }

        for (Map.Entry<Integer, Integer> entry : dataIdxToTableRowNum.entrySet()) {
            var gridTable = logicalTable.getSubtable(colIdx, entry.getValue(), 1, 1).getSource();
            var key = gridTable.getCell(0, 0).getStringValue();

            if (key == null) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(MessageUtils.EMPTY_UNQ_IDX_KEY,
                        new GridCellSourceCodeModule(gridTable));
                cxt.addError(error);
                break;
            }

            key = key.trim();

            if (index.containsKey(key)) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                        MessageUtils.getDuplicatedKeyIndexErrorMessage(key),
                        new GridCellSourceCodeModule(gridTable));
                cxt.addError(error);
                break;
            }

            index.put(key, entry.getKey());
        }

        return Collections.unmodifiableMap(index);
    }

    @Override
    public Collection<Object> getUniqueValues(int colIdx) throws SyntaxNodeException {

        var values = new LinkedHashSet<>();

        if (dataIdxToTableRowNum == null || dataIdxToTableRowNum.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<Integer, Integer> entry : dataIdxToTableRowNum.entrySet()) {

            var gridTable = logicalTable.getSubtable(colIdx, entry.getValue(), 1, 1).getSource();
            var value = gridTable.getCell(0, 0).getObjectValue();

            if (value == null) {
                throw SyntaxNodeExceptionUtils.createError(MessageUtils.EMPTY_UNQ_IDX_KEY,
                        new GridCellSourceCodeModule(gridTable));
            }

            if (!values.add(value)) {
                throw SyntaxNodeExceptionUtils.createError(
                        MessageUtils.getDuplicatedKeyIndexErrorMessage(String.valueOf(value)),
                        new GridCellSourceCodeModule(gridTable));
            }
        }

        return values;
    }

    @Override
    public void populate(IDataBase dataBase, IBindingContext bindingContext) throws Exception {

        var rows = logicalTable.getHeight();
        var columns = logicalTable.getWidth();

        var hasError = validateOnErrors(bindingContext, dataBase, columns);

        if (hasError) {
            return;
        }

        var dataArrayLength = Array.getLength(dataArray);
        for (var i = 0; i < dataArrayLength; i++) {
            var env = bindingContext.getOpenL().getVm().getRuntimeEnv();
            Object target = Array.get(dataArray, i);
            env.pushThis(target);

            var rowNum = dataIdxToTableRowNum.get(i);
            // calculate height
            int height;
            if (i + 1 < dataArrayLength) {
                height = dataIdxToTableRowNum.get(i + 1) - rowNum;
            } else {
                height = rows - rowNum;
            }

            var context = getCachedContext(i);
            if (context == null) {
                context = new DatatypeArrayMultiRowElementContext();
            }
            env.pushLocalFrame(new Object[]{context});
            for (var j = 0; j < columns; j++) {
                var descriptor = dataModel.getDescriptor(j);

                if (descriptor instanceof ForeignKeyColumnDescriptor fkDescriptor) {

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
                                        bindingContext,
                                        env);
                            }
                        } catch (SyntaxNodeException e) {
                            bindingContext.addError(e);
                        }
                    }
                }
            }
            env.popLocalFrame();
            env.popThis();
        }
        // clear cache
        dataContextCache = null;
    }

    private boolean validateOnErrors(IBindingContext bindingContext, IDataBase dataBase, int columns) {
        var hasError = false;
        // Validation
        for (var j = 0; j < columns; j++) {
            SyntaxNodeException ex = null;
            var descriptor = dataModel.getDescriptor(j);
            if (descriptor instanceof ForeignKeyColumnDescriptor fkDescriptor) {
                if (fkDescriptor.isReference()) {
                    var foreignKeyTable = fkDescriptor.getForeignKeyTable();
                    var foreignKey = fkDescriptor.getForeignKey();
                    var foreignKeyTableName = foreignKeyTable.getIdentifier();
                    var foreignTable = dataBase.getTable(foreignKeyTableName);

                    if (foreignTable == null) {
                        String message = MessageUtils.getTableNotFoundErrorMessage(foreignKeyTableName);
                        ex = SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                    } else {
                        if (foreignKey != null) {
                            var columnName = foreignKey.getIdentifier();
                            var foreignKeyIndex = foreignTable.getColumnIndex(columnName);
                            if (foreignKeyIndex == -1) {
                                String message = MessageUtils.getColumnNotFoundErrorMessage(columnName);
                                ex = SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
                            } else {
                                foreignTable.getColumnDescriptor(foreignKeyIndex)
                                        .getUniqueIndex(foreignTable, foreignKeyIndex, bindingContext);
                            }
                        } else {
                            // we don't have defined PK lets use first key as PK
                            var foreignKeyIndex = 0;
                            var dataModel = foreignTable.getDataModel();
                            var d1 = dataModel.getDescriptors()[0];
                            if (d1.isPrimaryKey()) {
                            } else {
                                var firstColDescriptor = dataModel.getDescriptor(0);
                                if (firstColDescriptor.isPrimaryKey()) {
                                    // first column is primary key for another level. So return column index for first
                                    // descriptor
                                    foreignKeyIndex = descriptor.getColumnIdx();
                                }
                                foreignTable.getColumnDescriptor(foreignKeyIndex)
                                        .getUniqueIndex(foreignTable, foreignKeyIndex, bindingContext);

                            }

                            var errors = bindingContext.getErrors();
                            for (SyntaxNodeException error : errors) {
                                var sourceLocation = error.getSourceLocation();
                                if (sourceLocation != null && foreignTable.getTableSyntaxNode()
                                        .getUriParser()
                                        .intersects(new XlsUrlParser(sourceLocation))) {
                                    String message = MessageUtils
                                            .getForeignTableCompilationErrorsMessage(foreignKeyTableName);
                                    ex = SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                                }
                            }
                        }
                    }
                }
            }
            if (ex != null) {
                bindingContext.addError(ex);
                hasError = true;
            }
        }
        return hasError;
    }

    @Override
    public void preLoad(OpenlToolAdaptor openlAdapter) throws Exception {
        var rows = logicalTable.getHeight();
        var startRow = getStartRowForData();

        if (tableSyntaxNode.getNodeType() == XlsNodeTypes.XLS_DATA && isSupportMultirow()) {
            // process not merged rows as merged if they have the same value in first column
            var resultContainer = new ArrayList<Object>();
            var dataContexts = new ArrayList<DatatypeArrayMultiRowElementContext>();

            processMultirowDataTable(resultContainer, openlAdapter, dataContexts, startRow, rows);

            dataArray = Array.newInstance(dataModel.getInstanceClass(), resultContainer.size());
            for (var i = 0; i < resultContainer.size(); i++) {
                Array.set(dataArray, i, resultContainer.get(i));
            }
            this.dataContextCache = Collections.unmodifiableList(dataContexts);
        } else {
            dataArray = Array.newInstance(dataModel.getInstanceClass(), rows - startRow);
            for (var rowNum = startRow; rowNum < rows; rowNum++) {
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
                                          List<DatatypeArrayMultiRowElementContext> dataContexts,
                                          int startRow,
                                          int rows) throws OpenLCompilationException {

        // group descriptors by KEY
        var descriptorGroups = new TreeMap<ColumnDescriptor.ColumnGroupKey, List<ColumnDescriptor>>();
        for (ColumnDescriptor descriptor : dataModel.getDescriptors()) {
            var key = descriptor.getGroupKey();
            var descriptorsByKey = descriptorGroups.computeIfAbsent(key, k -> new ArrayList<>());
            if (descriptor.getField() != null && !descriptor.isReference()) {
                descriptorsByKey.add(descriptor);
            }
        }

        try {
            parseRowsAndPopulateRootLiteral(resultContainer,
                    dataContexts,
                    new ArrayList<>(descriptorGroups.values()),
                    openlAdapter,
                    startRow,
                    rows);
        } catch (SyntaxNodeException e) {
            openlAdapter.getBindingContext().addError(e);
        }
    }

    private void parseRowsAndPopulateRootLiteral(List<Object> resultContainer,
                                                 List<DatatypeArrayMultiRowElementContext> dataContexts,
                                                 List<List<ColumnDescriptor>> allDescriptors,
                                                 OpenlToolAdaptor openlAdapter,
                                                 int startRow,
                                                 int rows) throws OpenLCompilationException {

        List<ColumnDescriptor> descriptors = allDescriptors.getFirst();

        Object[][] rowValues = new Object[rows - startRow][descriptors.size()];
        for (var rowNum = startRow; rowNum < rows; rowNum++) {
            for (var colNum = 0; colNum < descriptors.size(); colNum++) {
                var descriptor = descriptors.get(colNum);
                ILogicalTable valuesTable = LogicalTableHelper
                        .make1ColumnTable(logicalTable.getSubtable(descriptor.getColumnIdx(), rowNum, 1, 1));
                var prevRes = ColumnDescriptor.PREV_RES_EMPTY;
                var width = valuesTable.getSource().getWidth();
                for (var i = 0; i < valuesTable.getSource().getHeight(); i++) {
                    ILogicalTable logicalTable = LogicalTableHelper.make1ColumnTable(
                            LogicalTableHelper.logicalTable(valuesTable.getSource().getSubtable(0, i, width, i + 1))
                                    .getSubtable(0, 0, width, 1));
                    var res = descriptor.parseCellValue(logicalTable, openlAdapter);
                    if (!descriptor.isSameValue(res, prevRes)) {
                        rowValues[rowNum - startRow][colNum] = res;
                        prevRes = res;
                    }
                }
            }
        }

        var env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        for (var rowNum = 0; rowNum < rowValues.length; rowNum++) {
            var height = 1;
            var thisRow = rowValues[rowNum];
            if (thisRow == null) {
                continue;
            }
            var literal = createLiteral();
            addToRowIndex(rowNum, literal);
            for (var j = rowNum + 1; j < rowValues.length; j++) {
                var nextRow = rowValues[j];
                var isSameRow = true;
                for (var k = 0; k < thisRow.length; k++) {
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

            var context = new DatatypeArrayMultiRowElementContext();
            env.pushLocalFrame(new Object[]{context});
            env.pushThis(literal);
            try {
                for (List<ColumnDescriptor> allDescriptor : allDescriptors) {
                    parseRowsAndPopulateLiteral(literal, allDescriptor, openlAdapter, env, rowNum + startRow, height);
                }
                bindDataIndexWithTableRowNum(resultContainer.size(), rowNum + startRow);
                resultContainer.add(literal);
                dataContexts.add(context);
            } finally {
                env.popThis();
                env.popLocalFrame();
            }
        }
    }

    private void parseRowsAndPopulateLiteral(Object literal,
                                             List<ColumnDescriptor> descriptors,
                                             OpenlToolAdaptor openlAdapter,
                                             IRuntimeEnv env,
                                             int rowNum,
                                             int height) throws OpenLCompilationException {

        if (descriptors.isEmpty()) {
            return;
        }
        var context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];

        Object[][] rowValues = null;
        for (var colNum = 0; colNum < descriptors.size(); colNum++) {
            var descriptor = descriptors.get(colNum);
            ILogicalTable valuesTable = LogicalTableHelper
                    .make1ColumnTable(logicalTable.getSubtable(descriptor.getColumnIdx(), rowNum, 1, height));
            if (rowValues == null) {
                rowValues = new Object[valuesTable.getSource().getHeight()][descriptors.size()];
            }
            var width = valuesTable.getSource().getWidth();
            for (var i = 0; i < valuesTable.getSource().getHeight(); i++) {
                ILogicalTable logicalTable = LogicalTableHelper.make1ColumnTable(
                        LogicalTableHelper.logicalTable(valuesTable.getSource().getSubtable(0, i, width, i + 1))
                                .getSubtable(0, 0, width, 1));
                rowValues[i][colNum] = descriptor.parseCellValue(logicalTable, openlAdapter);
            }
        }

        var pkDescriptor = descriptors.getFirst();

        Object[] prevRow = null;
        var shouldSkipMergingSameValues = !pkDescriptor.isPrimaryKey() && !pkDescriptor
                .isDeclaredClassSupportMultirow();

        for (var i = 0; i < rowValues.length; i++) {
            boolean isSameRow;
            var thisRow = rowValues[i];
            context.setRow(i);
            if (prevRow == null || shouldSkipMergingSameValues) {
                isSameRow = false;
            } else {
                if (pkDescriptor.isPrimaryKey()) {
                    isSameRow = pkDescriptor.isSameValue(thisRow[0], prevRow[0]);
                } else {
                    isSameRow = true;
                    for (var k = 0; k < thisRow.length; k++) {
                        isSameRow = descriptors.get(k).isSameValue(thisRow[k], prevRow[k]);
                        if (!isSameRow) {
                            break;
                        }
                    }
                }
            }
            context.setRowValueIsTheSameAsPrevious(isSameRow);
            for (var k = 0; k < thisRow.length; k++) {
                var descriptor = descriptors.get(k);
                var thisValue = thisRow[k];
                if (descriptor.isValuesAnArray()) {
                    var currentValue = descriptor.getFieldValue(literal, env);
                    var thisLen = Array.getLength(thisValue);
                    if (currentValue == null || Array.getLength(currentValue) == 0) {
                        descriptor.setFieldValue(literal, thisLen == 0 ? null : thisValue, env);
                    } else if (thisLen != 0) {
                        var currentLen = Array.getLength(currentValue);
                        Object newArray = Array.newInstance(thisValue.getClass().getComponentType(),
                                currentLen + thisLen);
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
            var dim = 0;
            Class<?> type = dataModel.getInstanceClass();
            while (type.isArray()) {
                type = type.getComponentType();
                dim++;
            }
            return Array.newInstance(type, new int[dim]);
        } else {
            var literal = dataModel.newInstance();
            if (literal == null) {
                throw new OpenLCompilationException(
                        "Cannot create an instance of '%s'.".formatted(dataModel.getName()));
            }
            return literal;
        }
    }

    private void processRow(OpenlToolAdaptor openlAdapter, int startRow, int rowNum) throws OpenLCompilationException {

        var constructor = isConstructor();
        Object literal = null;

        var rowIndex = rowNum - startRow;

        if (!constructor) {
            literal = createLiteral();
            addToRowIndex(rowIndex, literal);
        }

        var env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        env.pushLocalFrame(new Object[]{new DatatypeArrayMultiRowElementContext()});
        var hasError = false;
        Set<String> fieldWithValue = new HashSet<>();
        if (Objects.equals(tableSyntaxNode.getType(), XlsNodeTypes.XLS_TEST_METHOD.toString())) {
            hasError = Arrays.stream(dataModel.getDescriptors()).anyMatch(m -> m.getName().startsWith(TestMethodHelper.EXPECTED_ERROR));
            fieldWithValue = Arrays.stream(dataModel.getDescriptors())
                    .filter(d -> {
                        var lTable = logicalTable.getSubtable(d.getColumnIdx(), rowNum, 1, 1);
                        var nonEmptyResCell = !(lTable.getHeight() == 1 && lTable.getWidth() == 1) || lTable.getCell(0, 0)
                                .getStringValue() != null && d.getField() != null;
                        return nonEmptyResCell && d.getField().getName().startsWith(TestMethodHelper.EXPECTED_RESULT_NAME) &&
                                d.getField() instanceof FieldChain && ((FieldChain) d.getField()).getFields().length > 1;
                    })
                    .map(d -> {
                        var fieldChain = (FieldChain) d.getField();
                        return (FieldChain.makeNames(Arrays.copyOfRange(fieldChain.getFields(), 0, fieldChain.getFields().length - 1)));
                    })
                    .collect(Collectors.toSet());
        }
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            var hasValue = false;
            if (columnDescriptor.getField() instanceof FieldChain) {
                var fieldChain = (FieldChain) columnDescriptor.getField();
                if (fieldChain.getFields().length > 1) {
                    String fieldName = FieldChain.makeNames(Arrays.copyOfRange(fieldChain.getFields(), 0, fieldChain.getFields().length - 1));
                    hasValue = fieldWithValue.contains(fieldName);
                }
            }
            literal = processColumn(columnDescriptor, openlAdapter, constructor, rowNum, literal, env, hasError, hasValue);
        }
        env.popLocalFrame();
        if (literal == null) {
            literal = dataModel.getType().nullObject();
        }

        var idx = rowNum - startRow;
        bindDataIndexWithTableRowNum(idx, rowNum);
        Array.set(dataArray, idx, literal);
    }

    private void bindDataIndexWithTableRowNum(Integer idx, Integer rowNum) {
        if (dataIdxToTableRowNum == null) {
            dataIdxToTableRowNum = new HashMap<>();
        }
        dataIdxToTableRowNum.put(idx, rowNum);
    }

    private Object processColumn(ColumnDescriptor columnDescriptor,
                                 OpenlToolAdaptor openlAdapter,
                                 boolean constructor,
                                 int rowNum,
                                 Object literal,
                                 IRuntimeEnv env, boolean hasError, boolean hasValue) throws SyntaxNodeException {

        if (columnDescriptor != null && !columnDescriptor.isReference()) {
            if (constructor) {
                literal = columnDescriptor.getLiteral(dataModel.getType(),
                        logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1),
                        openlAdapter);
            } else {
                try {
                    var lTable = logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1);
                    if (!(lTable.getHeight() == 1 && lTable.getWidth() == 1) || lTable.getCell(0, 0)
                            .getStringValue() != null) { // EPBDS-6104. For empty values should be used data type default value.
                        return columnDescriptor.populateLiteral(literal, lTable, openlAdapter, env, false);
                    } else if (columnDescriptor.getField() != null && columnDescriptor.getField().getName().startsWith(TestMethodHelper.EXPECTED_RESULT_NAME) &&
                            !columnDescriptor.isValuesAnArray() && !hasError && hasValue) {
                        return columnDescriptor.populateLiteral(literal, lTable, openlAdapter, env, true);
                    }
                } catch (SyntaxNodeException ex) {
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
        var oldRow = primaryIndexMap.getKey(value);
        if (oldRow != null && row != oldRow) {
            throw new OpenLRuntimeException("Duplicated key: %s in rows %s and %s.".formatted(value, oldRow, row));
        }
        primaryIndexMap.put(row, value);
    }

    @Override
    public Object findObject(int columnIndex, String skey, IBindingContext cxt) {
        var descriptor = dataModel.getDescriptor(columnIndex);

        Map<String, Integer> index = descriptor.getUniqueIndex(this, columnIndex, cxt);

        var found = index.get(skey);

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

    private DatatypeArrayMultiRowElementContext getCachedContext(int i) {
        if (dataContextCache == null || dataContextCache.isEmpty()) {
            return null;
        }
        return dataContextCache.get(i);
    }
}
