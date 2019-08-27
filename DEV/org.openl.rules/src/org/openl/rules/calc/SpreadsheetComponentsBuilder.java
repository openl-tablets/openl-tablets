package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.ArrayResultBuilder;
import org.openl.rules.calc.result.DefaultResultBuilder;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.calc.result.ScalarResultBuilder;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.JavaKeywordUtils;
import org.openl.util.text.AbsolutePosition;
import org.openl.util.text.ILocation;
import org.openl.util.text.IPosition;
import org.openl.util.text.TextInfo;
import org.openl.util.text.TextInterval;

import gcardone.junidecode.Junidecode;

/**
 *
 * @author DLiauchuk
 *
 *
 */
public class SpreadsheetComponentsBuilder {

    /** tableSyntaxNode of the spreadsheet **/
    private TableSyntaxNode tableSyntaxNode;

    /** binding context for indicating execution mode **/
    private IBindingContext bindingContext;

    private CellsHeaderExtractor cellsHeaderExtractor;

    private ReturnSpreadsheetHeaderDefinition returnHeaderDefinition;

    private Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = new HashMap<>();
    private Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = new HashMap<>();
    private BidiMap<String, SpreadsheetHeaderDefinition> headerDefinitions = new DualHashBidiMap<>();

    public SpreadsheetComponentsBuilder(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext) {
        this.tableSyntaxNode = tableSyntaxNode;
        CellsHeaderExtractor extractor = ((SpreadsheetHeaderNode) tableSyntaxNode.getHeader())
            .getCellHeadersExtractor();
        if (extractor == null) {
            extractor = new CellsHeaderExtractor(getSignature(tableSyntaxNode),
                tableSyntaxNode.getTableBody().getRow(0).getColumns(1),
                tableSyntaxNode.getTableBody().getColumn(0).getRows(1));
        }
        this.cellsHeaderExtractor = extractor;
        this.bindingContext = bindingContext;
    }

    public Map<Integer, SpreadsheetHeaderDefinition> getRowHeaders() {
        return rowHeaders;
    }

    public Map<Integer, SpreadsheetHeaderDefinition> getColumnHeaders() {
        return columnHeaders;
    }

    private static String handleWrongSymbols(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s != null && s.length() > 0) {
            s = Junidecode.unidecode(s);
            if (JavaKeywordUtils.isJavaKeyword(s) || Character.isDigit(s.charAt(0))) {
                s = "_" + s;
            }
            s = s.replaceAll("\\s+", "_"); // Replace whitespaces
            s = s.replaceAll("[^0-9a-zA-Z_]+", "");
        }
        return s;
    }

    public String[] getRowNamesMarkedWithAsterisk() {
        final long rowsMarkedWithAsteriskCount = rowHeaders.entrySet()
            .stream()
            .filter(e -> e.getValue().getDefinition().isMarkedWithAsterisk())
            .count();

        String[] ret = buildArrayForHeaders(rowHeaders,
            cellsHeaderExtractor.getHeight(),
            e -> rowsMarkedWithAsteriskCount == 0 || e.getDefinition().isMarkedWithAsterisk());

        for (int i = 0; i < ret.length; i++) {
            ret[i] = handleWrongSymbols(ret[i]);
        }
        return ret;
    }

    public String[] getColumnNamesMarkedWithAsterisk() {
        final long columnsMarkedWithAsterisk = columnHeaders.entrySet()
            .stream()
            .filter(e -> e.getValue().getDefinition().isMarkedWithAsterisk())
            .count();

        String[] ret = buildArrayForHeaders(columnHeaders,
            cellsHeaderExtractor.getWidth(),
            e -> columnsMarkedWithAsterisk == 0 || e.getDefinition().isMarkedWithAsterisk());

        for (int i = 0; i < ret.length; i++) {
            ret[i] = handleWrongSymbols(ret[i]);
        }

        return ret;
    }

    public String[] getRowNames() {
        return buildArrayForHeaders(rowHeaders, cellsHeaderExtractor.getHeight(), e -> true);
    }

    public String[] getColumnNames() {
        return buildArrayForHeaders(columnHeaders, cellsHeaderExtractor.getWidth(), e -> true);
    }

    private String[] buildArrayForHeaders(Map<Integer, SpreadsheetHeaderDefinition> headers,
            int size,
            Predicate<SpreadsheetHeaderDefinition> predicate) {
        String[] ret = new String[size];
        for (Entry<Integer, SpreadsheetHeaderDefinition> x : headers.entrySet()) {
            int k = x.getKey();
            if (predicate.test(x.getValue())) {
                ret[k] = x.getValue().getDefinitionName();
            }
        }
        return ret;
    }

    public CellsHeaderExtractor getCellsHeadersExtractor() {
        return cellsHeaderExtractor;
    }

    /**
     * Extract following data form the spreadsheet source table: row names, column names, header definitions, return
     * cell.
     */
    public void buildHeaders(IOpenClass spreadsheetHeaderType) {
        addRowHeaders();
        addColumnHeaders();
        buildHeaderDefinitions();
        buildReturnCells(spreadsheetHeaderType);
    }

    void addError(SyntaxNodeException e) {
        getTableSyntaxNode().addError(e);
        getBindingContext().addError(e);
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
            addError(e);
        }
        return resultBuilder;
    }

    private void addRowHeaders() {
        String[] rowNames = cellsHeaderExtractor.getRowNames();
        for (int i = 0; i < rowNames.length; i++) {
            if (rowNames[i] != null) {
                IGridTable rowNameForHeader = cellsHeaderExtractor.getRowNamesTable()
                    .getRow(i)
                    .getColumn(0)
                    .getSource();
                IOpenSourceCodeModule source = new GridCellSourceCodeModule(rowNameForHeader, bindingContext);
                parseHeader(source, i, true);
            }
        }
    }

    private void addColumnHeaders() {
        String[] columnNames = cellsHeaderExtractor.getColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i] != null) {
                IGridTable columnNameForHeader = cellsHeaderExtractor.getColumnNamesTable()
                    .getColumn(i)
                    .getRow(0)
                    .getSource();
                GridCellSourceCodeModule source = new GridCellSourceCodeModule(columnNameForHeader, bindingContext);
                parseHeader(source, i, false);
            }
        }
    }

    private void parseHeader(IOpenSourceCodeModule source, int index, boolean row) {
        try {
            SymbolicTypeDefinition parsed = parseHeaderElement(source);
            IdentifierNode name = parsed.getName();
            String headerName = name.getIdentifier();

            SpreadsheetHeaderDefinition h1 = headerDefinitions.get(headerName);
            if (h1 != null) {
                SyntaxNodeException error;
                error = SyntaxNodeExceptionUtils.createError("The header definition is duplicated", name);

                addError(error);
                throw new DuplicatedVarException(null, headerName);
            } else {
                SpreadsheetHeaderDefinition header;
                if (row) {
                    header = rowHeaders.computeIfAbsent(index, e -> new SpreadsheetHeaderDefinition(parsed, index, -1));
                } else {
                    header = columnHeaders.computeIfAbsent(index,
                        e -> new SpreadsheetHeaderDefinition(parsed, -1, index));
                }
                headerDefinitions.put(headerName, header);
            }
        } catch (SyntaxNodeException error) {
            addError(error);
        }
    }

    private IdentifierNode removeAsteriskInTheEnd(IdentifierNode identifierNode) {
        String s = StringUtils.stripEnd(identifierNode.getIdentifier(), null);
        int d = s.lastIndexOf(SpreadsheetSymbols.ASTERISK.toString());
        if (d != s.length() - 1) {
            throw new IllegalStateException("Asterisk symbols is not found!");
        }
        String v = StringUtils.stripEnd(identifierNode.getIdentifier().substring(0, d), null);
        int delta = identifierNode.getIdentifier().length() - v.length();

        IPosition end = new AbsolutePosition(-delta + identifierNode.getLocation()
            .getEnd()
            .getAbsolutePosition(new TextInfo(identifierNode.getIdentifier())));
        ILocation location = new TextInterval(identifierNode.getLocation().getStart(), end);
        return new IdentifierNode(identifierNode.getType(), location, v, identifierNode.getModule());
    }

    private SymbolicTypeDefinition parseHeaderElement(IOpenSourceCodeModule source) throws SyntaxNodeException {
        IdentifierNode[] nodes;

        try {
            nodes = Tokenizer.tokenize(source, SpreadsheetSymbols.TYPE_DELIMETER.toString());
        } catch (OpenLCompilationException e) {
            throw SyntaxNodeExceptionUtils.createError("Cannot parse header", source);
        }

        IdentifierNode headerNameNode = nodes[0];
        boolean endsWithAsterisk = false;
        if ((nodes.length == 1 || nodes.length == 2) && nodes[0].getIdentifier()
            .endsWith(SpreadsheetSymbols.ASTERISK.toString())) {
            headerNameNode = removeAsteriskInTheEnd(nodes[0]);
            endsWithAsterisk = true;
        }
        switch (nodes.length) {
            case 1:
                return new SymbolicTypeDefinition(headerNameNode, null, endsWithAsterisk);
            case 2:
                return new SymbolicTypeDefinition(headerNameNode, nodes[1], endsWithAsterisk);
            default:
                String message = String.format("Valid header format: name [%s type]",
                    SpreadsheetSymbols.TYPE_DELIMETER.toString());
                if (nodes.length > 2) {
                    throw SyntaxNodeExceptionUtils.createError(message, nodes[2]);
                } else {
                    throw SyntaxNodeExceptionUtils.createError(message, source);
                }
        }
    }

    private void buildHeaderDefinitions() {
        for (SpreadsheetHeaderDefinition headerDefinition : headerDefinitions.values()) {

            IOpenClass headerType = null;
            IdentifierNode typeIdentifierNode = null;

            SymbolicTypeDefinition symbolicTypeDefinition = headerDefinition.getDefinition();
            typeIdentifierNode = symbolicTypeDefinition.getType();
            if (typeIdentifierNode != null) {
                String typeIdentifier = typeIdentifierNode.getText();
                try {
                    IOpenClass type = RuleRowHelper.getType(typeIdentifier, typeIdentifierNode, bindingContext);
                    headerType = type;
                } catch (SyntaxNodeException e) {
                    addError(e);
                }
            }

            if (headerType != null) {
                headerDefinition.setType(headerType);
            }

            if (!bindingContext.isExecutionMode() && getTableSyntaxNode()
                .getMetaInfoReader() instanceof SpreadsheetMetaInfoReader) {
                SpreadsheetMetaInfoReader metaInfoReader = (SpreadsheetMetaInfoReader) getTableSyntaxNode()
                    .getMetaInfoReader();
                List<NodeUsage> nodeUsages = new ArrayList<>();
                if (headerType != null && typeIdentifierNode != null) {
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
                                NodeType.DATATYPE);
                            nodeUsages.add(nodeUsage);
                        }
                    }
                }
                ILogicalTable cell;
                if (headerDefinition.getRow() >= 0) {
                    cell = cellsHeaderExtractor.getRowNamesTable().getRow(headerDefinition.getRow());
                } else {
                    cell = cellsHeaderExtractor.getColumnNamesTable().getColumn(headerDefinition.getColumn());
                }
                if (headerDefinition.getDefinition().isMarkedWithAsterisk()) {
                    String s = handleWrongSymbols(headerDefinition.getDefinitionName());
                    if (StringUtils.isEmpty(s)) {
                        s = "Empty string";
                    }
                    String stringValue = cell.getCell(0, 0).getStringValue();
                    int d = stringValue.lastIndexOf(SpreadsheetSymbols.ASTERISK.toString());
                    SimpleNodeUsage nodeUsage = new SimpleNodeUsage(d, d, s, null, NodeType.OTHER);
                    nodeUsages.add(nodeUsage);
                }
                if (!nodeUsages.isEmpty()) {
                    CellMetaInfo cellMetaInfo = new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
                    ICell c = cell.getCell(0, 0);
                    metaInfoReader.addHeaderMetaInfo(c.getAbsoluteRow(), c.getAbsoluteColumn(), cellMetaInfo);
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
                IdentifierNode[] nodes = Tokenizer
                    .tokenize(typeIdentifierNode.getModule(), " []\n\r", variableAndType[1].getLocation());
                if (nodes.length > 0) {
                    return nodes[0];
                }
            }
        } catch (OpenLCompilationException e) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Cannot parse header", typeIdentifierNode);
            addError(error);
        }

        return null;
    }

    private void buildReturnCells(IOpenClass spreadsheetHeaderType) {
        SpreadsheetHeaderDefinition headerDefinition = headerDefinitions.get(SpreadsheetSymbols.RETURN_NAME.toString());

        if (spreadsheetHeaderType
            .equals(JavaOpenClass.getOpenClass(SpreadsheetResult.class)) && headerDefinition == null) {
            return;
        }

        if (headerDefinition == null) {
            for (SpreadsheetHeaderDefinition spreadsheetHeaderDefinition : headerDefinitions.values()) {
                if (headerDefinition == null) {
                    headerDefinition = spreadsheetHeaderDefinition;
                } else if (headerDefinition.getRow() < spreadsheetHeaderDefinition.getRow()) {
                    headerDefinition = spreadsheetHeaderDefinition;
                }
            }
        }

        if (Boolean.FALSE
            .equals(tableSyntaxNode.getTableProperties().getAutoType()) && headerDefinition.getType() == null) {
            headerDefinition.setType(spreadsheetHeaderType);
        } else if (spreadsheetHeaderType
            .getAggregateInfo() == null || (spreadsheetHeaderType.getAggregateInfo() != null && spreadsheetHeaderType
                .getAggregateInfo()
                .getComponentType(spreadsheetHeaderType) == null)) {
            int nonEmptyCellsCount = getNonEmptyCellsCount(headerDefinition);
            if (nonEmptyCellsCount == 1) {
                headerDefinition.setType(spreadsheetHeaderType);
            }
        }

        String key = headerDefinitions.getKey(headerDefinition);
        returnHeaderDefinition = new ReturnSpreadsheetHeaderDefinition(headerDefinition);
        headerDefinitions.replace(key, returnHeaderDefinition);
    }

    private int getNonEmptyCellsCount(SpreadsheetHeaderDefinition headerDefinition) {
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

                ILogicalTable cell = LogicalTableHelper.mergeBounds(
                    cellsHeaderExtractor.getRowNamesTable().getRow(rowIndex),
                    cellsHeaderExtractor.getColumnNamesTable().getColumn(columnIndex));
                String value = cell.getSource().getCell(0, 0).getStringValue();
                if (!StringUtils.isBlank(value)) {
                    nonEmptyCellsCount += 1;
                }
            }
        }
        return nonEmptyCellsCount;
    }

    public boolean isExistsReturnHeader() {
        return returnHeaderDefinition != null;
    }

    public ReturnSpreadsheetHeaderDefinition getReturnHeaderDefinition() {
        return returnHeaderDefinition;
    }

    private IResultBuilder getResultBuilderInternal(Spreadsheet spreadsheet) throws SyntaxNodeException {
        IResultBuilder resultBuilder;

        SymbolicTypeDefinition symbolicTypeDefinition = null;

        if (isExistsReturnHeader()) {
            String key = headerDefinitions.getKey(returnHeaderDefinition);
            symbolicTypeDefinition = returnHeaderDefinition.findDefinition(key);
        }

        if (!isExistsReturnHeader() && spreadsheet.getHeader()
            .getType()
            .equals(JavaOpenClass.getOpenClass(SpreadsheetResult.class))) {
            resultBuilder = new DefaultResultBuilder();
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
            boolean byColumn = true;
            if (n < 0) {
                n = returnHeaderDefinition.getColumn();
                byColumn = false;
                for (int i = 0; i < spreadsheet.getCells().length; i++) {
                    sprCells.add(spreadsheet.getCells()[i][n]);
                }
            } else {
                for (int i = 0; i < spreadsheet.getCells()[n].length; i++) {
                    sprCells.add(spreadsheet.getCells()[n][i]);
                }
            }

            List<SpreadsheetCell> nonEmptySpreadsheetCells = new ArrayList<>();
            for (SpreadsheetCell cell : sprCells) {
                if (!cell.isEmpty()) {
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

            SpreadsheetCell[] retCells = returnSpreadsheetCells.toArray(new SpreadsheetCell[] {});
            if (!returnSpreadsheetCells.isEmpty()) {
                if (asArray) {
                    returnHeaderDefinition.setReturnCells(byColumn, retCells);
                } else {
                    returnHeaderDefinition.setReturnCells(byColumn,
                        returnSpreadsheetCells.get(returnSpreadsheetCells.size() - 1));
                }
            } else {
                if (!nonEmptySpreadsheetCells.isEmpty()) {
                    if (asArray) {
                        returnHeaderDefinition.setReturnCells(byColumn,
                            nonEmptySpreadsheetCells.toArray(new SpreadsheetCell[] {}));
                    } else {
                        returnHeaderDefinition.setReturnCells(byColumn,
                            nonEmptySpreadsheetCells.get(nonEmptySpreadsheetCells.size() - 1));
                    }
                }
            }

            switch (returnSpreadsheetCells.size()) {
                case 0:
                    if (!nonEmptySpreadsheetCells.isEmpty()) {
                        SpreadsheetCell nonEmptySpreadsheetCell = nonEmptySpreadsheetCells
                            .get(nonEmptySpreadsheetCells.size() - 1);
                        if (nonEmptySpreadsheetCell.getType() != null) {
                            throw SyntaxNodeExceptionUtils.createError(
                                "Can not convert from " + nonEmptySpreadsheetCell.getType()
                                    .getName() + " to " + spreadsheet.getHeader().getType().getName(),
                                symbolicTypeDefinition == null ? null : symbolicTypeDefinition.getName());
                        } else {
                            return null;
                        }
                    } else {
                        throw SyntaxNodeExceptionUtils.createError("There is no return expression cell",
                            symbolicTypeDefinition == null ? null : symbolicTypeDefinition.getName());

                    }
                case 1:
                    resultBuilder = new ScalarResultBuilder(
                        returnSpreadsheetCells.get(returnSpreadsheetCells.size() - 1),
                        casts.get(casts.size() - 1),
                        isCalculateAllCellsInSpreadsheet(spreadsheet));
                    break;
                default:
                    if (asArray) {
                        resultBuilder = new ArrayResultBuilder(retCells,
                            castsAsArray.toArray(new IOpenCast[] {}),
                            type,
                            isCalculateAllCellsInSpreadsheet(spreadsheet));
                    } else {
                        resultBuilder = new ScalarResultBuilder(
                            returnSpreadsheetCells.get(returnSpreadsheetCells.size() - 1),
                            casts.get(casts.size() - 1),
                            isCalculateAllCellsInSpreadsheet(spreadsheet));
                    }
            }
        }
        return resultBuilder;
    }

    private boolean isCalculateAllCellsInSpreadsheet(Spreadsheet spreadsheet) {
        return !Boolean.FALSE.equals(spreadsheet.getMethodProperties().getCalculateAllCells());
    }

    private String getSignature(TableSyntaxNode table) {
        return table.getHeader().getHeaderToken().getModule().getCode();
    }
}
