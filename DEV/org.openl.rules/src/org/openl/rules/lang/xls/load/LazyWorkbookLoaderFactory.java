package org.openl.rules.lang.xls.load;

import lombok.RequiredArgsConstructor;

import org.openl.source.IOpenSourceCodeModule;

@RequiredArgsConstructor
public class LazyWorkbookLoaderFactory implements WorkbookLoaderFactory {
    private final boolean canUnload;

    @Override
    public WorkbookLoader createWorkbookLoader(IOpenSourceCodeModule fileSource) {
        return canUnload ? new UnloadableLazyWorkbookLoader(fileSource) : new GreedyLazyWorkbookLoader(fileSource);
    }
}
