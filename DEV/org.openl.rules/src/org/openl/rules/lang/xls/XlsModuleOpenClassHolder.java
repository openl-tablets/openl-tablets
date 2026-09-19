package org.openl.rules.lang.xls;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;

/*
 * This is ugly workaround, but no way to fix the issue EPBDS-12543 in other way
 * */
public final class XlsModuleOpenClassHolder {

    private final ThreadLocal<XlsModuleOpenClass> xlsModuleOpenClassThreadLocal = new ThreadLocal<>();

    private XlsModuleOpenClassHolder() {
    }

    private static class XlsModuleOpenClassHolderHolder {
        private static final XlsModuleOpenClassHolder INSTANCE = new XlsModuleOpenClassHolder();
    }

    public static XlsModuleOpenClassHolder getInstance() {
        return XlsModuleOpenClassHolderHolder.INSTANCE;
    }

    public @Nullable XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClassThreadLocal.get();
    }

    /**
     * Binds the module to the current thread, or unbinds it when {@code null} is passed.
     *
     * <p>Unbinding drops the thread-local entry, so a pooled thread does not keep the module and its class
     * loader alive after the binding is over.
     */
    public void setXlsModuleOpenClass(@Nullable XlsModuleOpenClass xlsModuleOpenClass) {
        if (xlsModuleOpenClass == null) {
            xlsModuleOpenClassThreadLocal.remove();
        } else {
            xlsModuleOpenClassThreadLocal.set(xlsModuleOpenClass);
        }
    }
}
