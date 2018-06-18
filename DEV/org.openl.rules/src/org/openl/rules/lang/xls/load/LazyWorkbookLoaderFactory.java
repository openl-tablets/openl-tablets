package org.openl.rules.lang.xls.load;

import org.openl.source.IOpenSourceCodeModule;

public class LazyWorkbookLoaderFactory implements WorkbookLoaderFactory {
    private final boolean canUnload;

    public LazyWorkbookLoaderFactory(boolean canUnload) {
        this.canUnload = canUnload;
    }

    @Override
    public WorkbookLoader createWorkbookLoader(IOpenSourceCodeModule fileSource) {
        return canUnload ? new UnloadableLazyWorkbookLoader(fileSource) : new GreedyLazyWorkbookLoader(fileSource);
    }
}
