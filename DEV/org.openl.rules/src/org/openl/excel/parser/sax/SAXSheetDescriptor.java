package org.openl.excel.parser.sax;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.excel.parser.SheetDescriptor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SAXSheetDescriptor implements SheetDescriptor {
    @Getter
    private final String name;
    @Getter
    private final int index;
    @Getter
    private final String relationId;

    @Getter
    @Setter
    private int firstRowNum;
    @Getter
    @Setter
    private int firstColNum;
}
