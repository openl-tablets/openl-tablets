package org.openl.types.impl;

import org.openl.domain.IDomain;
import org.openl.meta.IMetaInfo;
import org.openl.types.DomainOpenClassAggregateInfo;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;

/**
 * {@link IOpenClass} implementation, that adds restriction for instances of this class by {@link IDomain}
 *
 */
public class DomainOpenClass extends OpenClassDelegator {
    
    private IDomain<?> domain;

    private IAggregateInfo aggregateInfo;

    public DomainOpenClass(String name, IOpenClass baseClass, IDomain<?> domain, IMetaInfo metaInfo) {
        super(name, baseClass, metaInfo);
        this.domain = domain;
    }

    @Override    
    public IDomain<?> getDomain() {
        return domain;
    }
    
    public void setDomain(IDomain<?> domain) {
        this.domain = domain;
    }
    
    /**
     * Overriden to add the possibility to return special aggregate info for DomainOpenClass
     * 
     * @author DLiauchuk
     */
    @Override
    public IAggregateInfo getAggregateInfo() { 
    	if (aggregateInfo == null) {
    		aggregateInfo = DomainOpenClassAggregateInfo.DOMAIN_AGGREGATE;
    	}
    	return aggregateInfo;
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }
}
