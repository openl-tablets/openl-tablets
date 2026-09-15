package org.openl.rules.lang.xls.load;

import java.lang.ref.WeakReference;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;

import org.openl.rules.lang.xls.SpreadsheetConstants;
import org.openl.source.IOpenSourceCodeModule;

/**
 * Provides lazy loading access to the workbook. When {@link #getWorkbook()} first time is called, it is loaded from
 * file system and then is cached using WeakReference. If that workbook is used nowhere, it can at a certain moment be
 * garbage collected, and after that if {@link #getWorkbook()} is called, the workbook will be loaded again. If you want
 * to prevent garbage collecting loaded Workbook instance, invoke {@link #setCanUnload(boolean) setCanUnload(false)}.
 *
 * <p>A workbook holding changes the file does not have yet is never collected: {@link #setModified(boolean)} keeps it
 * in memory until it is saved, so that a write is not thrown away and answered with the file it was read from.
 *
 * <p>Reading the file again and deciding whether to hold what was read happen under this object's lock. Without it two
 * threads finding nothing loaded each read the file, and the one whose workbook is kept answers every caller
 * afterwards — leaving the other writing into a workbook nothing will ever save.
 */
@RequiredArgsConstructor
public class UnloadableLazyWorkbookLoader implements WorkbookLoader {

    private final IOpenSourceCodeModule fileSource;

    private boolean canUnload = true;
    private boolean modified;
    private Workbook workbook; // Strong reference to workbook in edit mode. Do not remove it

    private WeakReference<Workbook> workbookCache = new WeakReference<>(null);
    private Integer numberOfSheetsCache;
    private SpreadsheetConstants spreadsheetConstantsCache;

    /**
     * Get the workbook. Depending on {@link #isCanUnload()} state, when this method is repeatedly called, it can (but
     * mustn't) return different instances of workbook java object.
     *
     * @return loaded workbook
     * @see #isCanUnload()
     */
    @Override
    public synchronized Workbook getWorkbook() {
        var cachedWorkbook = workbookCache.get();
        if (cachedWorkbook != null) {
            return cachedWorkbook;
        }

        Workbook wb = workbook != null ? workbook : loadWorkbook();
        workbookCache = new WeakReference<>(wb);
        if (!canUnload || modified) {
            // Store the strong reference to the workbook, so it will not garbage collected until setCanUnload(true)
            // invocation
            workbook = wb;
        }
        return wb;
    }

    protected Workbook loadWorkbook() {
        return WorkbookLoadUtils.loadWorkbook(fileSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SheetLoader getSheetLoader(int sheetIndex) {
        return new LazySheetLoader(this, sheetIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isCanUnload() {
        return canUnload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setCanUnload(boolean canUnload) {
        this.canUnload = canUnload;
        if (canUnload && !modified) {
            workbook = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setModified(boolean modified) {
        this.modified = modified;
        if (modified) {
            // Hold what is in memory: until it is saved, it is the only copy of the change, and a workbook read
            // from the file again would answer without it.
            workbook = getWorkbook();
        } else if (canUnload) {
            workbook = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getNumberOfSheets() {
        var numberOfSheets = numberOfSheetsCache;
        if (numberOfSheets == null) {
            numberOfSheets = getWorkbook().getNumberOfSheets();
            if (canUnload) {
                numberOfSheetsCache = numberOfSheets;
            }
        }
        return numberOfSheets;
    }

    @Override
    public synchronized SpreadsheetConstants getSpreadsheetConstants() {
        var spreadsheetConstants = spreadsheetConstantsCache;
        if (spreadsheetConstants == null) {
            spreadsheetConstants = new SpreadsheetConstants(getWorkbook().getSpreadsheetVersion());
            if (canUnload) {
                spreadsheetConstantsCache = spreadsheetConstants;
            }
        }
        return spreadsheetConstants;
    }
}
