package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.method.table.MethodTableBoundNode;
import org.openl.rules.method.table.TableMethod;
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
        var body = getTableSyntaxNode().getTableBody();
        var height = body.getHeight();

        for (var i = 0; i < height; i++) {
            var cell = body.getCell(0, i);
            if (isNeededCell(cell, row, col)) {
                var metaInfoList = MetaInfoReaderUtils.getMetaInfo(getSourceCodeModule(body),
                        ((TableMethod) getBoundNode().getMethod()).getCompositeMethod());

                return metaInfoList.get(i);
            }
        }

        return null;
    }

    private IOpenSourceCodeModule getSourceCodeModule(ILogicalTable bodyTable) {
        var height = bodyTable.getHeight();
        IOpenSourceCodeModule[] cellSources = new IOpenSourceCodeModule[height];

        for (var i = 0; i < height; i++) {
            cellSources[i] = new GridCellSourceCodeModule(bodyTable.getRow(i).getSource(), null);
        }

        return new CompositeSourceCodeModule(cellSources, "\n");
    }
}
