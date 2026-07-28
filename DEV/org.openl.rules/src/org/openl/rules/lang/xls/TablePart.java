package org.openl.rules.lang.xls;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.rules.table.IGridTable;

@RequiredArgsConstructor
public class TablePart implements Comparable<TablePart> {

    @Getter
    @Setter
    String partName;
    @Getter
    @Setter
    int part;
    @Getter
    @Setter
    boolean vertical;
    @Getter
    @Setter
    int size;

    @Getter
    final IGridTable table;
    @Getter
    final XlsSheetSourceCodeModule source;

    @Override
    public int compareTo(TablePart o) {
        return this.part - o.part;
    }

}
