package org.openl.types;

import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;

/**
 * Aggregate info for {@link DomainOpenClass} for creating aggregate and component types based on domain info.
 *
 * @author DLiauchuk
 *
 */
public class DomainOpenClassAggregateInfo extends JavaArrayAggregateInfo {

    public static final IAggregateInfo DOMAIN_AGGREGATE = new DomainOpenClassAggregateInfo();

    /**
     * Overriden to create aggregate type based on the domain restrictions
     */
    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType) {
        IOpenClass openClass = getArrayType(componentType);
        return new DomainOpenClass(componentType.getName() + "[]",
            openClass,
            componentType.getDomain(),
            componentType.getMetaInfo());
    }

    /**
     * Overriden to return component type based on the domain
     */
    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        DomainOpenClass domainType = (DomainOpenClass) aggregateType;

        String domainName = domainType.getName();
        if (!domainType.isArray()) {
            return null;
        }

        // remove on dimension to get component type
        // MyData[][] -> MyData[]
        // MyData[] -> MyData
        String componentType = domainName.replaceFirst("\\[]", "");

        return new DomainOpenClass(componentType,
            super.getComponentType(domainType.getBaseClass()),
            domainType.getDomain(),
            domainType.getMetaInfo());
    }

}
