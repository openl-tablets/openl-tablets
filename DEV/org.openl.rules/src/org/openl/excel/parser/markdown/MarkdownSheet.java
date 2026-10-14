package org.openl.excel.parser.markdown;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.excel.parser.SheetDescriptor;

/**
 * The sheet of a Markdown file: the table of the file, if it has one.
 *
 * <p>A file is a workbook of this sheet alone. The cells start at the top left corner of the sheet.
 */
@Getter
@RequiredArgsConstructor
final class MarkdownSheet implements SheetDescriptor {

    private final String name;
    private final @Nullable Object[][] cells;

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public int getFirstRowNum() {
        return 0;
    }

    @Override
    public int getFirstColNum() {
        return 0;
    }
}
