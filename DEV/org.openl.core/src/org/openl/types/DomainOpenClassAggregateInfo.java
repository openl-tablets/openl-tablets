package org.openl.types;

import java.lang.reflect.Array;

import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;

/**
 * Aggregate info for {@link DomainOpenClass} for creating aggregate and
 * component types based on domain info.
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
    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dim) {
        DomainOpenClass domainType = (DomainOpenClass) componentType;
        int[] dims = new int[dim];
        Class<?> type = domainType.getInstanceClass();
        Class<?> clazz = Array.newInstance(type, dims).getClass();
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(clazz);

        String domainName = domainType.getName();

        StringBuilder buf = new StringBuilder();
        buf.append(domainName);
        for (int i = 0; i < dim; i++) {
            buf.append("[]");
        }
        String name = buf.toString();

        return new DomainOpenClass(name, openClass, domainType.getDomain(), domainType.getMetaInfo());
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
