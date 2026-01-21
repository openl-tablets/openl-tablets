package org.openl.rules.lang.xls.load;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.excel.parser.ExcelUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;

// Package scope util class
final class WorkbookLoadUtils {
    private WorkbookLoadUtils() {
    }

    static Workbook loadWorkbook(IOpenSourceCodeModule fileSource) {
        Logger log = LoggerFactory.getLogger(WorkbookLoadUtils.class);
        log.debug("Loading workbook '{}'...", fileSource.getUri());
        if (VirtualSourceCodeModule.SOURCE_URI.equals(fileSource.getUri())) {
            return new XSSFWorkbook();
        }

        ExcelUtils.configureZipBombDetection();

        try (var is = fileSource.getByteStream()) {
            return WorkbookFactory.create(is);
        } catch (Exception e) {
            log.error("Error while preprocessing workbook", e);

            String message = "Cannot open source file or file is corrupted: " + ExceptionUtils.getRootCauseMessage(e);
            throw new OpenlNotCheckedException(message, e);
        }
    }
}
