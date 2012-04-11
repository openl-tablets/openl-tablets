package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.IOpenSourceCodeModule;
import org.openl.base.INamedThing;
import org.openl.binding.DuplicatedVarException;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaInfo;
import org.openl.meta.StringValue;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

public class SpreadsheetBuilder {

    static class SymbolicTypeDef {
        IdentifierNode name;
        IdentifierNode type;

        SymbolicTypeDef(IdentifierNode name, IdentifierNode type) {
            super();
            this.name = name;
            this.type = type;
        }
    }
    private static final String RETURN_NAME = "RETURN";
    private static final IResultBuilder DEFAULT_RESULT_BULDER = new IResultBuilder() {

        public Object makeResult(SpreadsheetResult res) {
            return res;
        }
    };
    static final IOpenClass DEFAULT_CELL_TYPE = JavaOpenClass.getOpenClass(AnyCellValue.class);
    IBindingContext cxt;

    Spreadsheet spreadsheet;
    TableSyntaxNode tsn;

    Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();
    Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = new HashMap<Integer, SpreadsheetHeaderDefinition>();

    Map<String, SpreadsheetHeaderDefinition> varDefinitions = new HashMap<String, SpreadsheetHeaderDefinition>();

    private SpreadsheetHeaderDefinition returnHeaderDefinition;

    List<ISyntaxError> errors = new ArrayList<ISyntaxError>();

    ILogicalTable rowNamesTable;

    ILogicalTable columnNamesTable;

    Map<Integer, IBindingContext> rowContexts = new HashMap<Integer, IBindingContext>();

    Map<Integer, IBindingContextDelegator> colContexts = new HashMap<Integer, IBindingContextDelegator>();

    List<SCell> formulaCells = new ArrayList<SCell>();

    public SpreadsheetBuilder(IBindingContext cxt, Spreadsheet spreadsheet, TableSyntaxNode tsn) {
        this.cxt = cxt;
        this.spreadsheet = spreadsheet;
        this.tsn = tsn;
    }

    public void addColumnHeader(int col, StringValue sv, int cxtLevel) {
        SpreadsheetHeaderDefinition h = columnHeaders.get(col);
        if (h == null) {
            h = new SpreadsheetHeaderDefinition(-1, col);
            columnHeaders.put(col, h);
        }
        parseHeader(h, sv);

    }

    private void addColumnNames(int col, ILogicalTable logicalCol) {
        for (int i = 0; i < logicalCol.getLogicalHeight(); i++) {
            IGridTable nameCell = logicalCol.getLogicalRow(i).getGridTable();
            String value = nameCell.getStringValue(0, 0);
            if (value != null) {
                String shortName = "scol" + col + "_" + i;
                StringValue sv = new StringValue(value, shortName, null, nameCell.getUri());
                addColumnHeader(col, sv, i);
            }
            // FIXME
            spreadsheet.colNames[col] = value;
        }
    }

    public void addRowHeader(int row, StringValue sv, int cxtLevel) {
        SpreadsheetHeaderDefinition h = rowHeaders.get(row);
        if (h == null) {
            h = new SpreadsheetHeaderDefinition(row, -1);
            rowHeaders.put(row, h);
        }

        parseHeader(h, sv);
    }

    private void addRowNames(int row, ILogicalTable logicalRow) {
        for (int i = 0; i < logicalRow.getLogicalWidth(); i++) {
            IGridTable nameCell = logicalRow.getLogicalColumn(i).getGridTable();
            String value = nameCell.getStringValue(0, 0);
            if (value != null) {
                String shortName = "srow" + row + "_" + i;
                StringValue sv = new StringValue(value, shortName, null, nameCell.getUri());
                addRowHeader(row, sv, i);
            }
            // FIXME
            spreadsheet.rowNames[row] = value;
        }
    }

    public void build(ILogicalTable tableBody) {

        buildColumnRowNames(tableBody);

        buildColRowHeaderTypes();

        try {
            processReturnCells();
        } catch (BoundError e) {
            errors.add(e);
        }

        buildCells();

        if (errors.size() == 0) {
            checkError(buildReturn());
        }

        for (ISyntaxError e : errors) {

            tsn.addError(e);
            cxt.addError(e);
        }
    }

    private void buildCells() {
        // ILogicalTable cellTable = LogicalTable.mergeBounds(rowNamesTable,
        // columnNamesTable);

        int h = rowNamesTable.getLogicalHeight();
        int w = columnNamesTable.getLogicalWidth();

        SpreadsheetType stype = spreadsheet.getSpreadsheetType();
        ModuleBindingContext scxt = new ModuleBindingContext(cxt, stype);

        SCell[][] cells = new SCell[h][w];

        spreadsheet.setCells(cells);

        for (int row = 0; row < h; row++) {

            for (int col = 0; col < w; col++) {
                SCell scell = new SCell(row, col);
                cells[row][col] = scell;
                ILogicalTable cell = LogicalTable.mergeBounds(rowNamesTable.getLogicalRow(row), columnNamesTable
                        .getLogicalColumn(col));
                IOpenSourceCodeModule src = new GridCellSourceCodeModule(cell.getGridTable());
                String srcStr = src.getCode();
                IOpenClass type = deriveCellType(scell, cell, columnHeaders.get(col), rowHeaders.get(row), srcStr);

                scell.setType(type);

                if (columnHeaders.get(col) == null || rowHeaders.get(row) == null) {
                    continue;
                }

                for (SymbolicTypeDef coldef : columnHeaders.get(col).getVars()) {

                    for (SymbolicTypeDef rowdef : rowHeaders.get(row).getVars())

                    {
                        String fieldname = "$" + coldef.name.getIdentifier() + "$" + rowdef.name.getIdentifier();
                        stype.addField(new SCellField(stype, fieldname, scell));
                        // System.out.println("$"+coldef.name.getIdentifier() +
                        // "$"+rowdef.name.getIdentifier());
                    }
                }
            }
        }

        for (int row = 0; row < h; row++) {
            IBindingContext rowCxt = getRowContext(row, scxt);

            for (int col = 0; col < w; col++) {
                if (columnHeaders.get(col) == null || rowHeaders.get(row) == null) {
                    continue;
                }

                IBindingContext colCxt = getColContext(col, rowCxt);

                ILogicalTable cell = LogicalTable.mergeBounds(rowNamesTable.getLogicalRow(row), columnNamesTable
                        .getLogicalColumn(col));

                SCell scell = cells[row][col];
                IOpenSourceCodeModule src = new GridCellSourceCodeModule(cell.getGridTable());
                String srcStr = src.getCode();

                if (CellLoader.isFormula(srcStr)) {
                    formulaCells.add(scell);
                }

                String name = "$" + columnHeaders.get(col).getFirstname() + '$' + rowHeaders.get(row).getFirstname();

                IMetaInfo meta = new SpreadsheetCellMetaInfo(name, src);
                CellLoader loader = new CellLoader(colCxt, makeHeader(meta.getDisplayName(INamedThing.SHORT),
                        spreadsheet.getHeader(), scell.getType()), makeConvertor(scell.getType()));

                try {
                    Object cellvalue = loader.loadSingleParam(src, meta);
                    scell.setValue(cellvalue);
                } catch (BoundError e) {
                    tsn.addError(e);
                    cxt.addError(e);
                }

            }

        }
    }

    private void buildColRowHeaderTypes() {

        SpreadsheetType stype = new SpreadsheetType(null, spreadsheet.getName() + "Type", cxt.getOpenL());

        spreadsheet.setSpreadsheetType(stype);

        for (SpreadsheetHeaderDefinition h : varDefinitions.values()) {
            IOpenClass htype = null;
            for (SymbolicTypeDef sx : h.vars) {

                if (sx.type != null) {
                    BoundError b = null;
                    IOpenClass type = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, sx.type.getIdentifier());
                    if (type == null) {
                        b = new BoundError(sx.type, "Type not found: " + sx.type.getIdentifier());
                    } else if (htype == null) {
                        htype = type;
                    } else if (htype != type) {
                        b = new BoundError(sx.type, "Type redefinition");
                    }
                    if (b != null) {
                        tsn.addError(b);
                        cxt.addError(b);
                    }

                }
            }
            if (htype != null) {
                h.setType(htype);
            }

            // for(SymbolicTypeDef sx: h.vars)
            // {
            // stype.addField(new SpreadsheetHeaderField(stype, sx.name, h));
            // }

        }

    }
    protected void buildColumnRowNames(ILogicalTable tableBody) {
        rowNamesTable = tableBody.getLogicalColumn(0).rows(1);
        columnNamesTable = tableBody.getLogicalRow(0).columns(1);

        int h = rowNamesTable.getLogicalHeight();
        int w = columnNamesTable.getLogicalWidth();

        spreadsheet.setRowNames(new String[h]);
        spreadsheet.setColumnNames(new String[w]);

        for (int row = 0; row < h; row++) {
            addRowNames(row, rowNamesTable.getLogicalRow(row));
        }

        for (int col = 0; col < w; col++) {
            addColumnNames(col, columnNamesTable.getLogicalColumn(col));
        }

    }

    private BoundError buildReturn() {
        SymbolicTypeDef sdef = null;
        if (returnHeaderDefinition != null) {
            sdef = returnHeaderDefinition.findVarDef(RETURN_NAME);
        }

        if (spreadsheet.getHeader().getType() == JavaOpenClass.getOpenClass(SpreadsheetResult.class)) {
            if (returnHeaderDefinition != null) {
                return new BoundError(sdef.name,
                        "If Spreadsheet return type is SpreadsheetResult, no return type is allowed");
            }

            spreadsheet.setResultBuilder(DEFAULT_RESULT_BULDER);

        } else if (spreadsheet.getHeader().getType() == JavaOpenClass.VOID) {
            return new BoundError(tsn, "Spreadsheet can not return 'void' type");
        } else // real return type
        {
            if (returnHeaderDefinition == null) {
                return new BoundError(tsn, "There should be RETURN row or column for this return type");
            }

            List<SCell> notEmpty = spreadsheet.listNonEmptyCells(returnHeaderDefinition);

            switch (notEmpty.size()) {
                case 0:
                    return new BoundError(sdef.name, "There is no return expression cell");
                case 1:
                    spreadsheet.setResultBuilder(new ScalarResultBuilder(notEmpty));
                    break;
                default:
                    spreadsheet.setResultBuilder(new ArrayResultBuilder(notEmpty, returnHeaderDefinition.getType()));

            }

        }

        return null;

    }

    private int calculateNonEmptyCells(SpreadsheetHeaderDefinition sdef) {

        int fromRow = 0, toRow = rowNamesTable.getLogicalHeight();
        int fromCol = 0, toCol = columnNamesTable.getLogicalWidth();

        if (sdef.isRow()) {
            fromRow = sdef.row;
            toRow = fromRow + 1;
        } else {
            fromCol = sdef.column;
            toCol = fromCol + 1;
        }
        int cnt = 0;
        for (int col = fromCol; col < toCol; ++col) {
            for (int row = fromRow; row < toRow; ++row) {
                ILogicalTable cell = LogicalTable.mergeBounds(rowNamesTable.getLogicalRow(row), columnNamesTable
                        .getLogicalColumn(col));
                String str = cell.getGridTable().getStringValue(0, 0);
                if (str != null && str.trim().length() > 0) {
                    ++cnt;
                }
            }
        }

        return cnt;
    }

    void checkError(BoundError err) {
        if (err != null) {
            errors.add(err);
        }
    }

    private IOpenClass deriveCellType(SCell scell, ILogicalTable cell, SpreadsheetHeaderDefinition colHeader,
            SpreadsheetHeaderDefinition rowHeader, String cellvalue) {
        if (colHeader != null && colHeader.getType() != null) {
            return colHeader.getType();
        } else if (rowHeader != null && rowHeader.getType() != null) {
            return rowHeader.getType();
        } else {
            try {
                if (CellLoader.isFormula(cellvalue)) {
                    return JavaOpenClass.getOpenClass(DoubleValue.class);
                }
                new String2DataConvertorFactory.String2DoubleConvertor().parse(cellvalue, null, null);
                return JavaOpenClass.getOpenClass(DoubleValue.class);
            } catch (Throwable t) {
                return JavaOpenClass.getOpenClass(StringValue.class);
            }
        }

    }

    /**
     *
     * @param ncells
     * @param sdef
     * @return the type that should be in the cell that is located in RETURN row
     *         or column
     *
     * Right now we allow only to return types = scalars or arrays.
     * @throws BoundError
     */

    private IOpenClass deriveSingleCellReturnType(int ncells, SpreadsheetHeaderDefinition sdef) throws BoundError {
        IOpenClass retType = spreadsheet.getHeader().getType();

        if (ncells < 2) {
            return retType;
        }

        IAggregateInfo aggr = retType.getAggregateInfo();
        if (aggr != null && aggr.getComponentType(retType) != null) {
            retType = aggr.getComponentType(retType);
        } else {
            throw new BoundError(sdef.findVarDef(RETURN_NAME).name,
                    "The return type is scalar, but there are more than one return cells");
        }
        return retType;
    }

    private IBindingContext getColContext(int col, IBindingContext scxt) {
        IBindingContextDelegator cxt = colContexts.get(col);
        if (cxt == null) {
            cxt = makeColContext(col, scxt);
            colContexts.put(col, cxt);
        } else {
            cxt.setDelegate(scxt);
        }

        return cxt;
    }

    private IBindingContext getRowContext(int row, IBindingContext scxt) {
        IBindingContext cxt = rowContexts.get(row);
        if (cxt == null) {
            cxt = makeRowContext(row, scxt);
            rowContexts.put(row, cxt);
        }

        return cxt;
    }

    private IBindingContextDelegator makeColContext(int col, IBindingContext scxt) {
        int h = spreadsheet.height();

        ModuleOpenClass rtype = new ModuleOpenClass(null, spreadsheet.getName() + "ColType" + col, cxt.getOpenL());

        ModuleBindingContext ccxt = new ModuleBindingContext(scxt, rtype);

        for (int row = 0; row < h; row++) {
            SpreadsheetHeaderDefinition rh = rowHeaders.get(row);
            if (rh == null) {
                continue;
            }
            SCell scell = spreadsheet.getCells()[row][col];
            for (SymbolicTypeDef rowdef : rh.getVars()) {
                String fieldname = "$" + rowdef.name.getIdentifier();
                rtype.addField(new SCellField(rtype, fieldname, scell));
            }

        }
        return ccxt;
    }

    private IString2DataConvertor makeConvertor(IOpenClass type) {
        return String2DataConvertorFactory.getConvertor(type.getInstanceClass());
    }

    private IOpenMethodHeader makeHeader(String name, IOpenMethodHeader header, IOpenClass type) {
        return new OpenMethodHeader(name, type, header.getSignature(), header.getDeclaringClass());
    }

    private IBindingContextDelegator makeRowContext(int row, IBindingContext scxt) {
        int w = spreadsheet.width();

        ModuleOpenClass rtype = new ModuleOpenClass(null, spreadsheet.getName() + "RowType" + row, cxt.getOpenL());

        ModuleBindingContext rcxt = new ModuleBindingContext(scxt, rtype);

        for (int col = 0; col < w; col++) {
            SpreadsheetHeaderDefinition h = columnHeaders.get(col);
            if (h == null) {
                continue;
            }
            SCell scell = spreadsheet.getCells()[row][col];
            for (SymbolicTypeDef coldef : h.getVars()) {
                String fieldname = "$" + coldef.name.getIdentifier();
                rtype.addField(new SCellField(rtype, fieldname, scell));
            }

        }
        return rcxt;
    }

    public void parseHeader(SpreadsheetHeaderDefinition h, StringValue sv) {
        try {

            SymbolicTypeDef parsed = parseHeaderElement(sv);
            String hname = parsed.name.getIdentifier();

            SpreadsheetHeaderDefinition h1 = varDefinitions.get(hname);

            if (h1 != null) {
                throw new DuplicatedVarException(null, hname);
            } else {
                varDefinitions.put(hname, h);
            }

            h.addVarHeader(parsed);

        } catch (Throwable t) {
            BoundError b = null;
            if (t instanceof BoundError) {
                b = (BoundError) t;
            } else {
                b = new BoundError(t, sv.asSourceCodeModule());
            }
            tsn.addError(b);
            cxt.addError(b);
        }

    }

    SymbolicTypeDef parseHeaderElement(StringValue sv) throws BoundError {
        IdentifierNode[] nodes = TokenizerParser.tokenize(sv.asSourceCodeModule(), ":");
        switch (nodes.length) {
            case 1:
                return new SymbolicTypeDef(nodes[0], null);
            case 2:
                return new SymbolicTypeDef(nodes[0], nodes[1]);
            default:
                throw new BoundError("Valid header format: name [: type]", sv.asSourceCodeModule());
        }
    }

    protected BoundError processReturnCells() throws BoundError {

        SpreadsheetHeaderDefinition sdef = varDefinitions.get(RETURN_NAME);

        if (sdef == null) {
            return null;
        }

        int ncells = calculateNonEmptyCells(sdef);

        IOpenClass ctype = deriveSingleCellReturnType(ncells, sdef);
        if (sdef.getType() == null) {
            sdef.setType(ctype);
        } else {
            throw new BoundError(sdef.getVars().get(0).name, "RETURN " + sdef.rowOrColumn()
                    + " derives it's type from the Spreadsheet return type and therefore must not be defined here");
        }
        returnHeaderDefinition = sdef;

        return null;

    }

}
