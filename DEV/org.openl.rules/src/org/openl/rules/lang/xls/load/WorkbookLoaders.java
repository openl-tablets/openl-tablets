package org.openl.rules.lang.xls.load;

import org.openl.source.IOpenSourceCodeModule;

public abstract class WorkbookLoaders {
    private static final WorkbookLoaderFactory DEFAULT_FACTORY = new LazyWorkbookLoaderFactory(true);
    private static ThreadLocal<WorkbookLoaderFactory> workbookLoaderFactoryHolder = new ThreadLocal<WorkbookLoaderFactory>() {
        @Override
        protected WorkbookLoaderFactory initialValue() {
            return DEFAULT_FACTORY;
        }
    };

    public static void setCurrentFactory(WorkbookLoaderFactory factory) {
        workbookLoaderFactoryHolder.set(factory);
    }

    public static void resetCurrentFactory() {
        workbookLoaderFactoryHolder.remove();
    }

    public static WorkbookLoader getWorkbookLoader(IOpenSourceCodeModule fileSource) {
        return workbookLoaderFactoryHolder.get().createWorkbookLoader(fileSource);
    }
}
