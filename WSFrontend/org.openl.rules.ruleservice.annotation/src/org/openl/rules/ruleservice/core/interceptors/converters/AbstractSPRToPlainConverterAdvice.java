package org.openl.rules.ruleservice.core.interceptors.converters;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

public abstract class AbstractSPRToPlainConverterAdvice<T> extends AbstractServiceMethodAfterReturningAdvice<T> implements IOpenClassAware, IOpenMemberAware {
    private XlsModuleOpenClass module;
    private IOpenMember openMember;

    protected XlsModuleOpenClass getModule() {
        return module;
    }

    @Override
    public void setIOpenClass(IOpenClass openClass) {
        this.module = (XlsModuleOpenClass) openClass;
    }

    protected IOpenMember getOpenMember() {
        return openMember;
    }

    @Override
    public void setIOpenMember(IOpenMember openMember) {
        this.openMember = openMember;
    }

}
