package org.openl.rules.ruleservice.core;

public final class OpenLServiceHolder {
    private static final class OpenLServiceHolderHolder {
        private static final OpenLServiceHolder INSTANCE = new OpenLServiceHolder();
    }

    private ThreadLocal<OpenLService> openLServiceHolder = new ThreadLocal<>();

    private OpenLServiceHolder() {
    }

    public static OpenLServiceHolder getInstance() {
        return OpenLServiceHolderHolder.INSTANCE;
    }

    public OpenLService get() {
        return openLServiceHolder.get();
    }

    public void setOpenLService(OpenLService openLService) {
        openLServiceHolder.set(openLService);
    }

    public void remove() {
        openLServiceHolder.remove();
    }
}
