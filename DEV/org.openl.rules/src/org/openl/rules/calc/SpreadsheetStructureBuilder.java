package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

import lombok.Getter;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;
import org.apache.commons.collections4.map.LinkedMap;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.IMetaHolder;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.element.SpreadsheetExpressionMarker;
import org.openl.rules.calc.element.SpreadsheetStructureBuilderHolder;
import org.openl.rules.calc.result.ArrayResultBuilder;
import org.openl.rules.calc.result.EmptyResultBuilder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.calc.result.ScalarResultBuilder;
import org.openl.rules.calc.result.SpreadsheetResultBuilder;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.JavaKeywordUtils;
import org.openl.util.MessageUtils;
import org.openl.util.OpenClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.AbsolutePosition;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

public class SpreadsheetStructureBuilder {

    private static final SpreadsheetHeaderDefinition[] EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY = new SpreadsheetHeaderDefinition[0];

    public static final String DOLLAR_SIGN = "$";

    private IBindingContext spreadsheetBindingContext;

    private final IOpenMethodHeader spreadsheetHeader;

    private final XlsModuleOpenClass xlsModuleOpenClass;

    @Getter
    private final SpreadsheetStructureBuilderHolder spreadsheetStructureBuilderHolder = new SpreadsheetStructureBuilderHolder(
            this);

    public static final ThreadLocal<Stack<Set<SpreadsheetCell>>> preventCellsLoopingOnThis = new ThreadLocal<>();

    /**
     * tableSyntaxNode of the spreadsheet
     **/
    private final TableSyntaxNode tableSyntaxNode;

    @Getter
    private final ILogicalTable tableBody;

    public SpreadsheetStructureBuilder(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext,
                                       IOpenMethodHeader spreadsheetHeader,
                                       XlsModuleOpenClass xlsModuleOpenClass) {
        this.tableSyntaxNode = tableSyntaxNode;
        this.tableBody = tableSyntaxNode.getTableBody();
        this.bindingContext = bindingContext;
        this.spreadsheetHeader = spreadsheetHeader;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.rowHeaders = new LinkedMap<>();
        this.columnHeaders = new LinkedMap<>();
        this.rowDescriptions = new DualHashBidiMap<>();
        this.columnDescriptions = new DualHashBidiMap<>();
        addHeaders();
    }

    /**
     * binding context for indicating execution mode
     **/
    private final IBindingContext bindingContext;
    private final Map<Integer, IBindingContext> rowContexts = new HashMap<>();
    private final Map<Integer, SpreadsheetOpenClass> colComponentOpenClasses = new HashMap<>();
    private final Map<Integer, Map<Integer, SpreadsheetContext>> spreadsheetResultContexts = new HashMap<>();

    private final Map<Integer, SpreadsheetHeaderDefinition> rowHeaders;
    private final Map<Integer, SpreadsheetHeaderDefinition> columnHeaders;

    private final BidiMap<Integer, Integer> rowDescriptions;
    private final BidiMap<Integer, Integer> columnDescriptions;

    // Mappings from physical to logical indices (built after headers are parsed)
    private Map<Integer, Integer> physicalToLogicalRow;
    private Map<Integer, Integer> physicalToLogicalColumn;
    private Integer[] logicalToPhysicalRow;
    private Integer[] logicalToPhysicalColumn;

    private SpreadsheetHeaderDefinition returnHeaderDefinition;
    private SpreadsheetCell[][] cells;

    private final List<SpreadsheetCell> extractedCellValues = new ArrayList<>();

    private final List<SyntaxNodeException> onDemandErrors = new ArrayList<>();
    private final Collection<OpenLMessage> onDemandMessages = new LinkedHashSet<>();

    private volatile boolean cellsExtracted;

    /**
     * Extract cell values from the source spreadsheet table.
     *
     * @return cells of spreadsheet with its values
     */
    public SpreadsheetCell[][] getCells() {
        if (!cellsExtracted) {
            synchronized (this) {
                if (!cellsExtracted) {
                    try {
                        extractCellValues();
                        extractCellDescriptions();
                    } finally {
                        cellsExtracted = true;
                        reportOnDemandDiagnostics();
                    }
                }
            }
        }
        return cells;
    }

    /**
     * Add to {@link SpreadsheetOpenClass} fields that are represented by spreadsheet cells.
     *
     * @param spreadsheetType open class of the spreadsheet
     */
    public void addCellFields(SpreadsheetOpenClass spreadsheetType, boolean autoType) {

        // create cells according to the logical size (excluding description rows/columns)
        var logicalHeight = this.rowHeaders.size();
        var logicalWidth = this.columnHeaders.size();
        cells = new SpreadsheetCell[logicalHeight][logicalWidth];

        // create the binding context for the spreadsheet level
        spreadsheetBindingContext = new SpreadsheetContext(bindingContext, spreadsheetType, xlsModuleOpenClass);

        // Build mappings between physical and logical indices
        logicalToPhysicalRow = this.rowHeaders.keySet().toArray(new Integer[0]);
        logicalToPhysicalColumn = this.columnHeaders.keySet().toArray(new Integer[0]);
        physicalToLogicalRow = new HashMap<>();
        physicalToLogicalColumn = new HashMap<>();
        for (var logical = 0; logical < logicalHeight; logical++) {
            physicalToLogicalRow.put(logicalToPhysicalRow[logical], logical);
        }
        for (var logical = 0; logical < logicalWidth; logical++) {
            physicalToLogicalColumn.put(logicalToPhysicalColumn[logical], logical);
        }

        for (var logicalRow = 0; logicalRow < logicalHeight; logicalRow++) {
            var physicalRow = logicalToPhysicalRow[logicalRow];
            for (var logicalCol = 0; logicalCol < logicalWidth; logicalCol++) {
                var physicalCol = logicalToPhysicalColumn[logicalCol];
                // build spreadsheet cell using physical indices to read from table,
                // but store logical indices in the cell
                var spreadsheetCell = buildCell(physicalRow, physicalCol, logicalRow, logicalCol, autoType);

                // init cells array with logical indices
                cells[logicalRow][logicalCol] = spreadsheetCell;

                // create and add field of the cell to the spreadsheetType
                addSpreadsheetFields(spreadsheetType, physicalRow, physicalCol);
            }
        }
    }

    private void extractCellValues() {
        // Guard against empty cells array (no valid headers)
        if (cells.length == 0) {
            return;
        }
        // Iterate over logical indices since cells[][] uses logical indexing
        for (var logicalRow = 0; logicalRow < cells.length; logicalRow++) {
            var physicalRow = logicalToPhysicalRow[logicalRow];
            var rowBindingContext = getRowContext(physicalRow);

            for (var logicalCol = 0; logicalCol < cells[0].length; logicalCol++) {
                var physicalCol = logicalToPhysicalColumn[logicalCol];
                var found = false;
                for (SpreadsheetCell cell : extractedCellValues) {
                    // SpreadsheetCell now stores logical indices
                    if (cell.getRowIndex() == logicalRow && cell.getColumnIndex() == logicalCol) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    extractCellValue(rowBindingContext, logicalRow, logicalCol, physicalRow, physicalCol);
                }
            }
        }
    }

    private void extractCellDescriptions() {
        // Guard against empty cells array (no valid headers)
        if (cells.length == 0) {
            return;
        }
        // cells[][] now uses logical indexing, so this method works correctly
        for (var logicalRow = 0; logicalRow < cells.length; logicalRow++) {
            for (var logicalCol = 0; logicalCol < cells[0].length; logicalCol++) {
                // cells[i][j] can be null if the cell is empty, meta info is not working in this case
                if (cells[logicalRow][logicalCol] == null) {
                    var physicalRow = logicalToPhysicalRow[logicalRow];
                    var physicalCol = logicalToPhysicalColumn[logicalCol];
                    var sourceCell = tableBody.getCell(physicalCol + 1, physicalRow + 1);
                    cells[logicalRow][logicalCol] = new SpreadsheetCell(logicalRow, logicalCol, sourceCell, SpreadsheetCellType.DESCRIPTION);
                }
            }
        }
    }

    public IOpenClass makeType(SpreadsheetCell cell) {
        if (cell.getType() == null) {
            Stack<Set<SpreadsheetCell>> stack = preventCellsLoopingOnThis.get();
            var f = stack == null;
            try {
                if (f) {
                    preventCellsLoopingOnThis.set(stack = new Stack<>());
                }
                Set<SpreadsheetCell> cellInProgressSet;
                if (stack.isEmpty()) {
                    cellInProgressSet = new HashSet<>();
                    stack.push(cellInProgressSet);
                } else {
                    cellInProgressSet = stack.peek();
                }
                if (!cellInProgressSet.contains(cell)) {
                    try {
                        cellInProgressSet.add(cell);
                        // SpreadsheetCell stores logical indices
                        var logicalRow = cell.getRowIndex();
                        var logicalCol = cell.getColumnIndex();
                        // Convert to physical indices for table access
                        var physicalRow = logicalToPhysicalRow[logicalRow];
                        var physicalCol = logicalToPhysicalColumn[logicalCol];
                        var rowContext = getRowContext(physicalRow);
                        extractCellValueOnDemand(rowContext, logicalRow, logicalCol, physicalRow, physicalCol);
                        extractedCellValues.add(cell);
                    } finally {
                        cellInProgressSet.remove(cell);
                    }
                } else {
                    return JavaOpenClass.OBJECT;
                }
            } finally {
                if (f) {
                    preventCellsLoopingOnThis.remove();
                }
            }
        }
        return cell.getType();
    }

    /**
     * Compiles a single cell requested by an expression of another table.
     *
     * <p>Such a request arrives while the other table is still choosing how to bind its own expression. Everything
     * reported during that attempt is dropped as soon as another way of binding succeeds. Errors and warnings of the
     * cell are therefore kept aside and reported when this spreadsheet compiles its own cells.
     */
    private void extractCellValueOnDemand(IBindingContext rowBindingContext,
                                          int logicalRow,
                                          int logicalCol,
                                          int physicalRow,
                                          int physicalCol) {
        if (cellsExtracted) {
            // this spreadsheet has already reported its own cells, so there is no later moment to report at
            extractCellValue(rowBindingContext, logicalRow, logicalCol, physicalRow, physicalCol);
            return;
        }
        spreadsheetBindingContext.pushErrors();
        spreadsheetBindingContext.pushMessages();
        try {
            extractCellValue(rowBindingContext, logicalRow, logicalCol, physicalRow, physicalCol);
        } finally {
            onDemandErrors.addAll(spreadsheetBindingContext.popErrors());
            onDemandMessages.addAll(spreadsheetBindingContext.popMessages());
        }
    }

    /**
     * Reports errors and warnings kept aside by {@link #extractCellValueOnDemand} and forgets them.
     */
    private void reportOnDemandDiagnostics() {
        if (onDemandErrors.isEmpty() && onDemandMessages.isEmpty()) {
            return;
        }
        onDemandErrors.forEach(spreadsheetBindingContext::addError);
        onDemandErrors.clear();
        spreadsheetBindingContext.addMessages(onDemandMessages);
        onDemandMessages.clear();
    }

    private void extractCellValue(IBindingContext rowBindingContext, int logicalRow, int logicalCol, int physicalRow, int physicalCol) {
        var spreadsheetCell = cells[logicalRow][logicalCol];

        if (!columnHeaders.containsKey(physicalCol) || !rowHeaders.containsKey(physicalRow)) {
            spreadsheetCell.setValue(null);
            return;
        }

        var cell = tableBody.getCell(physicalCol + 1, physicalRow + 1);
        var source = new CellSourceCodeModule(cell, tableBody);
        var code = source.getCode();

        String name = getSpreadsheetCellFieldName(columnHeaders.get(physicalCol).getDefinitionName(),
                rowHeaders.get(physicalRow).getDefinitionName());

        var type = spreadsheetCell.getType();

        if (StringUtils.isBlank(code)) {
            spreadsheetCell.setValue(type.nullObject());
        } else if (SpreadsheetExpressionMarker.isFormula(code)) {

            var end = 0;
            if (code.startsWith(SpreadsheetExpressionMarker.OPEN_CURLY_BRACKET.getSymbol())) {
                end = -1;
            }

            var srcCode = new SubTextSourceCodeModule(source, 1, end);
            var signature = spreadsheetHeader.getSignature();
            var declaringClass = spreadsheetHeader.getDeclaringClass();
            var header = new OpenMethodHeader(name, type, signature, declaringClass);
            var columnBindingContext = getColumnContext(physicalCol, physicalRow, rowBindingContext);
            var openl = columnBindingContext.getOpenL();
            // columnBindingContext - is never null
            try {
                CompositeMethod method;
                if (header.getType() == null) {
                    method = OpenLManager.makeMethodWithUnknownType(openl,
                            srcCode,
                            name,
                            signature,
                            declaringClass,
                            columnBindingContext);
                    spreadsheetCell.setType(method.getType() != null ? method.getType() : NullOpenClass.the);
                } else {
                    method = OpenLManager.makeMethod(openl, srcCode, header, columnBindingContext);
                }
                spreadsheetCell.setValue(method);
                // Validate literal expressions against domain type
                if (type instanceof DomainOpenClass) {
                    var bodyNode = method.getMethodBodyBoundNode();
                    if (bodyNode != null && bodyNode.getChildren() != null) {
                        for (var child : bodyNode.getChildren()) {
                            BindHelper.validateDomainValue(child, type, columnBindingContext);
                        }
                    }
                }
            } catch (Exception | LinkageError e) {
                spreadsheetCell.setType(NullOpenClass.the);
                var message = "Cannot parse cell value '%s' to the necessary type.".formatted(code);
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils
                        .createError(message, e, LocationUtils.createTextInterval(code), source));
            }

        } else if (spreadsheetCell.isConstantCell()) {
            try {
                var openField = rowBindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, code, true);
                var constOpenField = (ConstantOpenField) openField;
                spreadsheetCell.setValue(constOpenField.getValue());
            } catch (Exception e) {
                var message = "Cannot parse cell value.";
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, e, null, source));
            }
        } else {
            Class<?> instanceClass = type.getInstanceClass();
            if (instanceClass == null) {
                String message = MessageUtils.getTypeDefinedErrorMessage(type.getName());
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, source));
            }

            try {
                var bindingContext = getColumnContext(physicalCol, physicalRow, rowBindingContext);
                Object result = null;
                if (String.class == instanceClass) {
                    result = String2DataConvertorFactory.parse(instanceClass, code, bindingContext);
                } else {
                    if (cell.hasNativeType()) {
                        result = RuleRowHelper.loadNativeValue(cell, type);
                    }
                    if (result == null) {
                        result = String2DataConvertorFactory.parse(instanceClass, code, bindingContext);
                    }
                }

                if (bindingContext.isExecutionMode() && result instanceof IMetaHolder holder) {
                    var meta = new ValueMetaInfo(name, null, source);
                    holder.setMetaInfo(meta);
                }

                var openCast = bindingContext.getCast(JavaOpenClass.getOpenClass(instanceClass), type);
                spreadsheetCell.setValue(openCast.convert(result));
            } catch (Exception t) {
                var message = "Cannot parse cell value '%s' to the necessary type.".formatted(code);
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, t, null, source));
            }
        }
    }

    /**
     * Creates a field from the spreadsheet cell and add it to the spreadsheetType
     *
     * @param physicalRow physical row index (in the source table)
     * @param physicalCol physical column index (in the source table)
     */
    private void addSpreadsheetFields(SpreadsheetOpenClass spreadsheetType, int physicalRow, int physicalCol) {
        var columnHeader = this.columnHeaders.get(physicalCol);
        var rowHeader = this.rowHeaders.get(physicalRow);

        if (columnHeader == null || rowHeader == null) {
            return;
        }

        var oneColumnSpreadsheet = columnHeaders.values().stream().filter(Objects::nonNull).limit(2).count() == 1;
        var oneRowSpreadsheet = rowHeaders.values().stream().filter(Objects::nonNull).limit(2).count() == 1;

        var columnDefinition = columnHeader.getDefinition();
        var rowDefinition = rowHeader.getDefinition();

        // get column name from the column definition
        var columnName = columnDefinition.getName().getIdentifier();

        // get row name from the row definition
        var rowName = rowDefinition.getName().getIdentifier();

        // Convert physical indices to logical indices for accessing cells array
        var logicalRow = physicalToLogicalRow.get(physicalRow);
        var logicalCol = physicalToLogicalColumn.get(physicalCol);
        var spreadsheetCell = cells[logicalRow][logicalCol];
        // create spreadsheet cell field
        createSpreadsheetCellField(spreadsheetType, spreadsheetCell, columnName, rowName);

        if (oneColumnSpreadsheet) {
            // add simplified field name
            createSpreadsheetCellField(spreadsheetType, spreadsheetCell, null, rowName);
        } else if (oneRowSpreadsheet) {
            // add simplified field name
            createSpreadsheetCellField(spreadsheetType, spreadsheetCell, columnName, null);
        }
    }

    /**
     * Gets the name of the spreadsheet cell field. <br>
     * Is represented as {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN} rowName, e.g. $Value$Final
     *
     * @param columnName name of cell column
     * @param rowName    name of the row column
     * @return {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN}rowName, e.g. $Value$Final
     */
    public static String getSpreadsheetCellFieldName(String columnName, String rowName) {
        return (DOLLAR_SIGN + columnName + DOLLAR_SIGN + rowName).intern();
    }

    private SpreadsheetCell buildCell(int physicalRow, int physicalCol, int logicalRow, int logicalCol, boolean autoType) {
        var sourceCell = tableBody.getCell(physicalCol + 1, physicalRow + 1);

        var cellCode = sourceCell.getStringValue();

        IOpenField openField = null;

        var columnHeader = columnHeaders.get(physicalCol);
        var rowHeader = rowHeaders.get(physicalRow);
        SpreadsheetCellType spreadsheetCellType;
        if (cellCode == null || cellCode.isEmpty() || columnHeader == null || rowHeader == null) {
            spreadsheetCellType = SpreadsheetCellType.EMPTY;
        } else if (SpreadsheetExpressionMarker.isFormula(cellCode)) {
            spreadsheetCellType = SpreadsheetCellType.METHOD;
        } else {
            spreadsheetCellType = SpreadsheetCellType.VALUE;
            openField = RuleRowHelper.findConstantField(spreadsheetBindingContext, cellCode);
            if (openField != null) {
                spreadsheetCellType = SpreadsheetCellType.CONSTANT;
            }
        }

        SpreadsheetCell spreadsheetCell;
        ICell sourceCellForExecutionMode = spreadsheetBindingContext.isExecutionMode() ? null : sourceCell;
        // Store logical indices in SpreadsheetCell for consistent indexing with results array
        spreadsheetCell = new SpreadsheetCell(logicalRow, logicalCol, sourceCellForExecutionMode, spreadsheetCellType);

        IOpenClass cellType;
        if (openField != null) {
            cellType = openField.getType();
        } else if (columnHeader != null && columnHeader.getType() != null) {
            cellType = columnHeader.getType();
        } else if (rowHeader != null && rowHeader.getType() != null) {
            cellType = rowHeader.getType();
        } else {

            // Try to derive cell type as double.
            //
            try {
                // Try to parse cell value.
                // If parse process will be finished with success then return
                // double type else string type.
                //
                if (autoType) {
                    if (SpreadsheetExpressionMarker.isFormula(cellCode)) {
                        cellType = null;
                    } else if (cellCode != null) {
                        var objectValue = sourceCell.getObjectValue();
                        if (objectValue instanceof String) {
                            String2DataConvertorFactory.getConvertor(Double.class).parse(cellCode, null);
                            cellType = JavaOpenClass.getOpenClass(Double.class);
                        } else {
                            cellType = JavaOpenClass.getOpenClass(objectValue.getClass());
                        }
                    } else {
                        cellType = NullOpenClass.the;
                    }
                } else {
                    if (!SpreadsheetExpressionMarker.isFormula(cellCode)) {
                        String2DataConvertorFactory.getConvertor(Double.class).parse(cellCode, null);
                    }
                    cellType = JavaOpenClass.getOpenClass(Double.class);
                }
            } catch (Exception t) {
                cellType = JavaOpenClass.getOpenClass(String.class);
            }
        }
        spreadsheetCell.setType(cellType);

        return spreadsheetCell;
    }

    private IBindingContext getRowContext(int rowIndex) {
        var rowContext = rowContexts.get(rowIndex);

        if (rowContext == null) {
            rowContext = makeRowContext(rowIndex);
            rowContexts.put(rowIndex, rowContext);
        }

        return rowContext;
    }

    private SpreadsheetContext getColumnContext(int columnIndex, int rowIndex, IBindingContext
            rowBindingContext) {
        Map<Integer, SpreadsheetContext> contexts = spreadsheetResultContexts.computeIfAbsent(columnIndex,
                e -> new HashMap<>());
        return contexts.computeIfAbsent(rowIndex, e -> makeSpreadsheetResultContext(columnIndex, rowBindingContext));
    }

    private SpreadsheetContext makeSpreadsheetResultContext(int columnIndex, IBindingContext rowBindingContext) {
        var columnOpenClass = colComponentOpenClasses.computeIfAbsent(columnIndex,
                e -> makeColumnComponentOpenClass(columnIndex));
        return new SpreadsheetContext(rowBindingContext, columnOpenClass, xlsModuleOpenClass);
    }

    private SpreadsheetOpenClass makeColumnComponentOpenClass(int physicalColumnIndex) {
        // create name for the column open class
        var columnOpenClassName = "%sColType%d".formatted(spreadsheetHeader.getName(), physicalColumnIndex);

        var columnOpenClass = new SpreadsheetOpenClass(columnOpenClassName, bindingContext.getOpenL());

        // Iterate over logical row indices
        for (var logicalRow = 0; logicalRow < cells.length; logicalRow++) {
            // Convert logical to physical to get header from map
            var physicalRow = logicalToPhysicalRow[logicalRow];
            var headerDefinition = rowHeaders.get(physicalRow);
            // Convert physical column to logical for cells access
            var logicalCol = physicalToLogicalColumn.get(physicalColumnIndex);
            proc(logicalRow, columnOpenClass, logicalCol, headerDefinition);
        }
        return columnOpenClass;
    }

    private IBindingContext makeRowContext(int physicalRowIndex) {

        /* create name for the row open class */
        var rowOpenClassName = "%sRowType%d".formatted(spreadsheetHeader.getName(), physicalRowIndex);

        // create row open class for current row
        var rowOpenClass = new SpreadsheetOpenClass(rowOpenClassName, bindingContext.getOpenL());

        // get the width of the whole spreadsheet (logical width)
        // Guard against empty cells array (no valid headers)
        int width = cells.length > 0 ? cells[0].length : 0;

        // Convert physical row to logical for cells access
        var logicalRow = physicalToLogicalRow.get(physicalRowIndex);

        // create for each column in row its field
        for (var logicalCol = 0; logicalCol < width; logicalCol++) {
            // Convert logical to physical to get header from map
            var physicalCol = logicalToPhysicalColumn[logicalCol];
            var columnHeader = columnHeaders.get(physicalCol);

            proc(logicalRow, rowOpenClass, logicalCol, columnHeader);
        }

        /* create row binding context */
        return new SpreadsheetContext(spreadsheetBindingContext, rowOpenClass, xlsModuleOpenClass);
    }

    private void proc(int rowIndex,
                      ComponentOpenClass rowOpenClass,
                      int columnIndex,
                      SpreadsheetHeaderDefinition columnHeader) {
        if (columnHeader == null) {
            return;
        }

        var cell = cells[rowIndex][columnIndex];

        var fieldName = columnHeader.getDefinition().getName().getIdentifier();
        createSpreadsheetCellField(rowOpenClass, cell, fieldName, null);
    }

    private void createSpreadsheetCellField(ComponentOpenClass rowOpenClass,
                                            SpreadsheetCell cell,
                                            String columnName, String rowName) {
        var structureBuilderContainer = getSpreadsheetStructureBuilderHolder();
        SpreadsheetCellField field;
        if (cell.getSpreadsheetCellType() == SpreadsheetCellType.METHOD) {
            field = new SpreadsheetCellField(structureBuilderContainer,
                    rowOpenClass,
                    columnName,
                    rowName,
                    cell
            );
        } else {
            field = new SpreadsheetCellField.ConstSpreadsheetCellField(structureBuilderContainer,
                    rowOpenClass,
                    columnName,
                    rowName,
                    cell);
        }
        rowOpenClass.addField(field);
    }

    public String[] getRowNamesForResultModel() {
        return getNamesForResultModel(rowHeaders);
    }

    public String[] getColumnNamesForResultModel() {
        return getNamesForResultModel(columnHeaders);
    }

    private String[] getNamesForResultModel(Map<Integer, SpreadsheetHeaderDefinition> headers) {
        final var rowsWithAsteriskCount = headers.values().stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getDefinition().isAsteriskPresented())
                .count();
        String[] ret;
        if (rowsWithAsteriskCount > 0) {
            ret = buildArrayForHeaders(headers.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> e.getDefinition().isAsteriskPresented());
        } else {
            ret = buildArrayForHeaders(headers.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> !e.getDefinition().isTildePresented());
        }
        for (var i = 0; i < ret.length; i++) {
            ret[i] = JavaKeywordUtils.toJavaIdentifier(ret[i]);
        }
        return ret;
    }

    public String[] getRowNames() {
        return buildArrayForHeaders(rowHeaders.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> true);
    }

    public String[] getColumnNames() {
        return buildArrayForHeaders(columnHeaders.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> true);
    }

    private String[] buildArrayForHeaders(SpreadsheetHeaderDefinition[] headers,
                                          Predicate<SpreadsheetHeaderDefinition> predicate) {

        var size = headers.length;
        String[] ret = new String[size];
        for (var i = 0; i < size; i++) {
            if (headers[i] != null && predicate.test(headers[i])) {
                ret[i] = headers[i].getDefinitionName();
            }
        }
        return ret;
    }

    private void addHeaders() {
        var height = tableBody.getHeight() - 1;
        var width = tableBody.getWidth() - 1;
        var registered = new HashSet<String>();
        var descriptionRows = new ArrayList<Integer>();
        for (var row = 0; row < height; row++) {
            var cell = tableBody.getCell(0, row + 1);
            var value = cell.getStringValue();
            if (StringUtils.isNotBlank(value)) {
                if (value.trim().startsWith("//")) {
                    descriptionRows.add(row);
                } else {
                    parseHeader(cell, row, true, registered);
                }
            }
        }
        // First we parse headers and then descriptions, because descriptions need to be validated by headers
        var rowNames = new HashMap<String, Integer>();
        rowHeaders.values().forEach(e -> rowNames.put(e.getDefinitionName(), e.getRow()));
        var rowNamesForDescription = new HashSet<String>();
        descriptionRows.forEach(e -> parseDescription(e, true, rowNames, rowNamesForDescription));

        var descriptionColumns = new ArrayList<Integer>();
        for (var col = 0; col < width; col++) {
            var cell = tableBody.getCell(col + 1, 0);
            var value = cell.getStringValue();
            if (StringUtils.isNotBlank(value)) {
                if (!value.trim().startsWith("//")) {
                    parseHeader(cell, col, false, registered);
                } else {
                    descriptionColumns.add(col);
                }
            }
        }
        // First we parse headers and then descriptions, because descriptions need to be validated by headers
        var columnNames = new HashMap<String, Integer>();
        columnHeaders.values().forEach(e -> columnNames.put(e.getDefinitionName(), e.getColumn()));
        var columnNamesForDescription = new HashSet<String>();
        descriptionColumns.forEach(e -> parseDescription(e, false, columnNames, columnNamesForDescription));

        var spreadsheetHeaderType = spreadsheetHeader.getType();

        if (bindingContext.findType(SpreadsheetResult.class.getSimpleName())
                .equals(spreadsheetHeaderType) && returnHeaderDefinition == null) {
            // No RETURN and SpreadsheetResult is in the return
            return;
        }

        if (returnHeaderDefinition == null) {
            // No RETURN, get the last row
            // Get value with max key
            // The last row is the row with the max key in the
            rowHeaders.keySet().stream().max(Integer::compareTo).ifPresent(e -> returnHeaderDefinition = rowHeaders.get(e));
        }

        if (Boolean.FALSE
                .equals(tableSyntaxNode.getTableProperties().getAutoType()) && returnHeaderDefinition.getType() == null) {
            //  Spreadsheet auto type is disabled and no type is defined in the cell name like  RowName:Double
            returnHeaderDefinition.setType(spreadsheetHeaderType);
        } else if (spreadsheetHeaderType
                .getAggregateInfo() == null || spreadsheetHeaderType.getAggregateInfo() != null && spreadsheetHeaderType
                .getAggregateInfo()
                .getComponentType(spreadsheetHeaderType) == null) {
            // No Java array in the return method signature
            if (hasOnlyOneEmptyCell(returnHeaderDefinition)) {
                returnHeaderDefinition.setType(spreadsheetHeaderType);
            }
        }
    }

    private void parseDescription(int index, boolean row, Map<String, Integer> names, Set<String> used) {
        ICell cell;
        Map<Integer, Integer> descriptions;
        if (row) {
            cell = tableBody.getCell(0, index + 1);
            descriptions = rowDescriptions;
        } else {
            cell = tableBody.getCell(index + 1, 0);
            descriptions = columnDescriptions;
        }
        var value = cell.getStringValue();
        if (value != null && value.trim().startsWith("//")) {
            value = value.trim().substring(2).trim();
            var mappedRowIndex = names.get(value);
            if (mappedRowIndex != null) {
                if (used.contains(value)) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage("The description column '%s' is already defined.".formatted(value), tableSyntaxNode));
                } else {
                    used.add(value);
                    descriptions.put(index, mappedRowIndex);
                }
            } else {
                bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage("The description column '%s' does not correspond to any existing %s in the table.".formatted(cell.getStringValue(), row ? "row" : "column"), tableSyntaxNode));
            }
        }
    }

    private void parseHeader(ICell cell, int index, boolean row, Set<String> registered) {
        IdentifierNode[] nodes;
        var source = new CellSourceCodeModule(cell, tableBody);

        try {
            nodes = Tokenizer.tokenize(source, SpreadsheetSymbols.TYPE_DELIMITER.toString());
        } catch (OpenLCompilationException e) {
            bindingContext.addError(SyntaxNodeExceptionUtils.createError("Cannot parse header.", source));
            return;
        }
        if (nodes.length == 0) {
            bindingContext.addError(SyntaxNodeExceptionUtils.createError("Cannot parse header.", source));
            return;
        }
        if (nodes.length > 2) {
            bindingContext.addError(SyntaxNodeExceptionUtils.createError("Valid header format: name [: type].", nodes[2]));
            return;
        }

        var headerNameNode = nodes[0];
        var typeIdentifierNode = nodes.length == 1 ? null : nodes[1];
        var headerName = headerNameNode.getIdentifier();

        var endsWithAsterisk = headerName.endsWith(SpreadsheetSymbols.ASTERISK.toString());
        var endsWithTilde = headerName.endsWith(SpreadsheetSymbols.TILDE.toString());
        if (endsWithAsterisk || endsWithTilde) {
            headerName = StringUtils.trim(headerName.substring(0, headerName.length() - 1));
            var end = new AbsolutePosition(headerName.length());
            var location = new TextInterval(headerNameNode.getLocation().getStart(), end);
            headerNameNode = new IdentifierNode(headerNameNode.getType(), location, headerName, headerNameNode.getModule());
        }

        var parsed = new SymbolicTypeDefinition(headerNameNode, typeIdentifierNode, endsWithAsterisk, endsWithTilde, source);

        if (!registered.add(headerName)) {
            // Register error if the Step name was already registered for the Spreadsheet.
            bindingContext.addError(SyntaxNodeExceptionUtils.createError("The header '%s' is already defined.".formatted(headerName),
                    headerNameNode));
            return;
        }

        SpreadsheetHeaderDefinition header;
        if (row) {
            header = new SpreadsheetHeaderDefinition(parsed, index, -1);
            rowHeaders.put(index, header);
        } else {
            header = new SpreadsheetHeaderDefinition(parsed, -1, index);
            columnHeaders.put(index, header);
        }

        if (typeIdentifierNode != null) {
            var typeIdentifier = typeIdentifierNode.getOriginalText();
            var headerType = OpenLManager.makeType(bindingContext.getOpenL(), typeIdentifier, source, bindingContext);
            header.setType(headerType);
        }

        addMetaInfo(header, cell);

        if ("RETURN".equals(headerName)) {
            // If the Spreadsheet Step name is "RETURN" keyword
            returnHeaderDefinition = header;
        }
    }

    private void addMetaInfo(SpreadsheetHeaderDefinition headerDefinition, ICell cell) {
        if (!bindingContext.isExecutionMode() && tableSyntaxNode
                .getMetaInfoReader() instanceof SpreadsheetMetaInfoReader metaInfoReader) {
            var headerType = headerDefinition.getType();
            var symbolicTypeDefinition = headerDefinition.getDefinition();
            var typeIdentifierNode = symbolicTypeDefinition.getType();
            var nodeUsages = new ArrayList<NodeUsage>();
            if (headerDefinition.getDefinition().isAsteriskPresented()) {
                String s = JavaKeywordUtils.toJavaIdentifier(headerDefinition.getDefinitionName());
                if (org.apache.commons.lang3.StringUtils.isEmpty(s)) {
                    s = "Empty string";
                }
                var stringValue = cell.getStringValue();
                var d = stringValue.lastIndexOf(SpreadsheetSymbols.ASTERISK.toString());
                var nodeUsage = new SimpleNodeUsage(0, d, s, null, NodeType.OTHER);
                nodeUsages.add(nodeUsage);
            }
            if (headerType != null) {
                var identifier = cutTypeIdentifier(typeIdentifierNode);
                if (identifier != null) {
                    var type = headerType;
                    while (type.getMetaInfo() == null && type.isArray()) {
                        type = type.getComponentClass();
                    }
                    var typeMeta = type.getMetaInfo();
                    if (typeMeta != null) {
                        var nodeUsage = new SimpleNodeUsage(identifier,
                                typeMeta.getDisplayName(INamedThing.SHORT),
                                typeMeta.getSourceUrl(),
                                type,
                                NodeType.DATATYPE);
                        nodeUsages.add(nodeUsage);
                    }
                }
            }
            if (!nodeUsages.isEmpty()) {
                var cellMetaInfo = new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
                metaInfoReader.addHeaderMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn(), cellMetaInfo);
            }
        }
    }

    /**
     * Cut a type identifier from a type identifier containing array symbols and whitespace.
     *
     * @param typeIdentifierNode identifier with additional info
     * @return cleaned type identifier
     */
    private IdentifierNode cutTypeIdentifier(IdentifierNode typeIdentifierNode) {
        try {
            IdentifierNode[] variableAndType = Tokenizer.tokenize(typeIdentifierNode.getModule(),
                    SpreadsheetSymbols.TYPE_DELIMITER.toString());
            if (variableAndType.length > 1) {
                IdentifierNode[] nodes = Tokenizer
                        .tokenize(typeIdentifierNode.getModule(), " []\n\r", variableAndType[1].getLocation());
                if (nodes.length > 0) {
                    return nodes[0];
                }
            }
        } catch (OpenLCompilationException e) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Cannot parse header.",
                    typeIdentifierNode);
            bindingContext.addError(error);
        }

        return null;
    }

    private boolean hasOnlyOneEmptyCell(SpreadsheetHeaderDefinition headerDefinition) {
        var fromRow = 0;
        var toRow = tableBody.getHeight();

        var fromColumn = 0;
        var toColumn = tableBody.getWidth();

        if (headerDefinition.isRow()) {
            fromRow = headerDefinition.getRow();
            toRow = fromRow + 1;
        } else {
            fromColumn = headerDefinition.getColumn();
            toColumn = fromColumn + 1;
        }

        var nonEmptyCellsCount = 0;

        for (var columnIndex = fromColumn; columnIndex < toColumn; columnIndex++) {
            for (var rowIndex = fromRow; rowIndex < toRow; rowIndex++) {
                if (headerDefinition.isRow() && columnHeaders.containsKey(columnIndex) || !headerDefinition.isRow() && rowHeaders.containsKey(rowIndex)) {
                    var value = tableBody.getCell(columnIndex + 1, rowIndex + 1).getStringValue();
                    var isFormula = SpreadsheetExpressionMarker.isFormula(value);
                    if (StringUtils.isNotBlank(value) && !isFormula) {
                        nonEmptyCellsCount += 1;
                        if (nonEmptyCellsCount > 1) {
                            return false;
                        }
                    }
                }
            }
        }
        return nonEmptyCellsCount == 1;
    }

    public boolean isExistsReturnHeader() {
        return returnHeaderDefinition != null;
    }

    public IResultBuilder buildResultBuilder(Spreadsheet spreadsheet,
                                             IBindingContext bindingContext) throws SyntaxNodeException {

        if (OpenClassUtils.isVoid(spreadsheet.getHeader().getType())) {
            return new EmptyResultBuilder();
        }

        IResultBuilder resultBuilder;

        if (!isExistsReturnHeader() && bindingContext
                .findType(SpreadsheetResult.class.getSimpleName())
                .equals(spreadsheet.getHeader().getType())) {
            resultBuilder = new SpreadsheetResultBuilder();
        } else {
            // real return type
            //
            List<SpreadsheetCell> returnSpreadsheetCells = new ArrayList<>();
            List<IOpenCast> casts = new ArrayList<>();
            var returnSpreadsheetCellsAsArray = new ArrayList<SpreadsheetCell>();
            var castsAsArray = new ArrayList<IOpenCast>();

            var type = spreadsheet.getType();
            var aggregateInfo = type.getAggregateInfo();
            var componentType = aggregateInfo.getComponentType(type);
            var asArray = false;

            var sprCells = new ArrayList<SpreadsheetCell>();
            var physicalIndex = returnHeaderDefinition.getRow();
            if (physicalIndex < 0) {
                // Return header is a column, convert physical to logical
                physicalIndex = returnHeaderDefinition.getColumn();
                var logicalCol = physicalToLogicalColumn.get(physicalIndex);
                for (var i = 0; i < spreadsheet.getCells().length; i++) {
                    sprCells.add(spreadsheet.getCells()[i][logicalCol]);
                }
            } else {
                // Return header is a row, convert physical to logical
                var logicalRow = physicalToLogicalRow.get(physicalIndex);
                sprCells.addAll(Arrays.asList(spreadsheet.getCells()[logicalRow]));
            }

            var nonEmptySpreadsheetCells = new ArrayList<SpreadsheetCell>();
            for (SpreadsheetCell cell : sprCells) {
                if (cell != null && !cell.isEmpty()) {
                    nonEmptySpreadsheetCells.add(cell);
                    if (cell.getType() != null) {
                        var cast = bindingContext.getCast(cell.getType(), type);
                        if (cast != null && cast.isImplicit() && !(cast instanceof IOneElementArrayCast)) {
                            returnSpreadsheetCells.add(cell);
                            casts.add(cast);
                        }

                        if (returnSpreadsheetCells.isEmpty() && componentType != null) {
                            cast = bindingContext.getCast(cell.getType(), componentType);
                            if (cast != null && cast.isImplicit() && !(cast instanceof IOneElementArrayCast)) {
                                returnSpreadsheetCellsAsArray.add(cell);
                                castsAsArray.add(cast);
                            }
                        }
                    }
                }
            }

            if (componentType != null && returnSpreadsheetCells.isEmpty()) {
                returnSpreadsheetCells = returnSpreadsheetCellsAsArray;
                returnHeaderDefinition.setType(componentType);
                casts = castsAsArray;
                asArray = true;
            } else {
                returnHeaderDefinition.setType(type);
            }

            if (!returnSpreadsheetCells.isEmpty()) {
                if (asArray) {
                    for (SpreadsheetCell cell : returnSpreadsheetCells) {
                        cell.setReturnCell(true);
                    }
                } else {
                    var spreadsheetCell = returnSpreadsheetCells.getLast();
                    spreadsheetCell.setReturnCell(true);
                }
            } else if (!nonEmptySpreadsheetCells.isEmpty()) {
                if (asArray) {
                    for (SpreadsheetCell cell : nonEmptySpreadsheetCells) {
                        cell.setReturnCell(true);
                    }
                } else {
                    var spreadsheetCell = nonEmptySpreadsheetCells.getLast();
                    spreadsheetCell.setReturnCell(true);
                }
            }

            if (returnSpreadsheetCells.isEmpty()) {
                var symbolicTypeDefinitionName = Optional.ofNullable(returnHeaderDefinition)
                        .map(SpreadsheetHeaderDefinition::getDefinition)
                        .map(SymbolicTypeDefinition::getName)
                        .orElse(null);
                if (!nonEmptySpreadsheetCells.isEmpty()) {
                    var nonEmptySpreadsheetCell = nonEmptySpreadsheetCells.getLast();
                    if (nonEmptySpreadsheetCell.getType() != null) {
                        throw SyntaxNodeExceptionUtils.createError(
                                "Cannot convert from '%s' to '%s'.".formatted(
                                        nonEmptySpreadsheetCell.getType().getName(),
                                        spreadsheet.getHeader().getType().getName()),
                                Optional.ofNullable(nonEmptySpreadsheetCell.getMethod())
                                        .filter(CompositeMethod.class::isInstance)
                                        .map(CompositeMethod.class::cast)
                                        .map(CompositeMethod::getMethodBodyBoundNode)
                                        .map(IBoundMethodNode::getSyntaxNode)
                                        .orElse(symbolicTypeDefinitionName));
                    } else {
                        return null;
                    }
                } else {
                    throw SyntaxNodeExceptionUtils.createError("There is no return expression cell.",
                            symbolicTypeDefinitionName);
                }
            } else if (asArray) {
                resultBuilder = new ArrayResultBuilder(returnSpreadsheetCells.toArray(new SpreadsheetCell[0]),
                        castsAsArray.toArray(new IOpenCast[]{}),
                        type,
                        isCalculateAllCellsInSpreadsheet(spreadsheet));
            } else {
                resultBuilder = new ScalarResultBuilder(
                        returnSpreadsheetCells.getLast(),
                        casts.getLast(),
                        isCalculateAllCellsInSpreadsheet(spreadsheet));

            }
        }
        return resultBuilder;
    }

    private boolean isCalculateAllCellsInSpreadsheet(Spreadsheet spreadsheet) {
        return !Boolean.FALSE.equals(spreadsheet.getMethodProperties().getCalculateAllCells());
    }

    public BidiMap<Integer, Integer> getRowOffsets() {
        var rowOffsets = new DualHashBidiMap<Integer, Integer>();
        var index = 0;
        for (Integer rowIndex : rowHeaders.keySet()) {
            rowOffsets.put(index, rowIndex);
            index++;
        }
        return rowOffsets;
    }

    public BidiMap<Integer, Integer> getColumnOffsets() {
        var columnOffsets = new DualHashBidiMap<Integer, Integer>();
        var index = 0;
        for (Integer rowIndex : columnHeaders.keySet()) {
            columnOffsets.put(index, rowIndex);
            index++;
        }
        return columnOffsets;
    }

    public BidiMap<Integer, Integer> getRowDescriptions() {
        return UnmodifiableBidiMap.unmodifiableBidiMap(rowDescriptions);
    }

    public BidiMap<Integer, Integer> getColumnDescriptions() {
        return UnmodifiableBidiMap.unmodifiableBidiMap(columnDescriptions);
    }

    /**
     * Keeps a spreadsheet diagnostic linked to the exact workbook cell range, including after table properties and
     * across table parts.
     */
    private static class CellSourceCodeModule extends StringSourceCodeModule {

        private CellSourceCodeModule(ICell cell, ILogicalTable table) {
            super(cell.getStringValue(), getSourceUri(cell, table));
        }

        private static String getSourceUri(ICell cell, ILogicalTable table) {
            var grid = table.getSource().getGrid();
            var region = grid instanceof CompositeGrid ? cell.getRegion() : cell.getAbsoluteRegion();
            if (region == null) {
                region = new GridRegion(cell.getRow(), cell.getColumn(), cell.getRow(), cell.getColumn());
            }
            return grid.getRangeUri(region.getLeft(), region.getTop(), region.getRight(), region.getBottom());
        }
    }

}
