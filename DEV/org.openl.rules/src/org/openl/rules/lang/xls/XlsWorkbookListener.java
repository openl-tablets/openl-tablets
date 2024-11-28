package org.openl.rules.lang.xls;

import java.util.EventListener;

public interface XlsWorkbookListener extends EventListener {

    void beforeSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule);

    void afterSave(XlsWorkbookSourceCodeModule workbookSourceCodeModule);

}