package org.openl.rules.lang.xls.types.meta;

import java.util.List;

import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.method.table.MethodTableBoundNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;

public class MethodTableMetaInfoReader extends AMethodMetaInfoReader<MethodTableBoundNode> {
    public MethodTableMetaInfoReader(MethodTableBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        ILogicalTable body = getTableSyntaxNode().getTableBody();
        int height = body.getHeight();

        for (int i = 0; i < height; i++) {
            ICell cell = body.getCell(0, i);
            if (isNeededCell(cell, row, col)) {
                List<CellMetaInfo> metaInfoList = OpenLCellExpressionsCompiler.getMetaInfo(getSourceCodeModule(body),
                    ((TableMethod) getBoundNode().getMethod()).getCompositeMethod());

                return metaInfoList.get(i);
            }
        }

        return null;
    }

    private IOpenSourceCodeModule getSourceCodeModule(ILogicalTable bodyTable) {
        int height = bodyTable.getHeight();
        IOpenSourceCodeModule[] cellSources = new IOpenSourceCodeModule[height];

        for (int i = 0; i < height; i++) {
            cellSources[i] = new GridCellSourceCodeModule(bodyTable.getRow(i).getSource(), null);
        }

        return new CompositeSourceCodeModule(cellSources, "\n");
    }
}
