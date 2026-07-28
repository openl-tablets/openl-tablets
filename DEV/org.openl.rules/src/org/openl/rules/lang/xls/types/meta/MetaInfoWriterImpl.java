package org.openl.rules.lang.xls.types.meta;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * This class should be used in edit actions only when meta info can be set to new cells etc
 */
public class MetaInfoWriterImpl implements MetaInfoWriter {
    // Rows/columns can be added. We cannot use array with fixed size here.
    private final Map<CellKey, CellMetaInfo> metaInfoMap = new HashMap<>();

    public MetaInfoWriterImpl(MetaInfoReader delegate, IGridTable gridTable) {
        // Copy all meta info for a given table from delegate.
        var region = GridTableUtils.getOriginalTable(gridTable).getRegion();

        var top = region.getTop();
        var left = region.getLeft();
        var bottom = region.getBottom();
        var right = region.getRight();
        for (var row = top; row <= bottom; row++) {
            for (var col = left; col <= right; col++) {
                CellKey key = CellKey.CellKeyFactory.getCellKey(col, row);
                metaInfoMap.put(key, delegate.getMetaInfo(row, col));
            }
        }
    }

    @Override
    public void setMetaInfo(int row, int col, CellMetaInfo metaInfo) {
        CellKey key = CellKey.CellKeyFactory.getCellKey(col, row);
        if (metaInfo == null) {
            metaInfoMap.remove(key);
        } else {
            metaInfoMap.put(key, metaInfo);
        }
    }

    @Override
    public CellMetaInfo getMetaInfo(int row, int col) {
        CellKey key = CellKey.CellKeyFactory.getCellKey(col, row);
        return metaInfoMap.get(key);
    }

    @Override
    public void prepare(IGridRegion region) {
    }

    @Override
    public void release() {
    }
}
