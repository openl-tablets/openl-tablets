package org.openl.types.impl;

import org.openl.domain.IDomain;
import org.openl.meta.IMetaInfo;
import org.openl.types.IOpenClass;

public class DomainOpenClass extends OpenClassDelegator {

    @SuppressWarnings("unchecked")
    private IDomain domain;

    @SuppressWarnings("unchecked")
    public DomainOpenClass(String name, IOpenClass baseClass, IDomain domain, IMetaInfo metaInfo) {
        super(name, baseClass, metaInfo);
        this.domain = domain;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IDomain getDomain() {
        return domain;
    }

    @SuppressWarnings("unchecked")
    public void setDomain(IDomain domain) {
        this.domain = domain;
    }

}
