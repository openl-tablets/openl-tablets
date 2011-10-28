package org.openl.rules.calc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.BindHelper;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.StringValue;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.ArrayResultBuilder;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.calc.result.ScalarResultBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
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
import org.openl.types.java.JavaOpenClass;
import org.openl.util.generation.JavaClassGeneratorHelper;

public class SpreadsheetSourceExtractor {
    
    /** cell name indicating return statement*/
    public static final String RETURN_NAME = "RETURN";
    
    /** tableSyntaxNode of the spreadsheet**/
    private TableSyntaxNode tableSyntaxNode;
    
    /** binding context for indicating execution mode**/
    private IBindingContext bindingContext;
    
    /** table representing column section in the spreadsheet**/
    private ILogicalTable columnNamesTable;
    
    /** table representing row section in the spreadsheet**/
    private ILogicalTable rowNamesTable;
    
    private SpreadsheetHeaderDefinition returnHeaderDefinition;
    
    private String[] rowNames;
    private String[] columnNames;
    
    private Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    private Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    private Map<String, SpreadsheetHeaderDefinition> headerDefinitions = new HashMap<String, SpreadsheetHeaderDefinition>();
    
    public SpreadsheetSourceExtractor(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext) {        
        this.tableSyntaxNode = tableSyntaxNode;
        this.bindingContext = bindingContext;
        initRowColumnTables();
    }
    
    private void initRowColumnTables() {        
        rowNamesTable = tableSyntaxNode.getTableBody().getColumn(0).getRows(1);
        columnNamesTable = tableSyntaxNode.getTableBody().getRow(0).getColumns(1);
    }
    
    public String[] getRowNames() {        
        return rowNames.clone();
    }
    
    public String[] getColumnNames() {        
        return columnNames.clone();
    }
    
    public Map<Integer, SpreadsheetHeaderDefinition> getRowHeaders() {        
        return new HashMap<Integer, SpreadsheetHeaderDefinition>(rowHeaders);
    }
    
    public Map<Integer, SpreadsheetHeaderDefinition> getColumnHeaders() {        
        return new HashMap<Integer, SpreadsheetHeaderDefinition>(columnHeaders);
    }
    
    /**
     * Extract following data form the spreadsheet source table:
     * row names, column names, header definitions, return cell.
     * 
     * @param spreadsheetHeaderType
     */
    public void extractDataFromSource(IOpenClass spreadsheetHeaderType) {
        extractRowNames();
        extractColumnNames();
        buildHeaderDefinitionsTypes();
        
        try {            
            processReturnCells(spreadsheetHeaderType);
        } catch (SyntaxNodeException e) {            
            getTableSyntaxNode().addError(e);
            BindHelper.processError(e, getBindingContext());    
        }
    }
    
    public ILogicalTable getColumnNamesTable() {
        return columnNamesTable;
    }
    
    public ILogicalTable getRowNamesTable() {
        return rowNamesTable;
    }
    
    public IBindingContext getBindingContext() {
        return bindingContext;
    }
    
    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }
    
    public IResultBuilder getResultBuilder(Spreadsheet spreadsheet) {
        IResultBuilder resultBuilder = null;
        try {
            resultBuilder = getResultBuilderInternal(spreadsheet);
        } catch (SyntaxNodeException e) {
            tableSyntaxNode.addError(e);
            BindHelper.processError(e, bindingContext); 
        }
        return resultBuilder;
    }
    
    private String[] extractRowNames() {   
        int height = rowNamesTable.getHeight();
        rowNames = new String[height];
        for (int row = 0; row < height; row++) {
            rowNames[row] = getRowName(row, rowNamesTable.getRow(row));
        }        
        return rowNames;        
    }
    
    private String[] extractColumnNames() {        
        int width = columnNamesTable.getWidth();  
        columnNames = new String[width];
        for (int col = 0; col < width; col++) {
            columnNames[col] = getColumnName(col, columnNamesTable.getColumn(col));
        }
        return columnNames;
    }
    
    private String getRowName(int row, ILogicalTable rowNameCell) {
        IGridTable nameCell = rowNameCell.getColumn(0).getSource();
        String value = nameCell.getCell(0, 0).getStringValue();

        if (value != null) {
            String shortName = String.format("srow%d", row);
            StringValue sv = new StringValue(value, shortName, null, new GridCellSourceCodeModule(nameCell,
                bindingContext));

            addRowHeader(row, sv);
        }
        return value;
    }
    
    private String getColumnName(int column, ILogicalTable columnNameCell) {
        IGridTable nameCell = columnNameCell.getRow(0).getSource();
        String value = nameCell.getCell(0, 0).getStringValue();

        if (value != null) {
            String shortName = String.format("scol%d", column);
            StringValue stringValue = new StringValue(value, shortName, null, new GridCellSourceCodeModule(nameCell,
                bindingContext));

            addColumnHeader(column, stringValue);
        }

        return value;
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
    
    private void parseHeader(SpreadsheetHeaderDefinition header, StringValue value) {
        try {
            SymbolicTypeDefinition parsed = parseHeaderElement(value);
            String headerName = parsed.getName().getIdentifier();

            SpreadsheetHeaderDefinition h1 = headerDefinitions.get(headerName);

            if (h1 != null) {
                throw new DuplicatedVarException(null, headerName);
            } else {
                headerDefinitions.put(headerName, header);
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
    
    public void buildHeaderDefinitionsTypes() {
        for (SpreadsheetHeaderDefinition headerDefinition : headerDefinitions.values()) {

            IOpenClass headerType = null;

            for (SymbolicTypeDefinition symbolicTypeDefinition : headerDefinition.getVars()) {

                if (symbolicTypeDefinition.getType() != null) {

                    SyntaxNodeException error = null;
                    
                    String typeIdentifier = symbolicTypeDefinition.getType().getIdentifier(); 
                    
                    IOpenClass type = findType(typeIdentifier);

                    if (type == null) {
                        // error case, can`t find type.
                        //
                        String message = "Type not found: " + typeIdentifier;
                        error = SyntaxNodeExceptionUtils.createError(message, symbolicTypeDefinition.getType());
                    } else if (headerType == null) {
                        // initialize header type
                        //                        
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
    
    /**
     * Gets appropriate IOpenClass for given typeIdentifier.<br>
     * Supports array types.
     * 
     * @param typeIdentifier String type identifier (e.g. DoubleValue or Driver[], etc)
     * 
     * @return appropriate IOpenClass for given typeIdentifier
     */
    private IOpenClass findType(String typeIdentifier) {
        IOpenClass result = null;
        if (JavaClassGeneratorHelper.isArray(typeIdentifier)) {
            // gets the name of the type, remove square brackets for array type declaration.
            //
            String cleanTypeIdentifier = JavaClassGeneratorHelper.cleanTypeName(typeIdentifier);
            IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, cleanTypeIdentifier);
            int typeDimension = JavaClassGeneratorHelper.getDimension(typeIdentifier);
            result = type.getAggregateInfo().getIndexedAggregateType(type, typeDimension);
        } else {
            result = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeIdentifier);
        }        
        return result;
    }
    
    public void processReturnCells(IOpenClass spreadsheetHeaderType) throws SyntaxNodeException {

        SpreadsheetHeaderDefinition headerDefinition = headerDefinitions.get(RETURN_NAME);

        if (headerDefinition == null) {
            return;
        }

        int nonEmptyCellsCount = calculateNonEmptyCells(headerDefinition);

        IOpenClass cellType = deriveSingleCellReturnType(nonEmptyCellsCount, headerDefinition, spreadsheetHeaderType);

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
    private IOpenClass deriveSingleCellReturnType(int cellsCount, SpreadsheetHeaderDefinition headerDefinition, IOpenClass spreadsheetHeaderType)
            throws SyntaxNodeException {

        IOpenClass returnType = spreadsheetHeaderType;

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
    
    private boolean isExistsReturnHeader() {
        return returnHeaderDefinition != null;
    }
    
    private IResultBuilder getResultBuilderInternal(Spreadsheet spreadsheet) throws SyntaxNodeException {
        IResultBuilder resultBuilder = null;
        
        SymbolicTypeDefinition symbolicTypeDefinition = null;
        
        if (isExistsReturnHeader()) {
            symbolicTypeDefinition = returnHeaderDefinition.findVarDef(SpreadsheetSourceExtractor.RETURN_NAME);
        }
        
        if (spreadsheet.getHeader().getType() == JavaOpenClass.VOID) {
            throw SyntaxNodeExceptionUtils.createError("Spreadsheet can not return 'void' type", tableSyntaxNode);
        }
        
        if (spreadsheet.getHeader().getType() == JavaOpenClass.getOpenClass(SpreadsheetResult.class)) {
            if (isExistsReturnHeader()) {
                throw SyntaxNodeExceptionUtils.createError(
                        "If Spreadsheet return type is SpreadsheetResult, no return type is allowed",
                        symbolicTypeDefinition.getName());
            }

            resultBuilder = new DefaultResultBuilder();
        } else {
            // real return type
            //
            if (!isExistsReturnHeader()) {
                throw SyntaxNodeExceptionUtils.createError("There should be RETURN row or column for this return type",
                    tableSyntaxNode);
            }            
            List<SpreadsheetCell> notEmptyReturnDefinitions = spreadsheet.listNonEmptyCells(returnHeaderDefinition);

            switch (notEmptyReturnDefinitions.size()) {
                case 0:
                    throw SyntaxNodeExceptionUtils.createError("There is no return expression cell",
                            symbolicTypeDefinition.getName());
                case 1:
                    resultBuilder = new ScalarResultBuilder(notEmptyReturnDefinitions);
                    break;
                default:
                    resultBuilder = new ArrayResultBuilder(notEmptyReturnDefinitions, returnHeaderDefinition.getType());
            }
        }
        return resultBuilder;
    }
}
