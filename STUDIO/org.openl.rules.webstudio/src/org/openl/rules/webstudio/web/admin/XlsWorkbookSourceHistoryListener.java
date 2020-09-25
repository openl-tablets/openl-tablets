package org.openl.rules.webstudio.web.admin;

import java.io.File;

import org.openl.rules.lang.xls.XlsWorkbookListener;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;

/**
 * @author Andrei Astrouski
 */
public class XlsWorkbookSourceHistoryListener implements XlsWorkbookListener {

    private String historyStoragePath;

    public XlsWorkbookSourceHistoryListener(String historyStoragePath) {
        if (historyStoragePath == null) {
            throw new IllegalArgumentException();
        }
        this.historyStoragePath = historyStoragePath;
    }

    @Override
    public void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
        File sourceFile = workbookSourceCodeModule.getSourceFile();
        beforeSave(sourceFile);
    }

    public void beforeSave(File sourceFile) {
        ProjectsInHistoryController.init(historyStoragePath, sourceFile);
    }

    @Override
    public void afterSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
        File sourceFile = workbookSourceCodeModule.getSourceFile();
        afterSave(sourceFile);
    }

    public void afterSave(File sourceFile) {
        ProjectsInHistoryController.save(historyStoragePath, sourceFile);
    }

}
