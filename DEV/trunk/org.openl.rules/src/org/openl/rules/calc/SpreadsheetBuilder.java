package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaInfo;
import org.openl.meta.StringValue;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.calc.element.CellLoader;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.result.ArrayResultBuilder;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.calc.result.ScalarResultBuilder;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.convertor.String2DoubleConvertor;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.ConstOpenField;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

public class SpreadsheetBuilder {

    private static final String RETURN_NAME = "RETURN";

    private Spreadsheet spreadsheet;
    private SpreadsheetHeaderDefinition returnHeaderDefinition;

    private IBindingContext bindingContext;

    private TableSyntaxNode tableSyntaxNode;

    private ILogicalTable rowNamesTable;
    private ILogicalTable columnNamesTable;

    private Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    private Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    private Map<String, SpreadsheetHeaderDefinition> varDefinitions = new HashMap<String, SpreadsheetHeaderDefinition>();

    private Map<Integer, IBindingContext> rowContexts = new HashMap<Integer, IBindingContext>();
    private Map<Integer, IBindingContextDelegator> colContexts = new HashMap<Integer, IBindingContextDelegator>();

    private List<SpreadsheetCell> formulaCells = new ArrayList<SpreadsheetCell>();

    public SpreadsheetBuilder(IBindingContext bindingContext, Spreadsheet spreadsheet, TableSyntaxNode tableSyntaxNode) {
        this.bindingContext = bindingContext;
        this.spreadsheet = spreadsheet;
        this.tableSyntaxNode = tableSyntaxNode;
    }

    public void build(ILogicalTable tableBody) {

        buildColumnRowNames(tableBody);
        buildHeaderTypes();

        try {
            processReturnCells();
            buildCells();
            buildReturn();
        } catch (SyntaxNodeException e) {
            tableSyntaxNode.addError(e);
            BindHelper.processError(e, bindingContext);
        }
    }

    private void addColumnHeader(int column, StringValue value) {

        SpreadsheetHeaderDefinition header = columnHeaders.get(column);

        if (header == null) {
            header = new SpreadsheetHeaderDefinition(-1, column);
            columnHeaders.put(column, header);
        }

        parseHeader(header, value);
    }

    private void addRowHeader(int row, StringValue value) {

        SpreadsheetHeaderDefinition header = rowHeaders.get(row);

        if (header == null) {
            header = new SpreadsheetHeaderDefinition(row, -1);
            rowHeaders.put(row, header);
        }

        parseHeader(header, value);
    }

    private void addColumnNames(int column, ILogicalTable logicalColumn) {

        for (int i = 0; i < logicalColumn.getHeight(); i++) {

            IGridTable nameCell = logicalColumn.getRow(i).getSource();
            String value = nameCell.getCell(0, 0).getStringValue();

            if (value != null) {
                String shortName = "scol" + column + "_" + i;
                StringValue stringValue = new StringValue(value, shortName, null, new GridCellSourceCodeModule(nameCell,
                        bindingContext));

                addColumnHeader(column, stringValue);
            }

            spreadsheet.getColumnNames()[column] = value;
        }
    }

    private void addRowNames(int row, ILogicalTable logicalRow) {

        for (int i = 0; i < logicalRow.getWidth(); i++) {

            IGridTable nameCell = logicalRow.getColumn(i).getSource();
            String value = nameCell.getCell(0, 0).getStringValue();

            if (value != null) {
                String shortName = "srow" + row + "_" + i;
                StringValue sv = new StringValue(value, shortName, null, new GridCellSourceCodeModule(nameCell,
                        bindingContext));

                addRowHeader(row, sv);
            }

            spreadsheet.getRowNames()[row] = value;
        }
    }

    private void buildCells() {

        int rowsCount = rowNamesTable.getHeight();
        int columnsCount = columnNamesTable.getWidth();

        SpreadsheetOpenClass spreadsheetOpenClass = spreadsheet.getSpreadsheetType();
        IBindingContext bindingContext = new ComponentBindingContext(this.bindingContext, spreadsheetOpenClass);

        SpreadsheetCell[][] cells = new SpreadsheetCell[rowsCount][columnsCount];
        spreadsheet.setCells(cells);

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {
                ILogicalTable cell = LogicalTableHelper.mergeBounds(rowNamesTable.getRow(rowIndex),
                        columnNamesTable.getColumn(columnIndex));
                ICell sourceCell = cell.getSource().getCell(0, 0);

                SpreadsheetCell spreadsheetCell;
                if (bindingContext.isExecutionMode()) {
                    spreadsheetCell = new SpreadsheetCell(rowIndex, columnIndex, null);
                }else{
                    spreadsheetCell = new SpreadsheetCell(rowIndex, columnIndex, sourceCell);
                }
                cells[rowIndex][columnIndex] = spreadsheetCell;

                String code = sourceCell.getStringValue();
                IOpenClass type = deriveCellType(spreadsheetCell, cell, columnHeaders.get(columnIndex), rowHeaders
                        .get(rowIndex), code);
                spreadsheetCell.setType(type);

                if (columnHeaders.get(columnIndex) == null || rowHeaders.get(rowIndex) == null) {
                    continue;
                }

                for (SymbolicTypeDefinition columnDefinition : columnHeaders.get(columnIndex).getVars()) {
                    for (SymbolicTypeDefinition rowDefinition : rowHeaders.get(rowIndex).getVars()) {

                        String columnName = columnDefinition.getName().getIdentifier();
                        String rowName = rowDefinition.getName().getIdentifier();
                        String fieldname = "$" + columnName + "$" + rowName;
                        SpreadsheetCellField field = new SpreadsheetCellField(spreadsheetOpenClass, fieldname,
                                spreadsheetCell);

                        spreadsheetOpenClass.addField(field);
                    }
                }
            }
        }

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {

            IBindingContext rowBindingContext = getRowContext(rowIndex, bindingContext);

            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {

                if (columnHeaders.get(columnIndex) == null || rowHeaders.get(rowIndex) == null) {
                    continue;
                }

                IBindingContext columnBindingContext = getColumnContext(columnIndex, rowBindingContext);

                ILogicalTable cell = LogicalTableHelper.mergeBounds(rowNamesTable.getRow(rowIndex),
                        columnNamesTable.getColumn(columnIndex));

                SpreadsheetCell spreadsheetCell = cells[rowIndex][columnIndex];
                IOpenSourceCodeModule source = new GridCellSourceCodeModule(cell.getSource(), bindingContext);
                String code = source.getCode();

                if (CellLoader.isFormula(code)) {
                    formulaCells.add(spreadsheetCell);
                }

                String name = "$" + columnHeaders.get(columnIndex).getFirstname() + '$'
                        + rowHeaders.get(rowIndex).getFirstname();

                IMetaInfo meta = new ValueMetaInfo(name, null, source);

                IOpenMethodHeader header = makeHeader(meta.getDisplayName(INamedThing.SHORT), spreadsheet.getHeader(),
                        spreadsheetCell.getType());
                IString2DataConvertor convertor = makeConvertor(spreadsheetCell.getType());

                CellLoader loader = new CellLoader(columnBindingContext, header, convertor);

                try {
                    Object cellValue = loader.loadSingleParam(source, meta);
                    spreadsheetCell.setValue(cellValue);
                } catch (SyntaxNodeException e) {

                    tableSyntaxNode.addError(e);
                    BindHelper.processError(e, bindingContext);
                }
            }
        }
    }

    private void buildHeaderTypes() {

        SpreadsheetOpenClass spreadsheetType = new SpreadsheetOpenClass(null, spreadsheet.getName() + "Type",
                bindingContext.getOpenL());

        spreadsheet.setSpreadsheetType(spreadsheetType);

        for (SpreadsheetHeaderDefinition headerDefinition : varDefinitions.values()) {

            IOpenClass headerType = null;

            for (SymbolicTypeDefinition symbolicTypeDefinition : headerDefinition.getVars()) {

                if (symbolicTypeDefinition.getType() != null) {

                    SyntaxNodeException error = null;
                    IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, symbolicTypeDefinition
                            .getType().getIdentifier());

                    if (type == null) {
                        String message = "Type not found: " + symbolicTypeDefinition.getType().getIdentifier();
                        error = SyntaxNodeExceptionUtils.createError(message, symbolicTypeDefinition.getType());
                    } else if (headerType == null) {
                        headerType = type;
                    } else if (headerType != type) {
                        error = SyntaxNodeExceptionUtils.createError("Type redefinition", symbolicTypeDefinition
                                .getType());
                    }

                    if (error != null) {
                        tableSyntaxNode.addError(error);
                        BindHelper.processError(error, bindingContext);
                    }

                }
            }

            if (headerType != null) {
                headerDefinition.setType(headerType);
            }
        }
    }

    protected void buildColumnRowNames(ILogicalTable tableBody) {

        rowNamesTable = tableBody.getColumn(0).getRows(1);
        columnNamesTable = tableBody.getRow(0).getColumns(1);

        int height = rowNamesTable.getHeight();
        int width = columnNamesTable.getWidth();

        spreadsheet.setRowNames(new String[height]);
        spreadsheet.setColumnNames(new String[width]);

        for (int row = 0; row < height; row++) {
            addRowNames(row, rowNamesTable.getRow(row));
        }

        for (int col = 0; col < width; col++) {
            addColumnNames(col, columnNamesTable.getColumn(col));
        }
    }

    private void buildReturn() throws SyntaxNodeException {

        SymbolicTypeDefinition symbolicTypeDefinition = null;

        if (returnHeaderDefinition != null) {
            symbolicTypeDefinition = returnHeaderDefinition.findVarDef(RETURN_NAME);
        }

        if (spreadsheet.getHeader().getType() == JavaOpenClass.getOpenClass(SpreadsheetResult.class)) {
            if (returnHeaderDefinition != null) {
                throw SyntaxNodeExceptionUtils.createError(
                        "If Spreadsheet return type is SpreadsheetResult, no return type is allowed",
                        symbolicTypeDefinition.getName());
            }

            spreadsheet.setResultBuilder(new DefaultResultBuilder());

        } else if (spreadsheet.getHeader().getType() == JavaOpenClass.VOID) {
            throw SyntaxNodeExceptionUtils.createError("Spreadsheet can not return 'void' type", tableSyntaxNode);
        } else {

            // real return type
            //
            if (returnHeaderDefinition == null) {
                throw SyntaxNodeExceptionUtils.createError("There should be RETURN row or column for this return type",
                        tableSyntaxNode);
            }

            List<SpreadsheetCell> notEmpty = spreadsheet.listNonEmptyCells(returnHeaderDefinition);

            switch (notEmpty.size()) {
                case 0:
                    throw SyntaxNodeExceptionUtils.createError("There is no return expression cell",
                            symbolicTypeDefinition.getName());
                case 1:
                    spreadsheet.setResultBuilder(new ScalarResultBuilder(notEmpty));
                    break;
                default:
                    spreadsheet.setResultBuilder(new ArrayResultBuilder(notEmpty, returnHeaderDefinition.getType()));
            }
        }
    }

    private int calculateNonEmptyCells(SpreadsheetHeaderDefinition headerDefinition) {

        int fromRow = 0;
        int toRow = rowNamesTable.getHeight();

        int fromColumn = 0;
        int toColumn = columnNamesTable.getWidth();

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

                ILogicalTable cell = LogicalTableHelper.mergeBounds(rowNamesTable.getRow(rowIndex),
                        columnNamesTable.getColumn(columnIndex));

                String value = cell.getSource().getCell(0, 0).getStringValue();

                if (value != null && value.trim().length() > 0) {
                    nonEmptyCellsCount += 1;
                }
            }
        }

        return nonEmptyCellsCount;
    }

    private IOpenClass deriveCellType(SpreadsheetCell spreadsheetCell, ILogicalTable cell,
            SpreadsheetHeaderDefinition columnHeader, SpreadsheetHeaderDefinition rowHeader, String cellValue) {

        if (columnHeader != null && columnHeader.getType() != null) {
            return columnHeader.getType();
        } else if (rowHeader != null && rowHeader.getType() != null) {
            return rowHeader.getType();
        } else {

            // Try to derive cell type as double.
            //
            try {
                if (CellLoader.isFormula(cellValue)) {
                    return JavaOpenClass.getOpenClass(DoubleValue.class);
                }

                // Try to parse cell value.
                // If parse process will be finished with success then return
                // double type else string type.
                //
                new String2DoubleConvertor().parse(cellValue, null, null);

                return JavaOpenClass.getOpenClass(DoubleValue.class);
            } catch (Throwable t) {
                return JavaOpenClass.getOpenClass(StringValue.class);
            }
        }
    }

    /**
     * Derives single cell return type.
     * 
     * @param cellsCount
     * @param headerDefinition
     * @return the type that should be in the cell that is located in RETURN row
     *         or column
     * 
     * Right now we allow only to return types = scalars or arrays.
     * @throws BoundError
     */

    private IOpenClass deriveSingleCellReturnType(int cellsCount, SpreadsheetHeaderDefinition headerDefinition)
            throws SyntaxNodeException {

        IOpenClass returnType = spreadsheet.getHeader().getType();

        if (cellsCount < 2) {
            return returnType;
        }

        IAggregateInfo aggregateInfo = returnType.getAggregateInfo();

        if (aggregateInfo != null && aggregateInfo.getComponentType(returnType) != null) {
            returnType = aggregateInfo.getComponentType(returnType);
        } else {
            throw SyntaxNodeExceptionUtils.createError(
                    "The return type is scalar, but there are more than one return cells", headerDefinition.findVarDef(
                            RETURN_NAME).getName());
        }

        return returnType;
    }

    private IBindingContext getColumnContext(int columnIndex, IBindingContext bindingContext) {

        IBindingContextDelegator columnContext = colContexts.get(columnIndex);

        if (columnContext == null) {
            columnContext = makeColumnContext(columnIndex, bindingContext);
            colContexts.put(columnIndex, columnContext);
        } else {
            columnContext.setDelegate(bindingContext);
        }

        return columnContext;
    }

    private IBindingContext getRowContext(int rowIndex, IBindingContext bindingContext) {

        IBindingContext rowContext = rowContexts.get(rowIndex);

        if (rowContext == null) {
            rowContext = makeRowContext(rowIndex, bindingContext);
            rowContexts.put(rowIndex, rowContext);
        }

        return rowContext;
    }

    private IBindingContextDelegator makeColumnContext(int columnIndex, IBindingContext scxt) {

        ComponentOpenClass returnType = new ComponentOpenClass(null, spreadsheet.getName() + "ColType" + columnIndex,
                bindingContext.getOpenL());

        IBindingContextDelegator moduleBindingContext = new ComponentBindingContext(scxt, returnType);

        int height = spreadsheet.getHeight();

        for (int rowIndex = 0; rowIndex < height; rowIndex++) {

            SpreadsheetHeaderDefinition headerDefinition = rowHeaders.get(rowIndex);

            if (headerDefinition == null) {
                continue;
            }

            SpreadsheetCell cell = spreadsheet.getCells()[rowIndex][columnIndex];

            for (SymbolicTypeDefinition typeDefinition : headerDefinition.getVars()) {
                String fieldName = "$" + typeDefinition.getName().getIdentifier();
                SpreadsheetCellField field = new SpreadsheetCellField(returnType, fieldName, cell);

                returnType.addField(field);
            }
        }

        IOpenField columnField = new ConstOpenField("$column", columnIndex, JavaOpenClass.INT);
        returnType.addField(columnField);
        SpreadsheetHeaderDefinition shd = rowHeaders.get(columnIndex);
        if (shd != null) {
            String columnName = shd.getFirstname();
            if (columnName != null) {
                IOpenField columnNameField = new ConstOpenField("$columnName", columnName, JavaOpenClass.STRING);
                returnType.addField(columnNameField);
            }
        }

        return moduleBindingContext;
    }

    private IString2DataConvertor makeConvertor(IOpenClass type) {
        
        Class<?> instanceClass = type.getInstanceClass();
        if (instanceClass == null) {
            throw new OpenLRuntimeException(String.format("Type '%s' was loaded with errors", type.getName()));
        }
        
        return String2DataConvertorFactory.getConvertor(instanceClass);
    }

    private IOpenMethodHeader makeHeader(String name, IOpenMethodHeader header, IOpenClass type) {
        return new OpenMethodHeader(name, type, header.getSignature(), header.getDeclaringClass());
    }

    private IBindingContextDelegator makeRowContext(int rowIndex, IBindingContext scxt) {

        ComponentOpenClass returnType = new ComponentOpenClass(null, spreadsheet.getName() + "RowType" + rowIndex,
                bindingContext.getOpenL());

        IBindingContextDelegator componentBindingContext = new ComponentBindingContext(scxt, returnType);

        int width = spreadsheet.getWidth();

        for (int columnIndex = 0; columnIndex < width; columnIndex++) {

            SpreadsheetHeaderDefinition headerDefinition = columnHeaders.get(columnIndex);

            if (headerDefinition == null) {
                continue;
            }

            SpreadsheetCell cell = spreadsheet.getCells()[rowIndex][columnIndex];

            for (SymbolicTypeDefinition typeDefinition : headerDefinition.getVars()) {
                String fieldName = "$" + typeDefinition.getName().getIdentifier();
                SpreadsheetCellField field = new SpreadsheetCellField(returnType, fieldName, cell);

                returnType.addField(field);
            }
        }

        IOpenField rowField = new ConstOpenField("$row", rowIndex, JavaOpenClass.INT);
        returnType.addField(rowField);

        SpreadsheetHeaderDefinition shd = rowHeaders.get(rowIndex);
        if (shd != null) {
            String rowName = shd.getFirstname();
            if (rowName != null) {
                IOpenField rowNameField = new ConstOpenField("$rowName", rowName, JavaOpenClass.STRING);
                returnType.addField(rowNameField);
            }
        }

        return componentBindingContext;
    }

    private void parseHeader(SpreadsheetHeaderDefinition header, StringValue value) {

        try {
            SymbolicTypeDefinition parsed = parseHeaderElement(value);
            String headerName = parsed.getName().getIdentifier();

            SpreadsheetHeaderDefinition h1 = varDefinitions.get(headerName);

            if (h1 != null) {
                throw new DuplicatedVarException(null, headerName);
            } else {
                varDefinitions.put(headerName, header);
            }

            header.addVarHeader(parsed);

        } catch (Throwable t) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                    "Cannot parse spreadsheet header definition", t, null, value.asSourceCodeModule());

            tableSyntaxNode.addError(error);
            BindHelper.processError(error, bindingContext);
        }
    }

    private SymbolicTypeDefinition parseHeaderElement(StringValue value) throws SyntaxNodeException {

        IOpenSourceCodeModule source = value.asSourceCodeModule();
        IdentifierNode[] nodes;

        try {
            nodes = Tokenizer.tokenize(source, ":");
        } catch (OpenLCompilationException e) {
            throw SyntaxNodeExceptionUtils.createError("Cannot parse header", source);
        }

        switch (nodes.length) {
            case 1:
                return new SymbolicTypeDefinition(nodes[0], null);
            case 2:
                return new SymbolicTypeDefinition(nodes[0], nodes[1]);
            default:
                throw SyntaxNodeExceptionUtils.createError("Valid header format: name [: type]", source);
        }
    }

    private void processReturnCells() throws SyntaxNodeException {

        SpreadsheetHeaderDefinition headerDefinition = varDefinitions.get(RETURN_NAME);

        if (headerDefinition == null) {
            return;
        }

        int nonEmptyCellsCount = calculateNonEmptyCells(headerDefinition);

        IOpenClass cellType = deriveSingleCellReturnType(nonEmptyCellsCount, headerDefinition);

        if (headerDefinition.getType() == null) {
            headerDefinition.setType(cellType);
        } else {
            String message = String
                    .format(
                            "RETURN %s derives it's type from the Spreadsheet return type and therefore must not be defined here",
                            headerDefinition.rowOrColumn());

            throw SyntaxNodeExceptionUtils.createError(message, headerDefinition.getVars().get(0).getName());
        }

        returnHeaderDefinition = headerDefinition;
    }

}
