package org.openl.rules.lang.xls;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.openl.source.SourceHistoryManager;

/**
 * @author Andrei Astrouski
 */
public class XlsWorkbookSourceHistoryListener implements XlsWorkbookListener {

    private SourceHistoryManager<File> historyManager;

    public XlsWorkbookSourceHistoryListener(SourceHistoryManager<File> historyManager) {
        if (historyManager == null) {
            throw new IllegalArgumentException();
        }
        this.historyManager = historyManager;
    }

    @Override
    public void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
        File sourceFile = workbookSourceCodeModule.getSourceFile();
        beforeSave(sourceFile);
    }

    public void beforeSave(File sourceFile) {
        historyManager.init();
    }

    @Override
    public void afterSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
        File sourceFile = workbookSourceCodeModule.getSourceFile();
        afterSave(sourceFile);
    }

    public void afterSave(File sourceFile) {
        historyManager.save(sourceFile);
    }

}
