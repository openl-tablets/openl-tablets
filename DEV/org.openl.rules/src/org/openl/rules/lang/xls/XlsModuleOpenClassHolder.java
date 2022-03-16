package org.openl.rules.lang.xls;

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

    public XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClassThreadLocal.get();
    }

    public void setXlsModuleOpenClass(XlsModuleOpenClass xlsModuleOpenClass) {
        xlsModuleOpenClassThreadLocal.set(xlsModuleOpenClass);
    }
}
