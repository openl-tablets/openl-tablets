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
 * What a comparison of the base version of a workbook with the two versions of a conflict found, sheet by sheet,
 * with the three versions open to read the compared tables from.
 *
 * <p>The versions stay open until the comparison is closed.
 */
public final class ConflictComparison implements Closeable {

    /**
     * The sheets of the base and your version in your order, then the sheets only their version holds.
     */
    @Getter
    private final List<ConflictSheet> sheets;
    private final Workbook base;
    private final Workbook yours;
    private final Workbook theirs;

    /**
     * @param sheets the sheets, the three versions lined up
     * @param base   the base version
     * @param yours  your version
     * @param theirs their version
     */
    public ConflictComparison(List<ConflictSheet> sheets, Workbook base, Workbook yours, Workbook theirs) {
        this.sheets = List.copyOf(sheets);
        this.base = base;
        this.yours = yours;
        this.theirs = theirs;
    }

    /**
     * A sheet of the base.
     *
     * @return the sheet, or {@code null} when the base has no sheet of that name
     */
    public @Nullable Sheet baseSheet(String name) {
        return base.getSheet(name);
    }

    /**
     * A sheet of your version.
     *
     * @return the sheet, or {@code null} when your version has no sheet of that name
     */
    public @Nullable Sheet yoursSheet(String name) {
        return yours.getSheet(name);
    }

    /**
     * A sheet of their version.
     *
     * @return the sheet, or {@code null} when their version has no sheet of that name
     */
    public @Nullable Sheet theirsSheet(String name) {
        return theirs.getSheet(name);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(base);
        IOUtils.closeQuietly(yours);
        theirs.close();
    }
}
