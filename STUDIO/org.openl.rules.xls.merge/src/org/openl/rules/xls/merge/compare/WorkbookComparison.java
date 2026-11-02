package org.openl.rules.xls.merge.compare;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.jspecify.annotations.Nullable;

/**
 * What a comparison of two versions of a workbook found, sheet by sheet, with both versions open to read the compared
 * tables from.
 *
 * <p>The versions stay open until the comparison is closed.
 */
public final class WorkbookComparison implements Closeable {

    /**
     * The sheets of the first version in its order, then the sheets only the second version holds in its order.
     */
    @Getter
    private final List<SheetComparison> sheets;
    private final Workbook first;
    private final Workbook second;

    public WorkbookComparison(List<SheetComparison> sheets, Workbook first, Workbook second) {
        this.sheets = List.copyOf(sheets);
        this.first = first;
        this.second = second;
    }

    /**
     * A sheet of the first version.
     *
     * @return the sheet, or {@code null} when the first version has no sheet of that name
     */
    public @Nullable Sheet firstSheet(String name) {
        return first.getSheet(name);
    }

    /**
     * A sheet of the second version.
     *
     * @return the sheet, or {@code null} when the second version has no sheet of that name
     */
    public @Nullable Sheet secondSheet(String name) {
        return second.getSheet(name);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(first);
        second.close();
    }
}
