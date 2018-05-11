package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;

public interface MetaInfoReader {
    CellMetaInfo getMetaInfo(int row, int col);
}
