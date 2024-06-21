package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;
import org.apache.commons.collections4.map.LinkedMap;

import org.openl.OpenL;
import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellRefType;
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
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
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

    private final SpreadsheetStructureBuilderHolder spreadsheetStructureBuilderHolder = new SpreadsheetStructureBuilderHolder(
            this);

    public static final ThreadLocal<Stack<Set<SpreadsheetCell>>> preventCellsLoopingOnThis = new ThreadLocal<>();

    /**
     * tableSyntaxNode of the spreadsheet
     **/
    private final TableSyntaxNode tableSyntaxNode;
    private final ILogicalTable tableBody;

    public SpreadsheetStructureBuilderHolder getSpreadsheetStructureBuilderHolder() {
        return spreadsheetStructureBuilderHolder;
    }

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

    private SpreadsheetHeaderDefinition returnHeaderDefinition;
    private SpreadsheetCell[][] cells;

    private final List<SpreadsheetCell> extractedCellValues = new ArrayList<>();

    private volatile boolean cellsExtracted = false;

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

        // create cells according to the size of the spreadsheet
        cells = new SpreadsheetCell[this.tableBody.getHeight() - 1][this.tableBody.getWidth() - 1];

        // create the binding context for the spreadsheet level
        spreadsheetBindingContext = new SpreadsheetContext(bindingContext, spreadsheetType, xlsModuleOpenClass);

        for (Integer rowIndex : this.rowHeaders.keySet()) {
            for (Integer columnIndex : this.columnHeaders.keySet()) {
                // build spreadsheet cell
                SpreadsheetCell spreadsheetCell = buildCell(rowIndex, columnIndex, autoType);

                // init cells array with appropriate cell
                cells[rowIndex][columnIndex] = spreadsheetCell;

                // create and add field of the cell to the spreadsheetType
                addSpreadsheetFields(spreadsheetType, rowIndex, columnIndex);
            }
        }
    }

    private void extractCellValues() {
        for (Integer rowIndex : this.rowHeaders.keySet()) {
            IBindingContext rowBindingContext = getRowContext(rowIndex);

            for (Integer columnIndex : this.columnHeaders.keySet()) {
                boolean found = false;
                for (SpreadsheetCell cell : extractedCellValues) {
                    int row = cell.getRowIndex();
                    int column = cell.getColumnIndex();
                    if (row == rowIndex && columnIndex == column) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    extractCellValue(rowBindingContext, rowIndex, columnIndex);
                }
            }
        }
    }

    private void extractCellDescriptions() {
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[0].length; j++) {
                // cells[i][j] can be null if the cell is empty, meta info is not working in this case
                if (cells[i][j] == null) {
                    ICell sourceCell = tableBody.getCell(j + 1, i + 1);
                    cells[i][j] = new SpreadsheetCell(i, j, sourceCell, SpreadsheetCellType.DESCRIPTION);
                }
            }
        }
    }

    public IOpenClass makeType(SpreadsheetCell cell) {
        if (cell.getType() == null) {
            Stack<Set<SpreadsheetCell>> stack = preventCellsLoopingOnThis.get();
            boolean f = stack == null;
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
                        int rowIndex = cell.getRowIndex();
                        int columnIndex = cell.getColumnIndex();
                        IBindingContext rowContext = getRowContext(rowIndex);
                        extractCellValue(rowContext, rowIndex, columnIndex);
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

    private void extractCellValue(IBindingContext rowBindingContext, int rowIndex, int columnIndex) {
        SpreadsheetCell spreadsheetCell = cells[rowIndex][columnIndex];

        if (!columnHeaders.containsKey(columnIndex) || !rowHeaders.containsKey(rowIndex)) {
            spreadsheetCell.setValue(null);
            return;
        }

        ICell cell = tableBody.getCell(columnIndex + 1, rowIndex + 1);
        var source = new CellSourceCodeModule(cell, tableBody);
        var code = source.getCode();

        String name = getSpreadsheetCellFieldName(columnHeaders.get(columnIndex).getDefinitionName(),
                rowHeaders.get(rowIndex).getDefinitionName());

        IOpenClass type = spreadsheetCell.getType();

        if (StringUtils.isBlank(code)) {
            spreadsheetCell.setValue(type.nullObject());
        } else if (SpreadsheetExpressionMarker.isFormula(code)) {

            int end = 0;
            if (code.startsWith(SpreadsheetExpressionMarker.OPEN_CURLY_BRACKET.getSymbol())) {
                end = -1;
            }

            IOpenSourceCodeModule srcCode = new SubTextSourceCodeModule(source, 1, end);
            IMethodSignature signature = spreadsheetHeader.getSignature();
            IOpenClass declaringClass = spreadsheetHeader.getDeclaringClass();
            IOpenMethodHeader header = new OpenMethodHeader(name, type, signature, declaringClass);
            IBindingContext columnBindingContext = getColumnContext(columnIndex, rowIndex, rowBindingContext);
            OpenL openl = columnBindingContext.getOpenL();
            // columnBindingContext - is never null
            try {
                IOpenMethod method;
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
            } catch (Exception | LinkageError e) {
                spreadsheetCell.setType(NullOpenClass.the);
                String message = String.format("Cannot parse cell value '%s' to the necessary type.", code);
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils
                        .createError(message, e, LocationUtils.createTextInterval(code), source));
            }

        } else if (spreadsheetCell.isConstantCell()) {
            try {
                IOpenField openField = rowBindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, code, true);
                ConstantOpenField constOpenField = (ConstantOpenField) openField;
                spreadsheetCell.setValue(constOpenField.getValue());
            } catch (Exception e) {
                String message = "Cannot parse cell value.";
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, e, null, source));
            }
        } else {
            Class<?> instanceClass = type.getInstanceClass();
            if (instanceClass == null) {
                String message = MessageUtils.getTypeDefinedErrorMessage(type.getName());
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, source));
            }

            try {
                IBindingContext bindingContext = getColumnContext(columnIndex, rowIndex, rowBindingContext);
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

                if (bindingContext.isExecutionMode() && result instanceof IMetaHolder) {
                    IMetaInfo meta = new ValueMetaInfo(name, null, source);
                    ((IMetaHolder) result).setMetaInfo(meta);
                }

                IOpenCast openCast = bindingContext.getCast(JavaOpenClass.getOpenClass(instanceClass), type);
                spreadsheetCell.setValue(openCast.convert(result));
            } catch (Exception t) {
                String message = String.format("Cannot parse cell value '%s' to the necessary type.", code);
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, t, null, source));
            }
        }
    }

    /**
     * Creates a field from the spreadsheet cell and add it to the spreadsheetType
     */
    private void addSpreadsheetFields(SpreadsheetOpenClass spreadsheetType, int rowIndex, int columnIndex) {
        SpreadsheetHeaderDefinition columnHeader = this.columnHeaders.get(columnIndex);
        SpreadsheetHeaderDefinition rowHeader = this.rowHeaders.get(rowIndex);

        if (columnHeader == null || rowHeader == null) {
            return;
        }

        boolean oneColumnSpreadsheet = columnHeaders.values().stream().filter(Objects::nonNull).limit(2).count() == 1;
        boolean oneRowSpreadsheet = rowHeaders.values().stream().filter(Objects::nonNull).limit(2).count() == 1;

        SymbolicTypeDefinition columnDefinition = columnHeader.getDefinition();
        SymbolicTypeDefinition rowDefinition = rowHeader.getDefinition();

        // get column name from the column definition
        String columnName = columnDefinition.getName().getIdentifier();

        // get row name from the row definition
        String rowName = rowDefinition.getName().getIdentifier();

        // create name of the field
        String fieldName = getSpreadsheetCellFieldName(columnName, rowName);

        SpreadsheetCell spreadsheetCell = cells[rowIndex][columnIndex];
        // create spreadsheet cell field
        createSpreadsheetCellField(spreadsheetType, spreadsheetCell, fieldName, SpreadsheetCellRefType.ROW_AND_COLUMN);

        if (oneColumnSpreadsheet) {
            // add simplified field name
            String simplifiedFieldName = getSpreadsheetCellSimplifiedFieldName(rowName);
            IOpenField field1 = spreadsheetType.getField(simplifiedFieldName);
            if (field1 == null) {
                createSpreadsheetCellField(spreadsheetType,
                        spreadsheetCell,
                        simplifiedFieldName,
                        SpreadsheetCellRefType.SINGLE_COLUMN);
            }
        } else if (oneRowSpreadsheet) {
            // add simplified field name
            String simplifiedFieldName = getSpreadsheetCellSimplifiedFieldName(columnName);
            IOpenField field1 = spreadsheetType.getField(simplifiedFieldName);
            if (field1 == null || field1 instanceof SpreadsheetCellField && ((SpreadsheetCellField) field1)
                    .isLastColumnRef()) {
                createSpreadsheetCellField(spreadsheetType,
                        spreadsheetCell,
                        simplifiedFieldName,
                        SpreadsheetCellRefType.SINGLE_ROW);
            }
        }
    }

    private String getSpreadsheetCellSimplifiedFieldName(String rowName) {
        return (DOLLAR_SIGN + rowName).intern();
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

    private SpreadsheetCell buildCell(int rowIndex, int columnIndex, boolean autoType) {
        ICell sourceCell = tableBody.getCell(columnIndex + 1, rowIndex + 1);

        String cellCode = sourceCell.getStringValue();

        IOpenField openField = null;

        SpreadsheetHeaderDefinition columnHeader = columnHeaders.get(columnIndex);
        SpreadsheetHeaderDefinition rowHeader = rowHeaders.get(rowIndex);
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
        spreadsheetCell = new SpreadsheetCell(rowIndex, columnIndex, sourceCellForExecutionMode, spreadsheetCellType);

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
                        Object objectValue = sourceCell.getObjectValue();
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
        IBindingContext rowContext = rowContexts.get(rowIndex);

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
        SpreadsheetOpenClass columnOpenClass = colComponentOpenClasses.computeIfAbsent(columnIndex,
                e -> makeColumnComponentOpenClass(columnIndex));
        return new SpreadsheetContext(rowBindingContext, columnOpenClass, xlsModuleOpenClass);
    }

    private SpreadsheetOpenClass makeColumnComponentOpenClass(int columnIndex) {
        // create name for the column open class
        String columnOpenClassName = String.format("%sColType%d", spreadsheetHeader.getName(), columnIndex);

        SpreadsheetOpenClass columnOpenClass = new SpreadsheetOpenClass(columnOpenClassName, bindingContext.getOpenL());

        for (int rowIndex = 0; rowIndex < cells.length; rowIndex++) {

            SpreadsheetHeaderDefinition headerDefinition = rowHeaders.get(rowIndex);

            proc(rowIndex, columnOpenClass, columnIndex, headerDefinition);
        }
        return columnOpenClass;
    }

    private IBindingContext makeRowContext(int rowIndex) {

        /* create name for the row open class */
        String rowOpenClassName = String.format("%sRowType%d", spreadsheetHeader.getName(), rowIndex);

        // create row open class for current row
        SpreadsheetOpenClass rowOpenClass = new SpreadsheetOpenClass(rowOpenClassName, bindingContext.getOpenL());

        // get the width of the whole spreadsheet
        int width = cells[0].length;

        // create for each column in row its field
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {

            SpreadsheetHeaderDefinition columnHeader = columnHeaders.get(columnIndex);

            proc(rowIndex, rowOpenClass, columnIndex, columnHeader);
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

        SpreadsheetCell cell = cells[rowIndex][columnIndex];

        SymbolicTypeDefinition typeDefinition = columnHeader.getDefinition();
        String fieldName = (DOLLAR_SIGN + typeDefinition.getName().getIdentifier()).intern();
        createSpreadsheetCellField(rowOpenClass, cell, fieldName, SpreadsheetCellRefType.LOCAL);
    }

    private void createSpreadsheetCellField(ComponentOpenClass rowOpenClass,
                                            SpreadsheetCell cell,
                                            String fieldName,
                                            SpreadsheetCellRefType spreadsheetCellRefType) {
        SpreadsheetStructureBuilderHolder structureBuilderContainer = getSpreadsheetStructureBuilderHolder();
        SpreadsheetCellField field;
        if (cell.getSpreadsheetCellType() == SpreadsheetCellType.METHOD) {
            field = new SpreadsheetCellField(structureBuilderContainer,
                    rowOpenClass,
                    fieldName,
                    cell,
                    spreadsheetCellRefType);
        } else {
            field = new SpreadsheetCellField.ConstSpreadsheetCellField(structureBuilderContainer,
                    rowOpenClass,
                    fieldName,
                    cell);
        }
        rowOpenClass.addField(field);
    }

    private static String removeWrongSymbols(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (!s.isEmpty()) {
            s = s.replaceAll("\\s+", "_"); // Replace whitespaces
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                if (Character.isJavaIdentifierPart(s.charAt(i))) {
                    sb.append(s.charAt(i));
                }
            }
            s = sb.toString();
            if (JavaKeywordUtils.isJavaKeyword(s) || !s.isEmpty() && !Character.isJavaIdentifierStart(s.charAt(0))) {
                s = "_" + s;
            }
        }
        return s;
    }

    public String[] getRowNamesForResultModel() {
        return getNamesForResultModel(rowHeaders);
    }

    public String[] getColumnNamesForResultModel() {
        return getNamesForResultModel(columnHeaders);
    }

    private String[] getNamesForResultModel(Map<Integer, SpreadsheetHeaderDefinition> headers) {
        final long rowsWithAsteriskCount = headers.values().stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getDefinition().isAsteriskPresented())
                .count();
        String[] ret;
        if (rowsWithAsteriskCount > 0) {
            ret = buildArrayForHeaders(headers.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> e.getDefinition().isAsteriskPresented());
        } else {
            ret = buildArrayForHeaders(headers.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> !e.getDefinition().isTildePresented());
        }
        for (int i = 0; i < ret.length; i++) {
            ret[i] = removeWrongSymbols(ret[i]);
        }
        return ret;
    }

    public String[] getRowNames() {
        return buildArrayForHeaders(rowHeaders.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> true);
    }

    public String[] getColumnNames() {
        return buildArrayForHeaders(columnHeaders.values().toArray(EMPTY_SPREADSHEET_HEADER_DEFINITION_ARRAY), e -> true);
    }

    public ILogicalTable getTableBody() {
        return tableBody;
    }

    private String[] buildArrayForHeaders(SpreadsheetHeaderDefinition[] headers,
                                          Predicate<SpreadsheetHeaderDefinition> predicate) {

        int size = headers.length;
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            if (headers[i] != null && predicate.test(headers[i])) {
                ret[i] = headers[i].getDefinitionName();
            }
        }
        return ret;
    }

    private void addHeaders() {
        int height = tableBody.getHeight() - 1;
        int width = tableBody.getWidth() - 1;
        Set<String> registered = new HashSet<>();
        List<Integer> descriptionRows = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            ICell cell = tableBody.getCell(0, row + 1);
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
        Map<String, Integer> rowNames = new HashMap<>();
        rowHeaders.values().forEach(e -> rowNames.put(e.getDefinitionName(), e.getRow()));
        Set<String> rowNamesForDescription = new HashSet<>();
        descriptionRows.forEach(e -> parseDescription(e, true, rowNames, rowNamesForDescription));

        List<Integer> descriptionColumns = new ArrayList<>();
        for (int col = 0; col < width; col++) {
            ICell cell = tableBody.getCell(col + 1, 0);
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
        Map<String, Integer> columnNames = new HashMap<>();
        columnHeaders.values().forEach(e -> columnNames.put(e.getDefinitionName(), e.getColumn()));
        Set<String> columnNamesForDescription = new HashSet<>();
        descriptionColumns.forEach(e -> parseDescription(e, false, columnNames, columnNamesForDescription));

        IOpenClass spreadsheetHeaderType = spreadsheetHeader.getType();

        if (bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, SpreadsheetResult.class.getSimpleName())
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
            Integer mappedRowIndex = names.get(value);
            if (mappedRowIndex != null) {
                if (used.contains(value)) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format("The description column '%s' is already defined.", value), tableSyntaxNode));
                } else {
                    used.add(value);
                    descriptions.put(index, mappedRowIndex);
                }
            } else {
                bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format("The description column '%s' does not correspond to any existing %s in the table.", cell.getStringValue(), row ? "row" : "column"), tableSyntaxNode));
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
            bindingContext.addError(SyntaxNodeExceptionUtils.createError(String.format("The header '%s' is already defined.", headerName),
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
            String typeIdentifier = typeIdentifierNode.getOriginalText();
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
                .getMetaInfoReader() instanceof SpreadsheetMetaInfoReader) {
            IOpenClass headerType = headerDefinition.getType();
            SymbolicTypeDefinition symbolicTypeDefinition = headerDefinition.getDefinition();
            IdentifierNode typeIdentifierNode = symbolicTypeDefinition.getType();
            SpreadsheetMetaInfoReader metaInfoReader = (SpreadsheetMetaInfoReader) tableSyntaxNode
                    .getMetaInfoReader();
            List<NodeUsage> nodeUsages = new ArrayList<>();
            if (headerDefinition.getDefinition().isAsteriskPresented()) {
                String s = removeWrongSymbols(headerDefinition.getDefinitionName());
                if (org.apache.commons.lang3.StringUtils.isEmpty(s)) {
                    s = "Empty string";
                }
                String stringValue = cell.getStringValue();
                int d = stringValue.lastIndexOf(SpreadsheetSymbols.ASTERISK.toString());
                SimpleNodeUsage nodeUsage = new SimpleNodeUsage(0, d, s, null, NodeType.OTHER);
                nodeUsages.add(nodeUsage);
            }
            if (headerType != null) {
                IdentifierNode identifier = cutTypeIdentifier(typeIdentifierNode);
                if (identifier != null) {
                    IOpenClass type = headerType;
                    while (type.getMetaInfo() == null && type.isArray()) {
                        type = type.getComponentClass();
                    }
                    IMetaInfo typeMeta = type.getMetaInfo();
                    if (typeMeta != null) {
                        SimpleNodeUsage nodeUsage = new SimpleNodeUsage(identifier,
                                typeMeta.getDisplayName(INamedThing.SHORT),
                                typeMeta.getSourceUrl(),
                                type,
                                NodeType.DATATYPE);
                        nodeUsages.add(nodeUsage);
                    }
                }
            }
            if (!nodeUsages.isEmpty()) {
                CellMetaInfo cellMetaInfo = new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
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
        int fromRow = 0;
        int toRow = tableBody.getHeight();

        int fromColumn = 0;
        int toColumn = tableBody.getWidth();

        if (headerDefinition.isRow()) {
            fromRow = headerDefinition.getRow();
            toRow = fromRow + 1;
        } else {
            fromColumn = headerDefinition.getColumn();
            toColumn = fromColumn + 1;
        }

        int nonEmptyCellsCount = 0;

        for (int columnIndex = fromColumn; columnIndex < toColumn; columnIndex++) {
            for (int rowIndex = fromRow; rowIndex < toRow; rowIndex++) {
                if (headerDefinition.isRow() && columnHeaders.containsKey(columnIndex) || !headerDefinition.isRow() && rowHeaders.containsKey(rowIndex)) {
                    var value = tableBody.getCell(columnIndex + 1, rowIndex + 1).getStringValue();
                    boolean isFormula = SpreadsheetExpressionMarker.isFormula(value);
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
                .findType(ISyntaxConstants.THIS_NAMESPACE, SpreadsheetResult.class.getSimpleName())
                .equals(spreadsheet.getHeader().getType())) {
            resultBuilder = new SpreadsheetResultBuilder();
        } else {
            // real return type
            //
            List<SpreadsheetCell> returnSpreadsheetCells = new ArrayList<>();
            List<IOpenCast> casts = new ArrayList<>();
            List<SpreadsheetCell> returnSpreadsheetCellsAsArray = new ArrayList<>();
            List<IOpenCast> castsAsArray = new ArrayList<>();

            IOpenClass type = spreadsheet.getType();
            IAggregateInfo aggregateInfo = type.getAggregateInfo();
            IOpenClass componentType = aggregateInfo.getComponentType(type);
            boolean asArray = false;

            List<SpreadsheetCell> sprCells = new ArrayList<>();
            int n = returnHeaderDefinition.getRow();
            if (n < 0) {
                n = returnHeaderDefinition.getColumn();
                for (int i = 0; i < spreadsheet.getCells().length; i++) {
                    sprCells.add(spreadsheet.getCells()[i][n]);
                }
            } else {
                sprCells.addAll(Arrays.asList(spreadsheet.getCells()[n]));
            }

            List<SpreadsheetCell> nonEmptySpreadsheetCells = new ArrayList<>();
            for (SpreadsheetCell cell : sprCells) {
                if (cell != null && !cell.isEmpty()) {
                    nonEmptySpreadsheetCells.add(cell);
                    if (cell.getType() != null) {
                        IOpenCast cast = bindingContext.getCast(cell.getType(), type);
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
                    SpreadsheetCell spreadsheetCell = returnSpreadsheetCells.get(returnSpreadsheetCells.size() - 1);
                    spreadsheetCell.setReturnCell(true);
                }
            } else if (!nonEmptySpreadsheetCells.isEmpty()) {
                if (asArray) {
                    for (SpreadsheetCell cell : nonEmptySpreadsheetCells) {
                        cell.setReturnCell(true);
                    }
                } else {
                    SpreadsheetCell spreadsheetCell = nonEmptySpreadsheetCells.get(nonEmptySpreadsheetCells.size() - 1);
                    spreadsheetCell.setReturnCell(true);
                }
            }

            if (returnSpreadsheetCells.isEmpty()) {
                IdentifierNode symbolicTypeDefinitionName = Optional.ofNullable(returnHeaderDefinition)
                        .map(SpreadsheetHeaderDefinition::getDefinition)
                        .map(SymbolicTypeDefinition::getName)
                        .orElse(null);
                if (!nonEmptySpreadsheetCells.isEmpty()) {
                    SpreadsheetCell nonEmptySpreadsheetCell = nonEmptySpreadsheetCells.get(nonEmptySpreadsheetCells.size() - 1);
                    if (nonEmptySpreadsheetCell.getType() != null) {
                        throw SyntaxNodeExceptionUtils.createError(
                                String.format("Cannot convert from '%s' to '%s'.",
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
                        returnSpreadsheetCells.get(returnSpreadsheetCells.size() - 1),
                        casts.get(casts.size() - 1),
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
        int index = 0;
        for (Integer rowIndex : rowHeaders.keySet()) {
            rowOffsets.put(index, rowIndex);
            index++;
        }
        return rowOffsets;
    }

    public BidiMap<Integer, Integer> getColumnOffsets() {
        var columnOffsets = new DualHashBidiMap<Integer, Integer>();
        int index = 0;
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

    private static class CellSourceCodeModule extends StringSourceCodeModule {

        public CellSourceCodeModule(ICell cell, ILogicalTable table) {
            super(cell.getStringValue(), table.getSource().getUri(cell.getColumn(), cell.getRow() - 1));
        }
    }
}
