package org.openl.rules.table.properties;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.types.impl.DefaultPropertiesContextMatcher;
import org.openl.rules.types.impl.IPropertiesContextMatcher;

/**
 * 
 * @author snshor
 * Created Oct 1, 2009 
 *
 *  This class provides a single point for properties handling within
 *  OpenL module. User-defined properties implementors may override this class.
 *  
 *  In general, there could be two ways of extending properties set:
 *  1) by implementing Java extensions to properties classes
 *  2) by defining new property definitions within OpenL Tablets dynamically per-project 
 *
 */

public class ITablePropertiesFactory {

    /**
     * 
     * @return the Java class correponding to the interface of the table 
     * properties. Can be overridden by user implementation. 
     * 
     * See #getRulesContextInterface
     */
    public Class<ITableProperties> getTablePropertiesInterface()
    {
        return ITableProperties.class;
    }
    
    /**
     * 
     * @return a complete set of TablePropertyDefinition, one per property
     */
    public TablePropertyDefinition[] getTablePropertyDefinitions()
    {
        return DefaultPropertyDefinitions.getDefaultDefinitions();
    }
    
    /**
     * 
     * @return the Java interface for IRulesContext
     */
    
    public Class<IRulesContext> getRulesContextInterface()
    {
        return IRulesContext.class;
    }
    
    
    /**
     * 
     * @return IPropertiesContextMatcher instance. For java properties extension it is recommended to
     * provide a new java implementation with new properties hard-coded into the new matcher.
     * There is also a possibility to dynamically extend existing matcher by calling {@link IPropertiesContextMatcher#addConstraint(String, org.openl.rules.types.impl.MatchingConstraint)},
     * but it should be reserved for dynamic property definitions. 
     */
    
    public IPropertiesContextMatcher getMatcher()
    {
        return new DefaultPropertiesContextMatcher();
    }
    
    
    
    
}
