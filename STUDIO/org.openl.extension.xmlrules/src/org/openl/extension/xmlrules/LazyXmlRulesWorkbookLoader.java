package org.openl.extension.xmlrules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.rules.lang.xls.load.UnloadableLazyWorkbookLoader;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyXmlRulesWorkbookLoader extends UnloadableLazyWorkbookLoader {
    private final Logger log = LoggerFactory.getLogger(LazyXmlRulesWorkbookLoader.class);
    private final ExtensionModule extensionModule;
    private final File folder;

    public LazyXmlRulesWorkbookLoader(File folder, ExtensionModule extensionModule) {
        super(null);
        this.folder = folder;
        this.extensionModule = extensionModule;
    }

    @Override
    protected Workbook loadWorkbook() {
        InputStream is = null;
        Workbook workbook;
        try {
            log.info("loading workbook {}...", extensionModule.getFileName());

            is = new BufferedInputStream(new FileInputStream(new File(folder, extensionModule.getFileName())));
            workbook = WorkbookFactory.create(is);
            IOUtils.closeQuietly(is);
        } catch (Exception e) {
            log.error("Error while preprocessing workbook", e);

            String message = "Can't open source file or file is corrupted: " +
                    ExceptionUtils.getRootCauseMessage(e);
            throw new OpenLRuntimeException(message, e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return workbook;
    }

    public ExtensionModule getExtensionModule() {
        return extensionModule;
    }
}
