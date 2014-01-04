package org.openl.rules.lang.xls.load;

import org.openl.source.IOpenSourceCodeModule;

public class SimpleWorkbookLoaderFactory implements WorkbookLoaderFactory {
    @Override
    public WorkbookLoader createWorkbookLoader(IOpenSourceCodeModule fileSource) {
        return new SimpleWorkbookLoader(WorkbookLoadUtils.loadWorkbook(fileSource));
    }
}
