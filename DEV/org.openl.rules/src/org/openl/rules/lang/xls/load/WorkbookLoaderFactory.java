package org.openl.rules.lang.xls.load;

import org.openl.source.IOpenSourceCodeModule;

public interface WorkbookLoaderFactory {
    WorkbookLoader createWorkbookLoader(IOpenSourceCodeModule fileSource);
}
