package org.openl.excel.parser.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.excel.parser.SheetDescriptor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EventSheetDescriptor implements SheetDescriptor {
    @Getter
    private final String name;
    @Getter
    private final int index;
    @Getter
    private final int offset;

    @Getter
    @Setter
    private int firstRowNum;
    @Getter
    @Setter
    private int firstColNum;
}
