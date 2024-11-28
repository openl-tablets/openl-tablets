package org.openl.rules.lang.xls.load;

import java.util.Objects;

import org.openl.source.IOpenSourceCodeModule;

public final class WorkbookLoaders {

    private WorkbookLoaders() {
    }

    private static final WorkbookLoaderFactory DEFAULT_FACTORY = new LazyWorkbookLoaderFactory(true);
    private static final ThreadLocal<WorkbookLoaderFactory> workbookLoaderFactoryHolder = new ThreadLocal<>();

    public static void setCurrentFactory(WorkbookLoaderFactory factory) {
        workbookLoaderFactoryHolder.set(factory);
    }

    public static void resetCurrentFactory() {
        workbookLoaderFactoryHolder.remove();
    }

    public static WorkbookLoader getWorkbookLoader(IOpenSourceCodeModule fileSource) {
        WorkbookLoaderFactory workbookLoaderFactory = workbookLoaderFactoryHolder.get();
        return Objects.requireNonNullElse(workbookLoaderFactory, DEFAULT_FACTORY).createWorkbookLoader(fileSource);
    }
}
