package org.apache.poi.xssf.streaming;

import java.io.IOException;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCells;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;

/**
 * This class implements optimized merged region addition. The class must be in the same package as SXSSFWorkbook to
 * access its package level methods and fields. Remove this class when this bug will be fixed:
 * https://bz.apache.org/bugzilla/show_bug.cgi?id=60397
 */
public class CustomizedSXSSFWorkbook extends SXSSFWorkbook {

    /**
     * Overrides base implementation to return CustomizedSXSSFSheet
     */
    @Override
    CustomizedSXSSFSheet createAndRegisterSXSSFSheet(XSSFSheet xSheet) {
        final CustomizedSXSSFSheet sxSheet;
        try {
            sxSheet = new CustomizedSXSSFSheet(this, xSheet);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        registerSheetMapping(sxSheet, xSheet);
        return sxSheet;
    }

    private static class CustomizedSXSSFSheet extends SXSSFSheet {
        public CustomizedSXSSFSheet(SXSSFWorkbook workbook, XSSFSheet xSheet) throws IOException {
            super(workbook, xSheet);
        }

        @Override
        public int addMergedRegionUnsafe(CellRangeAddress region) {
            CTWorksheet worksheet = _sh.getCTWorksheet();

            CTMergeCells ctMergeCells = worksheet.isSetMergeCells() ? worksheet.getMergeCells()
                                                                    : worksheet.addNewMergeCells();
            CTMergeCell ctMergeCell = ctMergeCells.addNewMergeCell();
            ctMergeCell.setRef(region.formatAsString());

            // Don't invoke ctMergeCells.sizeOfMergeCellArray() because it's very slow and not needed in our case.
            // See https://bz.apache.org/bugzilla/show_bug.cgi?id=60397#c5 for details.
            return 0;
        }
    }
}
