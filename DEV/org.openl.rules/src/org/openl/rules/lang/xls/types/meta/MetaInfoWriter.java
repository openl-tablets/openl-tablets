package org.openl.rules.lang.xls.types.meta;

import org.openl.rules.lang.xls.types.CellMetaInfo;

public interface MetaInfoWriter extends MetaInfoReader {
    void setMetaInfo(int row, int col, CellMetaInfo metaInfo);
}
