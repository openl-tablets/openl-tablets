package org.openl.excel.grid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.excel.parser.SheetDescriptor;
import org.openl.rules.lang.xls.TestWorkbooks;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.UnloadableLazyWorkbookLoader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.PathSourceCodeModule;

/**
 * A sheet taken up to be written is written through the workbook itself, which is not safe to touch from two
 * threads — see {@link org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule}. So it is given to the thread
 * writing it and to nobody else: everybody else reads the sheet as it was parsed.
 */
class ParsedGridEditingTest {

    @TempDir
    Path folder;

    @Test
    void the_sheet_being_written_is_given_to_the_thread_writing_it() throws IOException {
        var parsed = parsedSheet();

        parsed.table().edit();

        assertInstanceOf(XlsSheetGridModel.class, parsed.table().getGrid(),
                "the thread writing reads and writes through the workbook, where its own cells are");
        assertInstanceOf(ParsedGrid.class, onAnotherThread(parsed.table()::getGrid),
                "everybody else reads the sheet as it was parsed, rather than half of somebody's write");
    }

    @Test
    void the_sheet_is_read_as_parsed_again_once_the_write_has_finished() throws IOException {
        var parsed = parsedSheet();

        parsed.table().edit();
        parsed.table().stopEditing();

        assertInstanceOf(ParsedGrid.class, parsed.table().getGrid());
    }

    @Test
    void a_write_another_thread_left_behind_is_let_go_of_all_the_same() throws IOException {
        var parsed = parsedSheet();
        // Saving a workbook says this of every sheet in it, including one that another request took up and left.
        onAnotherThread(() -> {
            parsed.table().edit();
            return null;
        });
        assertFalse(parsed.loader().isCanUnload(), "a sheet being written holds its workbook in memory");

        parsed.table().stopEditing();

        assertTrue(parsed.loader().isCanUnload(), "and lets go of it however the write ended");
        var grid = (ParsedGrid) parsed.table().getGrid();
        assertNull(onAnotherThread(grid::writableGridIfWriting));
    }

    /** The one table of a parsed sheet, and the loader holding the workbook it is written through. */
    private record ParsedSheet(IGridTable table, UnloadableLazyWorkbookLoader loader) {
    }

    private ParsedSheet parsedSheet() throws IOException {
        var file = TestWorkbooks.writeOneCell(folder);
        var source = new PathSourceCodeModule(file);
        var loader = new UnloadableLazyWorkbookLoader(source);
        var sheetSource = new XlsSheetSourceCodeModule(0, new XlsWorkbookSourceCodeModule(source, loader));
        Object[][] cells = {{TestWorkbooks.CELL}};
        // A sheet the parser answers for: its name is read only where a style cannot be, and it begins at 0:0.
        var grid = new ParsedGrid(file.toString(), sheetSource, mock(SheetDescriptor.class), cells, false);
        return new ParsedSheet(grid.getTables()[0], loader);
    }

    private static <T> T onAnotherThread(Supplier<T> ask) {
        return CompletableFuture.supplyAsync(ask).join();
    }
}
