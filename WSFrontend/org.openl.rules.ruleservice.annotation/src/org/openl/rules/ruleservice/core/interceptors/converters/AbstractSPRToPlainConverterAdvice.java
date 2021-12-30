package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Array;
import java.util.Map;

import org.openl.rules.calc.AnySpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

public abstract class AbstractSPRToPlainConverterAdvice<T> extends AbstractServiceMethodAfterReturningAdvice<T> implements IOpenClassAware, IOpenMemberAware {

    private XlsModuleOpenClass module;
    private IOpenMember openMember;
    private volatile Class<?> convertToType;
    private volatile boolean convertToTypeInitialized;

    protected XlsModuleOpenClass getModule() {
        return module;
    }

    protected IOpenMember getOpenMember() {
        return openMember;
    }

    @Override
    public void setIOpenClass(IOpenClass openClass) {
        this.module = (XlsModuleOpenClass) openClass;
    }

    @Override
    public void setIOpenMember(IOpenMember openMember) {
        this.openMember = openMember;
        this.convertToType = getConvertToType();
    }

    protected Class<?> getConvertToType() {
        if (!convertToTypeInitialized) {
            synchronized (this) {
                if (!convertToTypeInitialized) {
                    IOpenClass openClass = openMember.getType();
                    int dim = 0;
                    while (openClass.isArray()) {
                        openClass = openClass.getComponentClass();
                        dim++;
                    }
                    if (openClass instanceof SpreadsheetResultOpenClass || openClass instanceof AnySpreadsheetResultOpenClass) {
                        Class<?> t = Map.class;
                        if (openClass instanceof SpreadsheetResultOpenClass && ((SpreadsheetResultOpenClass) openClass)
                            .getModule() != null) {
                            SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) openClass;
                            t = spreadsheetResultOpenClass.getModule()
                                .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                .toCustomSpreadsheetResultOpenClass()
                                .getBeanClass();
                        }
                        if (dim > 0) {
                            t = Array.newInstance(t, dim).getClass();
                        }
                        convertToType = t;
                    }
                    convertToTypeInitialized = true;
                }
            }
        }
        return convertToType;
    }
}
