package org.openl.rules.lang.xls.load;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

// Package scope util class
final class WorkbookLoadUtils {
    private WorkbookLoadUtils() {
    }

    static Workbook loadWorkbook(IOpenSourceCodeModule fileSource) {
        Logger log = LoggerFactory.getLogger(WorkbookLoadUtils.class);
        log.debug("Loading workbook '{}'...", fileSource.getUri(0));

        InputStream is = null;
        Workbook workbook;

        // Disable zip bomb detection
        ZipSecureFile.setMinInflateRatio(0);

        try {
            is = fileSource.getByteStream();
            workbook = WorkbookFactory.create(is);
        } catch (Exception e) {
            log.error("Error while preprocessing workbook", e);

            String message = "Can't open source file or file is corrupted: " +
                    ExceptionUtils.getRootCauseMessage(e);
            throw new OpenLRuntimeException(message, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Throwable e) {
                log.error("Error trying close input stream:", e);
            }
        }

        return workbook;
    }
}
