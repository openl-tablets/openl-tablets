package org.openl.rules.lang.xls.load;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.PathSourceCodeModule;

/**
 * A workbook is held behind a weak reference and read from the file again once it is collected. What a write has
 * changed lives only in that workbook until it is saved, so it must survive being collected.
 */
class UnloadableLazyWorkbookLoaderTest {

    @TempDir
    Path folder;

    private Path file;

    @BeforeEach
    void writeWorkbook() throws IOException {
        file = folder.resolve("Test.xlsx");
        try (var workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            workbook.createSheet("Sheet1").createRow(0).createCell(0).setCellValue("read from the file");
            workbook.write(out);
        }
    }

    @Test
    void modifiedWorkbookSurvivesGarbageCollection() {
        var loader = new UnloadableLazyWorkbookLoader(new PathSourceCodeModule(file));
        loader.setModified(true);
        loader.getWorkbook().getSheetAt(0).getRow(0).getCell(0).setCellValue("changed in memory");

        forceGarbageCollection();

        assertEquals("changed in memory",
                loader.getWorkbook().getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
    }

    @Test
    void cellWrittenThroughGridReachesTheFile() throws IOException {
        var source = new PathSourceCodeModule(file);
        var workbookSource = new XlsWorkbookSourceCodeModule(source, new UnloadableLazyWorkbookLoader(source));
        var grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, workbookSource));

        grid.setCellStringValue(0, 0, "written through the grid");
        forceGarbageCollection();
        workbookSource.save();

        try (var saved = WorkbookFactory.create(file.toFile())) {
            assertEquals("written through the grid", saved.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void concurrentCallersReadTheFileOnce() throws Exception {
        var loader = new UnloadableLazyWorkbookLoader(new PathSourceCodeModule(file));
        // An edit is under way: the workbook is held, so every caller must be answered with the one being written to.
        loader.setCanUnload(false);
        var callers = 8;
        var together = new CyclicBarrier(callers);
        var pool = Executors.newFixedThreadPool(callers);
        try {
            var answers = pool.invokeAll(Collections.nCopies(callers, (Callable<Workbook>) () -> {
                together.await();
                return loader.getWorkbook();
            }));
            var distinct = new HashSet<Workbook>();
            for (var answer : answers) {
                distinct.add(answer.get());
            }
            assertEquals(1, distinct.size(), "every caller must be answered with the same workbook");
        } finally {
            pool.shutdownNow();
        }
    }

    /** Collects what is unreachable, waiting for a reference of its own to be cleared rather than for a delay. */
    private static void forceGarbageCollection() {
        var canary = new WeakReference<>(new Object());
        for (var attempt = 0; attempt < 100 && canary.get() != null; attempt++) {
            System.gc();
        }
    }
}
