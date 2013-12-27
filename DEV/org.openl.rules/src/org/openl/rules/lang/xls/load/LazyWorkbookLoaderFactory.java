package org.openl.rules.lang.xls.load;

import org.openl.source.IOpenSourceCodeModule;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class LazyWorkbookLoaderFactory implements WorkbookLoaderFactory {
    private Set<WorkbookLoader> workbookLoadersCache = Collections.newSetFromMap(new WeakHashMap<WorkbookLoader, Boolean>());
    private boolean canUnload = true;

    @Override
    public WorkbookLoader createWorkbookLoader(IOpenSourceCodeModule fileSource) {
        WorkbookLoader workbookLoader = new LazyWorkbookLoader(fileSource);
        workbookLoader.setCanUnload(canUnload);

        if (!canUnload) {
            workbookLoadersCache.add(workbookLoader);
        }

        return workbookLoader;
    }

    public void disallowUnload() {
        canUnload = false;
    }

    public void allowUnload() {
        canUnload = true;
        for (WorkbookLoader workbookLoader : workbookLoadersCache) {
            workbookLoader.setCanUnload(true);
        }
        workbookLoadersCache.clear();
    }
}
