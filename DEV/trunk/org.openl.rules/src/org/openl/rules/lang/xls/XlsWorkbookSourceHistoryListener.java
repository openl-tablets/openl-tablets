package org.openl.rules.lang.xls;

import java.io.File;

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

    public void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule) {
        File sourceFile = workbookSourceCodeModule.getSourceFile();
        historyManager.save(sourceFile);
    }

}
