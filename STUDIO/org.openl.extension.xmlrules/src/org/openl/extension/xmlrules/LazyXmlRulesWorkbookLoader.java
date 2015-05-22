package org.openl.extension.xmlrules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.openl.exception.OpenLRuntimeException;
import org.openl.extension.xmlrules.model.Project;
import org.openl.rules.lang.xls.load.LazySheetLoader;
import org.openl.rules.lang.xls.load.LazyWorkbookLoader;
import org.openl.rules.lang.xls.load.SheetLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyXmlRulesWorkbookLoader extends LazyWorkbookLoader {
    private final Logger log = LoggerFactory.getLogger(LazyXmlRulesWorkbookLoader.class);
    private final Project project;
    private final File folder;

    public LazyXmlRulesWorkbookLoader(File folder, Project project) {
        super(null);
        this.folder = folder;
        this.project = project;
    }

    @Override
    public SheetLoader getSheetLoader(final int sheetIndex) {
        return new LazySheetLoader(this, sheetIndex) {
            @Override
            public String getSheetName() {
                return "" + sheetIndex; //FIXME
            }
        };
    }

    @Override
    protected Workbook loadWorkbook() {
        InputStream is = null;
        Workbook workbook;
        try {
            log.info("loading workbook {}...", project.getXlsFileName());

            is = new BufferedInputStream(new FileInputStream(new File(folder, project.getXlsFileName())));
            workbook = WorkbookFactory.create(is);
            IOUtils.closeQuietly(is);
        } catch (Exception e) {
            log.error("Error while preprocessing workbook", e);

            String message = "Can't open source file or file is corrupted: " +
                    ExceptionUtils.getRootCause(e).getMessage();
            throw new OpenLRuntimeException(message, e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return workbook;
    }
}
