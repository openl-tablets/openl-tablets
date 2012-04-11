package org.openl.types;

import java.lang.reflect.Array;

import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.generation.JavaClassGeneratorHelper;

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
	public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dim) {
		DomainOpenClass domainType = (DomainOpenClass)componentType;
        int[] dims = new int[dim];

        Object ary = Array.newInstance(domainType.getInstanceClass(), dims);
        
        String domainName = domainType.getName();
        
        String newName = JavaClassGeneratorHelper.getArrayName(domainName, dim);
        
        return new DomainOpenClass(newName, 
        		JavaOpenClass.getOpenClass(ary.getClass()), domainType.getDomain(), domainType.getMetaInfo());
    }
	
	/**
	 * Overriden to return component type based on the domain
	 */
	@Override
	public IOpenClass getComponentType(IOpenClass aggregateType) {
		DomainOpenClass domainType = (DomainOpenClass)aggregateType;
		
		String domainName = domainType.getName();
		
		String newDomainName = domainName;
		
		int dimension = JavaClassGeneratorHelper.getDimension(domainName);
		
		if (dimension > 0) {
			newDomainName = JavaClassGeneratorHelper.getArrayName(JavaClassGeneratorHelper.getNameWithoutBrackets(domainName), 
					dimension - 1);
		}
         
		return new DomainOpenClass(newDomainName, super.getComponentType(domainType.getBaseClass()), domainType.getDomain(), domainType.getMetaInfo()); 
	}

}
