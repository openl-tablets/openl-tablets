package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.NodeType;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.StringValue;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.result.ArrayResultBuilder;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.calc.result.ScalarResultBuilder;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

/**
 * 
 * @author DLiauchuk
 * 
 * 
 */
public class SpreadsheetComponentsBuilder {
    
    /** tableSyntaxNode of the spreadsheet**/
    private TableSyntaxNode tableSyntaxNode;
    
    /** binding context for indicating execution mode**/
    private IBindingContext bindingContext;    
    
    private CellsHeaderExtractor cellsHeaderExtractor;
    
    private ReturnSpreadsheetHeaderDefinition returnHeaderDefinition;
    
    private Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    private Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    private Map<String, SpreadsheetHeaderDefinition> headerDefinitions = new HashMap<String, SpreadsheetHeaderDefinition>();
    
    public SpreadsheetComponentsBuilder(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext) {        
        this.tableSyntaxNode = tableSyntaxNode;
        CellsHeaderExtractor extractor = ((SpreadsheetHeaderNode)tableSyntaxNode.getHeader()).getCellHeadersExtractor();
        if (extractor == null) {
            extractor = new CellsHeaderExtractor(getSignature(tableSyntaxNode), tableSyntaxNode.getTableBody().getRow(0).getColumns(1), 
                tableSyntaxNode.getTableBody().getColumn(0).getRows(1));
        }
        this.cellsHeaderExtractor = extractor; 
        this.bindingContext = bindingContext;
    }
    
    public Map<Integer, SpreadsheetHeaderDefinition> getRowHeaders() {        
        return new HashMap<Integer, SpreadsheetHeaderDefinition>(rowHeaders);
    }
    
    public Map<Integer, SpreadsheetHeaderDefinition> getColumnHeaders() {        
        return new HashMap<Integer, SpreadsheetHeaderDefinition>(columnHeaders);
    }
    
    public String[] getRowNames() {
        return buildArrayForHeaders(rowHeaders, cellsHeaderExtractor.getHeight());
    }

    public String[] getColumnNames() {
        return buildArrayForHeaders(columnHeaders, cellsHeaderExtractor.getWidth());
    }

    private String[] buildArrayForHeaders(Map<Integer, SpreadsheetHeaderDefinition> headers, int size){
        String[] ret = new String[size];
        for (Entry<Integer, SpreadsheetHeaderDefinition> x : headers.entrySet()){
            int k = x.getKey();
            ret[k] = x.getValue().getFirstname();
        }
        return ret;
    }
        
    public CellsHeaderExtractor getCellsHeadersExtractor() {
        return cellsHeaderExtractor;
    }
    
    /**
     * Extract following data form the spreadsheet source table:
     * row names, column names, header definitions, return cell.
     */
    public void buildHeaders(IOpenClass spreadsheetHeaderType) {
        addRowHeaders();
        addColumnHeaders();
        buildHeaderDefinitionsTypes();
        
        try {            
            buildReturnCells(spreadsheetHeaderType);
        } catch (SyntaxNodeException e) {            
            getTableSyntaxNode().addError(e);
            getBindingContext().addError(e);
        }
    }
    
    public IBindingContext getBindingContext() {
        return bindingContext;
    }
    
    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }
    
    public IResultBuilder buildResultBuilder(Spreadsheet spreadsheet) {
        IResultBuilder resultBuilder = null;
        try {
            resultBuilder = getResultBuilderInternal(spreadsheet);
        } catch (SyntaxNodeException e) {
            tableSyntaxNode.addError(e);
            bindingContext.addError(e);
        }
        return resultBuilder;
    }
    
    private void addRowHeaders() {
        String[] rowNames = cellsHeaderExtractor.getRowNames();
        for (int i = 0; i < rowNames.length; i++) {
            StringValue rowName = cellsHeaderExtractor.getRowNameForHeader(rowNames[i], i, bindingContext);
            if (rowName != null) {                
                addRowHeader(i, rowName);
            }
        }
    }
    
    private void addColumnHeaders() {
        String[] columnNames = cellsHeaderExtractor.getColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            StringValue columnName = cellsHeaderExtractor.getColumnNameForHeader(columnNames[i], i, bindingContext);
            if (columnName != null) {                
                addColumnHeader(i, columnName);
            }
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
        } catch (SyntaxNodeException error) {
            tableSyntaxNode.addError(error);
            bindingContext.addError(error);
        } catch (Exception t) {
            SyntaxNodeException error;
            try {
                error = SyntaxNodeExceptionUtils.createError("Cannot parse spreadsheet header definition", t, parseHeaderElement(value).getName());
            } catch (SyntaxNodeException e) {
                error = SyntaxNodeExceptionUtils.createError("Cannot parse spreadsheet header definition", t, null, value.asSourceCodeModule());
            }

            tableSyntaxNode.addError(error);
            bindingContext.addError(error);
        }
    }
    
    private SymbolicTypeDefinition parseHeaderElement(StringValue value) throws SyntaxNodeException {
        IOpenSourceCodeModule source = value.asSourceCodeModule();
        IdentifierNode[] nodes;

        try {
            nodes = Tokenizer.tokenize(source, SpreadsheetSymbols.TYPE_DELIMETER.toString());
        } catch (OpenLCompilationException e) {
            throw SyntaxNodeExceptionUtils.createError("Cannot parse header", source);
        }

        switch (nodes.length) {
            case 1:
                return new SymbolicTypeDefinition(nodes[0], null);
            case 2:
                return new SymbolicTypeDefinition(nodes[0], nodes[1]);
            default:
                String message = String.format("Valid header format: name [%s type]", SpreadsheetSymbols.TYPE_DELIMETER.toString());
                if (nodes.length > 2) {
                    throw SyntaxNodeExceptionUtils.createError(message, nodes[2]);
                } else {
                    TextInterval location = LocationUtils.createTextInterval(value);
                    throw SyntaxNodeExceptionUtils.createError(message, null, location, source);
                }
        }
    }

    private void buildHeaderDefinitionsTypes() {
        for (SpreadsheetHeaderDefinition headerDefinition : headerDefinitions.values()) {

            IOpenClass headerType = null;
            IdentifierNode typeIdentifierNode = null;

            for (SymbolicTypeDefinition symbolicTypeDefinition : headerDefinition.getVars()) {

                typeIdentifierNode = symbolicTypeDefinition.getType();
                if (typeIdentifierNode != null) {
                    SyntaxNodeException error = null;

                    String typeIdentifier = typeIdentifierNode.getIdentifier();

                    IOpenClass type = null;
                    if (typeIdentifier.indexOf('[') > 0) {
                        // gets the name of the type, remove square brackets for array type declaration.
                        //
                        String cleanTypeIdentifier = typeIdentifier.substring(0, typeIdentifier.indexOf("["));

                        IOpenClass componentType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, cleanTypeIdentifier);

                        if (componentType != null) {
                            // count of []
                            int typeDimension = typeIdentifier.split("\\[").length - 1;
                            type = componentType.getAggregateInfo().getIndexedAggregateType(componentType, typeDimension);
                        }

                    } else {
                        type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeIdentifier);
                    }

                    if (type == null) {
                        // error case, can`t find type.
                        //
                        String message = "Type is not found: " + typeIdentifier;
                        error = SyntaxNodeExceptionUtils.createError(message, typeIdentifierNode);
                    } else if (headerType == null) {
                        // initialize header type
                        //                        
                        headerType = type;
                    } else if (headerType != type) {
                        error = SyntaxNodeExceptionUtils.createError("Type redefinition", typeIdentifierNode);
                    }
                    if (error != null) {
                        tableSyntaxNode.addError(error);
                        bindingContext.addError(error);
                    }
                }
            }

            if (headerType != null) {
                headerDefinition.setType(headerType);
                if (!bindingContext.isExecutionMode() && typeIdentifierNode != null) {
                    ILogicalTable cell;
                    IOpenClass type = headerType;
                    while (type.getMetaInfo() == null && type.isArray()) {
                        type = type.getComponentClass();
                    }
                    IdentifierNode identifier = cutTypeIdentifier(typeIdentifierNode);

                    if (identifier != null) {
                        if (headerDefinition.getRow() >= 0) {
                            cell = cellsHeaderExtractor.getRowNamesTable().getRow(headerDefinition.getRow());
                        } else {
                            cell = cellsHeaderExtractor.getColumnNamesTable().getColumn(headerDefinition.getColumn());
                        }

                        RuleRowHelper.setCellMetaInfoWithNodeUsage(cell, identifier, type.getMetaInfo(),
                                NodeType.DATATYPE);
                    }
                }
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
                    SpreadsheetSymbols.TYPE_DELIMETER.toString());
            if (variableAndType.length > 1) {
                IdentifierNode[] nodes = Tokenizer.tokenize(typeIdentifierNode.getModule(),
                        " []\n\r", variableAndType[1].getLocation());
                if (nodes.length > 0) {
                    return nodes[0];
                }
            }
        } catch (OpenLCompilationException e) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Cannot parse header",
                    typeIdentifierNode);
            getTableSyntaxNode().addError(error);
            getBindingContext().addError(error);
        }

        return null;
    }

    private void buildReturnCells(IOpenClass spreadsheetHeaderType) throws SyntaxNodeException {

        SpreadsheetHeaderDefinition headerDefinition = headerDefinitions.get(SpreadsheetSymbols.RETURN_NAME.toString());

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

        returnHeaderDefinition = new ReturnSpreadsheetHeaderDefinition(headerDefinition, spreadsheetHeaderType);
        headerDefinitions.put(SpreadsheetSymbols.RETURN_NAME.toString(), returnHeaderDefinition);
    }
    
    private int calculateNonEmptyCells(SpreadsheetHeaderDefinition headerDefinition) {

        int fromRow = 0;
        int toRow = cellsHeaderExtractor.getHeight();

        int fromColumn = 0;
        int toColumn = cellsHeaderExtractor.getWidth();

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

                ILogicalTable cell = LogicalTableHelper.mergeBounds(cellsHeaderExtractor.getRowNamesTable().getRow(rowIndex),
                    cellsHeaderExtractor.getColumnNamesTable().getColumn(columnIndex));

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
     * @param cellsCount spreadsheet cells count
     * @param headerDefinition spreadsheet header
     * @return the type that should be in the cell that is located in RETURN row
     *         or column
     * 
     * Right now we allow only to return types = scalars or arrays.
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
                        SpreadsheetSymbols.RETURN_NAME.toString()).getName());
        }

        return returnType;
    }
    
    private boolean isExistsReturnHeader() {
        return returnHeaderDefinition != null;
    }
    
    private IResultBuilder getResultBuilderInternal(Spreadsheet spreadsheet) throws SyntaxNodeException {
        IResultBuilder resultBuilder;
        
        SymbolicTypeDefinition symbolicTypeDefinition = null;
        
        if (isExistsReturnHeader()) {
            symbolicTypeDefinition = returnHeaderDefinition.findVarDef(SpreadsheetSymbols.RETURN_NAME.toString());
        }
        
        if (spreadsheet.getHeader().getType() == JavaOpenClass.VOID) {
            throw SyntaxNodeExceptionUtils.createError("Spreadsheet can not return 'void' type", tableSyntaxNode);
        }
        
        if (spreadsheet.getHeader().getType() == JavaOpenClass.getOpenClass(SpreadsheetResult.class)) {
            if (isExistsReturnHeader()) {
                throw SyntaxNodeExceptionUtils.createError(
                        "If Spreadsheet return type is SpreadsheetResult, no return type is allowed",
                        symbolicTypeDefinition == null ? null : symbolicTypeDefinition.getName());
            }

            resultBuilder = new DefaultResultBuilder();
        } else {
            // real return type
            //
            if (!isExistsReturnHeader()) {
                throw SyntaxNodeExceptionUtils.createError("There should be RETURN row or column for this return type",
                    tableSyntaxNode);
            }            
            List<SpreadsheetCell> notEmptyReturnDefinitions = listNonEmptyCells(spreadsheet.getCells(), returnHeaderDefinition);

            switch (notEmptyReturnDefinitions.size()) {
                case 0:
                    throw SyntaxNodeExceptionUtils.createError("There is no return expression cell",
                            symbolicTypeDefinition == null ? null : symbolicTypeDefinition.getName());
                case 1:
                    resultBuilder = new ScalarResultBuilder(notEmptyReturnDefinitions);
                    break;
                default:
                    resultBuilder = new ArrayResultBuilder(notEmptyReturnDefinitions, returnHeaderDefinition.getReturnType());
            }
        }
        return resultBuilder;
    }

    private static List<SpreadsheetCell> listNonEmptyCells(SpreadsheetCell[][] cells, SpreadsheetHeaderDefinition definition) {

        List<SpreadsheetCell> list = new ArrayList<SpreadsheetCell>();

        int row = definition.getRow();
        int col = definition.getColumn();

        if (row >= 0) {
            for (int i = 0; i < cells[0].length; ++i) {
                if (!(cells[row][i].isEmpty())) {
                    list.add(cells[row][i]);
                }
            }
        } else {
            for (int i = 0; i < cells.length; ++i) {
                if (!(cells[i][col].isEmpty())) {
                    list.add(cells[i][col]);
                }
            }
        }

        return list;
    }

    private String getSignature(TableSyntaxNode table) {
        return table.getHeader().getHeaderToken().getModule().getCode();
    }
}
