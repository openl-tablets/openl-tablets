package org.openl.types.impl;

import org.openl.domain.IDomain;
import org.openl.meta.IMetaInfo;
import org.openl.types.IOpenClass;

public class DomainOpenClass extends OpenClassDelegator
{
    
    public DomainOpenClass(String name, IOpenClass baseClass, IDomain domain,  IMetaInfo metaInfo)
    {
	super(name, baseClass, metaInfo);
	this.domain = domain;
    }

    IDomain domain;

    public IDomain getDomain()
    {
        return domain;
    }

    public void setDomain(IDomain domain)
    {
        this.domain = domain;
    }
    
}
