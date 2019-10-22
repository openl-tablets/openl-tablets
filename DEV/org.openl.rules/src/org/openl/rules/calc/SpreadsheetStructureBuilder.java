package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.*;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.*;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.*;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;

public class SpreadsheetStructureBuilder {

    public static final String DOLLAR_SIGN = "$";

    private SpreadsheetComponentsBuilder componentsBuilder;

    private IBindingContext spreadsheetBindingContext;

    private IOpenMethodHeader spreadsheetHeader;

    private SpreadsheetStructureBuilderHolder spreadsheetStructureBuilderHolder = new SpreadsheetStructureBuilderHolder(
        this);

    public SpreadsheetStructureBuilderHolder getSpreadsheetStructureBuilderHolder() {
        return spreadsheetStructureBuilderHolder;
    }

    public SpreadsheetStructureBuilder(SpreadsheetComponentsBuilder componentsBuilder,
            IOpenMethodHeader spreadsheetHeader) {
        this.componentsBuilder = componentsBuilder;
        this.spreadsheetHeader = spreadsheetHeader;
    }

    private Map<Integer, IBindingContext> rowContexts = new HashMap<>();
    private Map<Integer, Map<Integer, IBindingContext>> colContexts = new HashMap<>();

    private SpreadsheetCell[][] cells;

    /**
     * Extract cell values from the source spreadsheet table.
     *
     * @return cells of spreadsheet with its values
     */
    public SpreadsheetCell[][] getCells() {
        extractCellValues();
        return cells;
    }

    /**
     * Add to {@link SpreadsheetOpenClass} fields that are represented by spreadsheet cells.
     *
     * @param spreadsheetType open class of the spreadsheet
     */
    public void addCellFields(SpreadsheetOpenClass spreadsheetType, boolean autoType) {
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();

        CellsHeaderExtractor cellsHeadersExtractor = componentsBuilder.getCellsHeadersExtractor();
        int rowsCount = cellsHeadersExtractor.getHeight();
        int columnsCount = cellsHeadersExtractor.getWidth();

        // create cells according to the size of the spreadsheet
        cells = new SpreadsheetCell[rowsCount][columnsCount];

        // create the binding context for the spreadsheet level
        spreadsheetBindingContext = new SpreadsheetContext(generalBindingContext, spreadsheetType);

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {
                // build spreadsheet cell
                SpreadsheetCell spreadsheetCell = buildCell(rowIndex, columnIndex, autoType);

                // init cells array with appropriate cell
                cells[rowIndex][columnIndex] = spreadsheetCell;

                // create and add field of the cell to the spreadsheetType
                addSpreadsheetFields(spreadsheetType, spreadsheetCell, rowIndex, columnIndex);
            }
        }
    }

    private void extractCellValues() {
        CellsHeaderExtractor cellsHeadersExtractor = componentsBuilder.getCellsHeadersExtractor();
        int rowsCount = cellsHeadersExtractor.getHeight();
        int columnsCount = cellsHeadersExtractor.getWidth();

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            IBindingContext rowBindingContext = getRowContext(rowIndex);

            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {
                boolean found = false;
                for (SpreadsheetCell cell : extractedCellValues) {
                    int row = cell.getRowIndex();
                    int column = cell.getColumnIndex();
                    if (row == rowIndex && columnIndex == column) {
                        found = true;
                    }
                }
                if (!found) {
                    extractCellValue(rowBindingContext, rowIndex, columnIndex);
                }
            }
        }
    }

    private List<SpreadsheetCell> processingCells = new ArrayList<>();
    private List<SpreadsheetCell> extractedCellValues = new ArrayList<>();

    public IOpenClass makeType(SpreadsheetCell cell) {
        if (cell.getType() == null) {
            int rowIndex = cell.getRowIndex();
            int columnIndex = cell.getColumnIndex();

            IBindingContext rowContext = getRowContext(rowIndex);
            if (processingCells.contains(cell)) {
                cell.setType(JavaOpenClass.OBJECT);
                throw new OpenlNotCheckedException("Spreadsheet Expression Loop: " + processingCells.toString());
            }
            processingCells.add(cell);

            extractCellValue(rowContext, rowIndex, columnIndex);
            extractedCellValues.add(cell);
            processingCells.remove(cell);
            if (cell.getType() == null) {
                cell.setType(JavaOpenClass.OBJECT);
            }
        }
        return cell.getType();
    }

    private void extractCellValue(IBindingContext rowBindingContext, int rowIndex, int columnIndex) {
        Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = componentsBuilder.getColumnHeaders();
        Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = componentsBuilder.getRowHeaders();

        SpreadsheetCell spreadsheetCell = cells[rowIndex][columnIndex];

        if (columnHeaders.get(columnIndex) == null || rowHeaders.get(rowIndex) == null) {
            spreadsheetCell.setValue(null);
            return;
        }

        ILogicalTable cell = LogicalTableHelper.mergeBounds(
            componentsBuilder.getCellsHeadersExtractor().getRowNamesTable().getRow(rowIndex),
            componentsBuilder.getCellsHeadersExtractor().getColumnNamesTable().getColumn(columnIndex));

        IOpenSourceCodeModule source = new GridCellSourceCodeModule(cell.getSource(), spreadsheetBindingContext);
        String code = StringUtils.trimToNull(source.getCode());

        String name = getSpreadsheetCellFieldName(columnHeaders.get(columnIndex).getDefinitionName(),
            rowHeaders.get(rowIndex).getDefinitionName());

        IOpenClass type = spreadsheetCell.getType();

        if (code == null) {
            spreadsheetCell.setValue(null);
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
                    method = OpenLCellExpressionsCompiler.makeMethodWithUnknownType(openl,
                        srcCode,
                        name,
                        signature,
                        declaringClass,
                        columnBindingContext);
                    spreadsheetCell.setType(method.getType());
                } else {
                    method = OpenLCellExpressionsCompiler.makeMethod(openl, srcCode, header, columnBindingContext);
                }
                spreadsheetCell.setValue(method);
            } catch (CompositeSyntaxNodeException e) {
                componentsBuilder.getTableSyntaxNode().addError(e);
                BindHelper.processError(e, spreadsheetBindingContext);
            } catch (Exception | LinkageError e) {
                String message = String.format("Cannot parse cell value: [%s] to the necessary type", code);

                addError(SyntaxNodeExceptionUtils
                    .createError(message, e, LocationUtils.createTextInterval(source.getCode()), source));
            }

        } else if (spreadsheetCell.isConstantCell()) {
            try {
                IOpenField openField = rowBindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, code, true);
                ConstantOpenField constOpenField = (ConstantOpenField) openField;
                spreadsheetCell.setValue(constOpenField.getValue());
            } catch (Exception e) {
                String message = "Cannot parse cell.";
                addError(SyntaxNodeExceptionUtils.createError(message, e, null, source));
            }
        } else {
            Class<?> instanceClass = type.getInstanceClass();
            if (instanceClass == null) {
                String message = String.format("Type '%s' was loaded with errors", type.getName());
                addError(SyntaxNodeExceptionUtils.createError(message, source));
            }

            try {
                IBindingContext bindingContext = getColumnContext(columnIndex, rowIndex, rowBindingContext);
                ICell theCellValue = cell.getCell(0, 0);
                Object result = null;
                if (String.class.equals(instanceClass)) {
                    result = String2DataConvertorFactory.parse(instanceClass, code, bindingContext);
                } else {
                    if (theCellValue.hasNativeType()) {
                        result = RuleRowHelper.loadNativeValue(theCellValue, type);
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
                String message = String.format("Cannot parse cell value: [%s] to the necessary type", code);
                addError(SyntaxNodeExceptionUtils.createError(message, t, null, source));
            }
        }
    }

    private void addError(SyntaxNodeException e) {
        componentsBuilder.getTableSyntaxNode().addError(e);
        spreadsheetBindingContext.addError(e);
    }

    /**
     * Creates a field from the spreadsheet cell and add it to the spreadsheetType
     */
    private void addSpreadsheetFields(SpreadsheetOpenClass spreadsheetType,
            SpreadsheetCell spreadsheetCell,
            int rowIndex,
            int columnIndex) {
        SpreadsheetHeaderDefinition columnHeaders = componentsBuilder.getColumnHeaders().get(columnIndex);
        SpreadsheetHeaderDefinition rowHeaders = componentsBuilder.getRowHeaders().get(rowIndex);

        if (columnHeaders == null || rowHeaders == null) {
            return;
        }

        boolean oneColumnSpreadsheet = componentsBuilder.getColumnHeaders().size() == 1;
        boolean oneRowSpreadsheet = componentsBuilder.getRowHeaders().size() == 1;

        SymbolicTypeDefinition columnDefinition = columnHeaders.getDefinition();
        SymbolicTypeDefinition rowDefinition = rowHeaders.getDefinition();

        // get column name from the column definition
        String columnName = columnDefinition.getName().getIdentifier();

        // get row name from the row definition
        String rowName = rowDefinition.getName().getIdentifier();

        // create name of the field
        String fieldname = getSpreadsheetCellFieldName(columnName, rowName);

        // create spreadsheet cell field
        SpreadsheetCellField field = createSpreadsheetCellField(spreadsheetType,
            spreadsheetCell,
            fieldname,
            SpreadsheetCellRefType.ROW_AND_COLUMN);

        // add spreadsheet cell field to its open class
        spreadsheetType.addField(field);

        if (oneColumnSpreadsheet) {
            // add simplified field name
            String simplifiedFieldName = getSpreadsheetCellSimplifiedFieldName(rowName);
            IOpenField field1 = spreadsheetType.getField(simplifiedFieldName);
            if (field1 == null) {
                SpreadsheetCellField simplifiedField = createSpreadsheetCellField(spreadsheetType,
                    spreadsheetCell,
                    simplifiedFieldName,
                    SpreadsheetCellRefType.SINGLE_COLUMN);
                spreadsheetType.addField(simplifiedField);
            }
        } else if (oneRowSpreadsheet) {
            // add simplified field name
            String simplifiedFieldName = getSpreadsheetCellSimplifiedFieldName(columnName);
            IOpenField field1 = spreadsheetType.getField(simplifiedFieldName);
            if (field1 == null || (field1 instanceof SpreadsheetCellField && ((SpreadsheetCellField) field1)
                .isLastColumnRef())) {
                SpreadsheetCellField simplifiedField = createSpreadsheetCellField(spreadsheetType,
                    spreadsheetCell,
                    simplifiedFieldName,
                    SpreadsheetCellRefType.SINGLE_ROW);
                spreadsheetType.addField(simplifiedField);
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
     * @param rowName name of the row column
     * @return {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN}rowName, e.g. $Value$Final
     */
    public static String getSpreadsheetCellFieldName(String columnName, String rowName) {
        return (DOLLAR_SIGN + columnName + DOLLAR_SIGN + rowName).intern();
    }

    private SpreadsheetCell buildCell(int rowIndex, int columnIndex, boolean autoType) {
        Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = componentsBuilder.getColumnHeaders();
        Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = componentsBuilder.getRowHeaders();

        ILogicalTable cell = LogicalTableHelper.mergeBounds(
            componentsBuilder.getCellsHeadersExtractor().getRowNamesTable().getRow(rowIndex),
            componentsBuilder.getCellsHeadersExtractor().getColumnNamesTable().getColumn(columnIndex));
        ICell sourceCell = cell.getSource().getCell(0, 0);

        String cellCode = sourceCell.getStringValue();

        IOpenField openField = null;

        SpreadsheetCellType spreadsheetCellType;
        if (cellCode == null || cellCode.isEmpty() || columnHeaders.get(columnIndex) == null || rowHeaders
            .get(rowIndex) == null) {
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

        SpreadsheetHeaderDefinition columnHeader = columnHeaders.get(columnIndex);
        SpreadsheetHeaderDefinition rowHeader = rowHeaders.get(rowIndex);
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
                    } else {
                        String2DataConvertorFactory.getConvertor(Double.class).parse(cellCode, null);
                        cellType = JavaOpenClass.getOpenClass(Double.class);
                    }
                } else {
                    if (!SpreadsheetExpressionMarker.isFormula(cellCode)) {
                        String2DataConvertorFactory.getConvertor(DoubleValue.class).parse(cellCode, null);
                    }
                    cellType = JavaOpenClass.getOpenClass(DoubleValue.class);
                }
            } catch (Exception t) {
                if (autoType) {
                    cellType = JavaOpenClass.getOpenClass(String.class);
                } else {
                    cellType = JavaOpenClass.getOpenClass(StringValue.class);
                }
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

    private IBindingContext getColumnContext(int columnIndex, int rowIndex, IBindingContext rowBindingContext) {
        Map<Integer, IBindingContext> contexts = colContexts.computeIfAbsent(columnIndex, e -> new HashMap<>());
        return contexts.computeIfAbsent(rowIndex, e -> makeColumnContext(columnIndex, rowBindingContext));
    }

    private IBindingContext makeColumnContext(int columnIndex, IBindingContext rowBindingContext) {
        // create name for the column open class
        String columnOpenClassName = String.format("%sColType%d", spreadsheetHeader.getName(), columnIndex);

        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();
        Map<Integer, SpreadsheetHeaderDefinition> headers = componentsBuilder.getRowHeaders();

        ComponentOpenClass columnOpenClass = new ComponentOpenClass(columnOpenClassName,
            generalBindingContext.getOpenL());

        int height = cells.length;

        for (int rowIndex = 0; rowIndex < height; rowIndex++) {

            SpreadsheetHeaderDefinition headerDefinition = headers.get(rowIndex);

            proc(rowIndex, columnOpenClass, columnIndex, headerDefinition);
        }
        return new SpreadsheetContext(rowBindingContext, columnOpenClass);
    }

    private IBindingContext makeRowContext(int rowIndex) {

        /** create name for the row open class */
        String rowOpenClassName = String.format("%sRowType%d", spreadsheetHeader.getName(), rowIndex);

        /** create row open class and populate it with fields **/
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();
        Map<Integer, SpreadsheetHeaderDefinition> headers = componentsBuilder.getColumnHeaders();

        // create row open class for current row
        ComponentOpenClass rowOpenClass = new ComponentOpenClass(rowOpenClassName, generalBindingContext.getOpenL());

        // get the width of the whole spreadsheet
        int width = cells[0].length;

        // create for each column in row its field
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {

            SpreadsheetHeaderDefinition columnHeader = headers.get(columnIndex);

            proc(rowIndex, rowOpenClass, columnIndex, columnHeader);
        }

        /** create row binding context **/
        return new SpreadsheetContext(spreadsheetBindingContext, rowOpenClass);
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
        SpreadsheetCellField field = createSpreadsheetCellField(rowOpenClass,
            cell,
            fieldName,
            SpreadsheetCellRefType.LOCAL);

        rowOpenClass.addField(field);
    }

    private SpreadsheetCellField createSpreadsheetCellField(IOpenClass rowOpenClass,
            SpreadsheetCell cell,
            String fieldName,
            SpreadsheetCellRefType spreadsheetCellRefType) {
        SpreadsheetStructureBuilderHolder structureBuilderContainer = getSpreadsheetStructureBuilderHolder();
        if (cell.getSpreadsheetCellType() == SpreadsheetCellType.METHOD) {
            return new SpreadsheetCellField(structureBuilderContainer,
                rowOpenClass,
                fieldName,
                cell,
                spreadsheetCellRefType);
        } else {
            return new SpreadsheetCellField.ConstSpreadsheetCellField(structureBuilderContainer,
                rowOpenClass,
                fieldName,
                cell);
        }
    }
}
