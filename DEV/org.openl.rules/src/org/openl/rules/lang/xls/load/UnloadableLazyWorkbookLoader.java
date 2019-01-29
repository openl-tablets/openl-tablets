package org.openl.rules.lang.xls.load;

import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.SpreadsheetConstants;
import org.openl.source.IOpenSourceCodeModule;

import java.lang.ref.WeakReference;

/**
 * Provides lazy loading access to the workbook.
 * When {@link #getWorkbook()} first time is called, it is loaded from file system
 * and then is cached using WeakReference.
 * If that workbook is used nowhere, it can at a certain moment be garbage collected,
 * and after that if {@link #getWorkbook()} is called, the workbook will be loaded again.
 * If you want to prevent garbage collecting loaded Workbook instance, invoke
 * {@link #setCanUnload(boolean) setCanUnload(false)}.
 */
public class UnloadableLazyWorkbookLoader implements WorkbookLoader {

    private final IOpenSourceCodeModule fileSource;

    private boolean canUnload = true;
    private Workbook workbook; // Strong reference to workbook in edit mode. Do not remove it

    private WeakReference<Workbook> workbookCache = new WeakReference<>(null);
    private Integer numberOfSheetsCache;
    private SpreadsheetConstants spreadsheetConstantsCache;

    public UnloadableLazyWorkbookLoader(IOpenSourceCodeModule fileSource) {
        this.fileSource = fileSource;
    }

    /**
     * Get the workbook.
     * Depending on {@link #isCanUnload()} state, when this method is repeatedly called,
     * it can (but mustn't) return different instances of workbook java object.
     *
     * @return loaded workbook
     * @see #isCanUnload()
     */
    @Override
    public Workbook getWorkbook() {
        Workbook cachedWorkbook = workbookCache.get();
        if (cachedWorkbook != null) {
            return cachedWorkbook;
        }

        Workbook wb = workbook != null ? workbook : loadWorkbook();
        workbookCache = new WeakReference<>(wb);
        if (!canUnload) {
            // Store the strong reference to the workbook, so it will not garbage collected until setCanUnload(true) invocation
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
    public boolean isCanUnload() {
        return canUnload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCanUnload(boolean canUnload) {
        this.canUnload = canUnload;
        if (canUnload) {
            workbook = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSheets() {
        Integer numberOfSheets = numberOfSheetsCache;
        if (numberOfSheets == null) {
            numberOfSheets = getWorkbook().getNumberOfSheets();
            if (canUnload) {
                numberOfSheetsCache = numberOfSheets;
            }
        }
        return numberOfSheets;
    }

    @Override
    public SpreadsheetConstants getSpreadsheetConstants() {
        SpreadsheetConstants spreadsheetConstants = spreadsheetConstantsCache;
        if (spreadsheetConstants == null) {
            spreadsheetConstants = new SpreadsheetConstants(getWorkbook().getSpreadsheetVersion());
            if (canUnload) {
                spreadsheetConstantsCache = spreadsheetConstants;
            }
        }
        return spreadsheetConstants;
    }
}
