package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

public class Spreadsheet extends AMethod implements IMemberMetaInfo {

    SSheetBoundNode node;

    protected IResultBuilder resultBuilder;

    SCell[][] cells;

    // FIXME
    String[] rowNames;

    // FIXME
    String[] colNames;

    SpreadsheetType spreadsheetType;
    static public Spreadsheet createSpreadsheet(IOpenMethodHeader header, SSheetBoundNode node) {
        return new Spreadsheet(header, node);
    }
    public Spreadsheet(IOpenMethodHeader header, SSheetBoundNode node) {
        super(header);
        this.node = node;
    }

    public SCell[][] getCells() {
        return cells;
    }

    public BindingDependencies getDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public IResultBuilder getResultBuilder() {
        return resultBuilder;
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) node.getSyntaxNode()).getUri();
    }

    public SpreadsheetType getSpreadsheetType() {
        return spreadsheetType;
    }

    public ISyntaxNode getSyntaxNode() {
        return node.getSyntaxNode();
    }

    public int height() {
        return cells.length;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        SpreadsheetResult res = new SpreadsheetResult(this, (IDynamicObject) target, params, env);

        return resultBuilder.makeResult(res);
    }

    public List<SCell> listNonEmptyCells(SpreadsheetHeaderDefinition hdef) {
        List<SCell> list = new ArrayList<SCell>();
        int row = hdef.row;
        int col = hdef.column;

        if (row >= 0) {
            for (int i = 0; i < width(); ++i) {
                if (!cells[row][i].isEmpty()) {
                    list.add(cells[row][i]);
                }
            }
        } else {
            for (int i = 0; i < height(); ++i) {
                if (!cells[i][col].isEmpty()) {
                    list.add(cells[i][col]);
                }
            }
        }

        return list;
    }

    public void setCells(SCell[][] cells) {
        this.cells = cells;
    }

    public void setColumnNames(String[] colNames) {
        this.colNames = colNames;
    }

    public void setResultBuilder(IResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public void setSpreadsheetType(SpreadsheetType spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public int width() {
        return cells[0].length;
    }

}
