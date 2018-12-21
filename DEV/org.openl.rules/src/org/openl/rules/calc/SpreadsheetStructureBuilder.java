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
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.StringValue;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.element.SpreadsheetExpressionMarker;
import org.openl.rules.calc.element.SpreadsheetStructureBuilderHolder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
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
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.ConstOpenField;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;

public class SpreadsheetStructureBuilder {

    private static final String EMPTY_ROW_NAME = "$rowName";

    private static final String EMPTY_COLUMN_NAME = "$columnName";

    public static final String DOLLAR_SIGN = "$";

    private static final String COLUMN_FIELD = "$column";
    private static final String ROW_FIELD = "$row";

    private SpreadsheetComponentsBuilder componentsBuilder;

    private IBindingContext spreadsheetBindingContext;

    private IOpenMethodHeader spreadsheetHeader;

    private Boolean autoType;

    private SpreadsheetStructureBuilderHolder spreadsheetStructureBuilderHolder = new SpreadsheetStructureBuilderHolder(
        this);

    public SpreadsheetStructureBuilderHolder getSpreadsheetStructureBuilderHolder() {
        return spreadsheetStructureBuilderHolder;
    }

    public SpreadsheetStructureBuilder(TableSyntaxNode tableSyntaxNode,
            IBindingContext bindingContext,
            IOpenMethodHeader spreadsheetHeader,
            Boolean autoType) {
        this.componentsBuilder = new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
        this.spreadsheetHeader = spreadsheetHeader;
        this.autoType = autoType;
    }

    private Map<Integer, IBindingContext> rowContexts = new HashMap<Integer, IBindingContext>();
    private Map<Integer, Map<Integer, IBindingContext>> colContexts = new HashMap<Integer, Map<Integer, IBindingContext>>();

    private List<SpreadsheetCell> formulaCells = new ArrayList<SpreadsheetCell>();

    private SpreadsheetCell[][] cells;

    /**
     * Add to {@link SpreadsheetOpenClass} fields that are represented by spreadsheet cells.
     * 
     * @param spreadsheetType open class of the spreadsheet
     */
    public void addCellFields(SpreadsheetOpenClass spreadsheetType) {
        /**
         * at first appropriate data should be extracted from the source table
         **/
        componentsBuilder.buildHeaders(spreadsheetHeader.getType());

        /** build cells representations of the spreadsheet */
        buildCellsInternal(spreadsheetType);
    }

    /**
     * Extract cell values from the source spreadsheet table.
     * 
     * @return cells of spreadsheet with its values
     */
    public SpreadsheetCell[][] getCells() {
        extractCellValues();
        return cells.clone();
    }

    public IResultBuilder getResultBuilder(Spreadsheet spreadsheet) {
        return componentsBuilder.buildResultBuilder(spreadsheet);
    }

    public String[] getRowNames() {
        return componentsBuilder.getRowNames();
    }

    public String[] getColumnNames() {
        return componentsBuilder.getColumnNames();
    }

    public String[] getRowTitles() {
        return componentsBuilder.getCellsHeadersExtractor().getRowNames();
    }

    public String[] getColumnTitles() {
        return componentsBuilder.getCellsHeadersExtractor().getColumnNames();
    }

    private void buildCellsInternal(SpreadsheetOpenClass spreadsheetType) {
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();

        CellsHeaderExtractor cellsHeadersExtractor = componentsBuilder.getCellsHeadersExtractor();
        int rowsCount = cellsHeadersExtractor.getHeight();
        int columnsCount = cellsHeadersExtractor.getWidth();

        /** create cells according to the size of the spreadsheet **/
        cells = new SpreadsheetCell[rowsCount][columnsCount];

        /** create the binding context for the spreadsheet level **/
        spreadsheetBindingContext = initSpreadsheetBindingContext(spreadsheetType, generalBindingContext);

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {
                /** build spreadsheet cell */
                SpreadsheetCell spreadsheetCell = buildCell(rowIndex, columnIndex);

                /** init cells array with appropriate cell */
                cells[rowIndex][columnIndex] = spreadsheetCell;

                /** create and add field of the cell to the spreadsheetType */
                addSpreadsheetFields(spreadsheetType, spreadsheetCell, rowIndex, columnIndex);
            }
        }
    }

    private IBindingContext initSpreadsheetBindingContext(SpreadsheetOpenClass spreadsheetType,
            IBindingContext generalBindingContext) {
        return new SpreadsheetContext(generalBindingContext, spreadsheetType);
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

    private List<SpreadsheetCell> processingCells = new ArrayList<SpreadsheetCell>();
    private List<SpreadsheetCell> extractedCellValues = new ArrayList<SpreadsheetCell>();

    public IOpenClass makeType(SpreadsheetCell cell) {
        if (cell.getType() == null) {
            int rowIndex = cell.getRowIndex();
            int columnIndex = cell.getColumnIndex();

            IBindingContext rowContext = getRowContext(rowIndex);
            checkAndAddProcessingLoop(cell);

            extractCellValue(rowContext, rowIndex, columnIndex);
            extractedCellValues.add(cell);
            cleanProcessingLoop(cell);
            if (cell.getType() == null) {
                cell.setType(JavaOpenClass.OBJECT);
            }
        }
        return cell.getType();
    }

    private void cleanProcessingLoop(SpreadsheetCell cell) {
        processingCells.remove(cell);
    }

    private void checkAndAddProcessingLoop(SpreadsheetCell cell) {
        if (processingCells.contains(cell)) {
            throw new OpenlNotCheckedException("Spreadsheet Expression Loop:" + processingCells.toString());
        }
        processingCells.add(cell);
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

        if (SpreadsheetExpressionMarker.isFormula(code)) {
            formulaCells.add(spreadsheetCell);
        }

        String name = getSpreadsheetCellFieldName(columnHeaders.get(columnIndex).getFirstname(),
            rowHeaders.get(rowIndex).getFirstname());

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
            } catch (Exception e) {
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
                String message = String.format("Cannot parse cell!", code);
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
                        result = RuleRowHelper.loadNativeValue(theCellValue, type, bindingContext);
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

        for (SymbolicTypeDefinition columnDefinition : columnHeaders.getVars()) {
            for (SymbolicTypeDefinition rowDefinition : rowHeaders.getVars()) {
                /** get column name from the column definition */
                String columnName = columnDefinition.getName().getIdentifier();

                /** get row name from the row definition */
                String rowName = rowDefinition.getName().getIdentifier();

                /** create name of the field */
                String fieldname = getSpreadsheetCellFieldName(columnName, rowName);

                /** create spreadsheet cell field */
                SpreadsheetCellField field = createSpreadsheetCellField(spreadsheetType, spreadsheetCell, fieldname);

                /** add spreadsheet cell field to its open class */
                spreadsheetType.addField(field);
            }
        }
    }

    /**
     * Gets the name of the spreadsheet cell field. <br>
     * Is represented as {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN} rowName, e.g. $Value$Final
     * 
     * @param columnName name of cell column
     * @param rowName name of the row column
     * @return {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN}rowName, e.g. $Value$Final
     */
    private String getSpreadsheetCellFieldName(String columnName, String rowName) {
        return (DOLLAR_SIGN + columnName + DOLLAR_SIGN + rowName).intern();
    }

    private SpreadsheetCell buildCell(int rowIndex, int columnIndex) {
        Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = componentsBuilder.getColumnHeaders();
        Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = componentsBuilder.getRowHeaders();

        ILogicalTable cell = LogicalTableHelper.mergeBounds(
            componentsBuilder.getCellsHeadersExtractor().getRowNamesTable().getRow(rowIndex),
            componentsBuilder.getCellsHeadersExtractor().getColumnNamesTable().getColumn(columnIndex));
        ICell sourceCell = cell.getSource().getCell(0, 0);

        String cellCode = sourceCell.getStringValue();

        IOpenField openField = null;

        SpreadsheetCellType spreadsheetCellType = null;
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

        IOpenClass cellType = deriveCellType(columnHeaders.get(columnIndex),
            rowHeaders.get(rowIndex),
            cellCode,
            openField);
        spreadsheetCell.setType(cellType);

        return spreadsheetCell;
    }

    private IOpenClass deriveCellType(SpreadsheetHeaderDefinition columnHeader,
            SpreadsheetHeaderDefinition rowHeader,
            String cellValue,
            IOpenField constantField) {
        if (constantField != null) {
            return constantField.getType();
        } else if (columnHeader != null && columnHeader.getType() != null) {
            return columnHeader.getType();
        } else if (rowHeader != null && rowHeader.getType() != null) {
            return rowHeader.getType();
        } else {

            // Try to derive cell type as double.
            //
            try {
                if (SpreadsheetExpressionMarker.isFormula(cellValue)) {
                    return autoType ? null : JavaOpenClass.getOpenClass(DoubleValue.class);
                }

                // Try to parse cell value.
                // If parse process will be finished with success then return
                // double type else string type.
                //
                if (autoType) {
                    String2DataConvertorFactory.getConvertor(Double.class).parse(cellValue, null);
                    return JavaOpenClass.getOpenClass(Double.class);
                } else {
                    String2DataConvertorFactory.getConvertor(DoubleValue.class).parse(cellValue, null);
                    return JavaOpenClass.getOpenClass(DoubleValue.class);
                }
            } catch (Exception t) {
                if (autoType) {
                    return JavaOpenClass.getOpenClass(String.class);
                } else {
                    return JavaOpenClass.getOpenClass(StringValue.class);
                }
            }
        }
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
        Map<Integer, IBindingContext> contexts = colContexts.get(columnIndex);
        if (contexts == null) {
            contexts = new HashMap<Integer, IBindingContext>();
            colContexts.put(columnIndex, contexts);
        }
        IBindingContext context = contexts.get(rowIndex);
        if (context == null) {
            context = makeColumnContext(columnIndex, rowBindingContext);
            contexts.put(rowIndex, context);
        }
        return context;
    }

    private IBindingContext makeColumnContext(int columnIndex, IBindingContext rowBindingContext) {
        /** create name for the column open class */
        String columnOpenClassName = String.format("%sColType%d", spreadsheetHeader.getName(), columnIndex);

        ComponentOpenClass columnOpenClass = createAndPopulateColumnOpenClass(columnIndex, columnOpenClassName);

        return new SpreadsheetContext(rowBindingContext, columnOpenClass);
    }

    private ComponentOpenClass createRowOrColumnOpenClass(String openClassName, OpenL openl) {
        return new ComponentOpenClass(openClassName, openl);
    }

    private IBindingContext makeRowContext(int rowIndex) {

        /** create name for the row open class */
        String rowOpenClassName = String.format("%sRowType%d", spreadsheetHeader.getName(), rowIndex);

        /** create row open class and populate it with fields **/
        ComponentOpenClass rowOpenClass = createAndPopulateRowOpenClass(rowIndex, rowOpenClassName);

        /** create row binding context **/
        return new SpreadsheetContext(spreadsheetBindingContext, rowOpenClass);
    }

    private ComponentOpenClass createAndPopulateColumnOpenClass(int columnIndex, String columnOpenClassName) {
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();

        ComponentOpenClass columnOpenClass = createRowOrColumnOpenClass(columnOpenClassName,
            generalBindingContext.getOpenL());

        int height = cells.length;

        for (int rowIndex = 0; rowIndex < height; rowIndex++) {

            SpreadsheetHeaderDefinition headerDefinition = componentsBuilder.getRowHeaders().get(rowIndex);

            if (headerDefinition == null) {
                continue;
            }

            SpreadsheetCell cell = cells[rowIndex][columnIndex];

            for (SymbolicTypeDefinition typeDefinition : headerDefinition.getVars()) {
                String fieldName = (DOLLAR_SIGN + typeDefinition.getName().getIdentifier()).intern();
                SpreadsheetCellField field = createSpreadsheetCellField(columnOpenClass, cell, fieldName);

                columnOpenClass.addField(field);
            }
        }
        String nameOpenField = COLUMN_FIELD;
        IOpenField columnField = new ConstOpenField(nameOpenField, columnIndex, JavaOpenClass.INT);
        columnOpenClass.addField(columnField);
        SpreadsheetHeaderDefinition shd = componentsBuilder.getRowHeaders().get(columnIndex);
        if (shd != null) {
            String columnName = shd.getFirstname();
            if (columnName != null) {
                IOpenField columnNameField = new ConstOpenField(EMPTY_COLUMN_NAME, columnName, JavaOpenClass.STRING);
                columnOpenClass.addField(columnNameField);
            }
        }
        return columnOpenClass;
    }

    private ComponentOpenClass createAndPopulateRowOpenClass(int rowIndex, String rowOpenClassName) {
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();

        /** create row open class for current row **/
        ComponentOpenClass rowOpenClass = createRowOrColumnOpenClass(rowOpenClassName,
            generalBindingContext.getOpenL());

        /** get the width of the whole spreadsheet **/
        int width = cells[0].length;

        /** create for each column in row its field */
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {

            SpreadsheetHeaderDefinition columnHeader = componentsBuilder.getColumnHeaders().get(columnIndex);

            if (columnHeader == null) {
                continue;
            }

            SpreadsheetCell cell = cells[rowIndex][columnIndex];

            for (SymbolicTypeDefinition typeDefinition : columnHeader.getVars()) {
                String fieldName = (DOLLAR_SIGN + typeDefinition.getName().getIdentifier()).intern();
                SpreadsheetCellField field = createSpreadsheetCellField(rowOpenClass, cell, fieldName);

                rowOpenClass.addField(field);
            }
        }

        String nameOpenField = ROW_FIELD;
        IOpenField rowField = new ConstOpenField(nameOpenField, rowIndex, JavaOpenClass.INT);
        rowOpenClass.addField(rowField);

        SpreadsheetHeaderDefinition shd = componentsBuilder.getRowHeaders().get(rowIndex);
        if (shd != null) {
            String rowName = shd.getFirstname();
            if (rowName != null) {
                IOpenField rowNameField = new ConstOpenField(EMPTY_ROW_NAME, rowName, JavaOpenClass.STRING);
                rowOpenClass.addField(rowNameField);
            }
        }
        return rowOpenClass;
    }

    private SpreadsheetCellField createSpreadsheetCellField(IOpenClass rowOpenClass,
            SpreadsheetCell cell,
            String fieldName) {
        SpreadsheetStructureBuilderHolder structureBuilderContainer = getSpreadsheetStructureBuilderHolder();
        if (cell.getSpreadsheetCellType() == SpreadsheetCellType.METHOD) {
            return new SpreadsheetCellField(structureBuilderContainer, rowOpenClass, fieldName, cell);
        } else {
            return new SpreadsheetCellField.ConstSpreadsheetCellField(structureBuilderContainer, rowOpenClass, fieldName, cell);
        }
    }
}
