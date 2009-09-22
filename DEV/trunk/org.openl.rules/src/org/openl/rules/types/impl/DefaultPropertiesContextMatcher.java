package org.openl.rules.types.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.table.properties.ITableProperties;

public class DefaultPropertiesContextMatcher implements IPropertiesContextMatcher {

    
    public DefaultPropertiesContextMatcher()
    {
        initilaize();
    }
    
    public MatchingResult match(String propName, ITableProperties props, IRulesContext context) {
        MatchingConstraint<?, ?> mc = constraints.get(propName);
        
        if (mc == null)
            throw new RuntimeException("Unexpectedly could not find a constarint for the property: " + propName);
        
        return mc.match(props, context);
    }

    Map<String, MatchingConstraint<?,?>> constraints = new HashMap<String, MatchingConstraint<?,?>>();
    
    
    protected void initilaize()
    {
        // <<< INSERT >>>
        constraints.put("effectiveDate", new MatchingConstraint<Date, Date>()
                {

                    @Override
                    protected Date getContextValue(IRulesContext cxt) {
                        return cxt.getCurrentDate();
                    }

                    @Override
                    protected Date getPropValue(ITableProperties props) {
                        return props.getEffectiveDate();
                    }

                    @Override
                    protected boolean matchNotNulls(Date propValue, Date contextValue) {
                        return LE(propValue, contextValue);
                    }
            
                });

        constraints.put("expirationDate", new MatchingConstraint<Date, Date>()
                {

                    @Override
                    protected Date getContextValue(IRulesContext cxt) {
                        return cxt.getCurrentDate();
                    }

                    @Override
                    protected Date getPropValue(ITableProperties props) {
                        return props.getExpirationDate();
                    }

                    @Override
                    protected boolean matchNotNulls(Date propValue, Date contextValue) {
                        return GT(propValue, contextValue);
                    }
            
                });
        // <<< END INSERT >>>
        
    }
    
    public static abstract class MatchingConstraint<P, C>
    {
        public MatchingResult match(ITableProperties props, IRulesContext cxt)
        {
            C contextValue = getContextValue(cxt);
            P propValue = getPropValue(props);
            return matchValues(propValue, contextValue);
        }
        
        protected abstract P getPropValue(ITableProperties props);

        protected abstract C getContextValue(IRulesContext cxt);

        public MatchingResult matchValues(P propValue, C contextValue)
        {
            if (propValue == null || contextValue == null)
                return MatchingResult.MATCH_BY_DEFAULT;
            return matchNotNulls(propValue, contextValue) ? MatchingResult.MATCH : MatchingResult.NO_MATCH ;
        }

        protected abstract boolean matchNotNulls(P propValue, C contextValue); 
        
        @SuppressWarnings("unchecked")
        static  public <T> boolean LE(Comparable<T> cmp1, Comparable<T> cmp2)
        {
            return cmp1.compareTo((T)cmp2) <= 0;
        }

        @SuppressWarnings("unchecked")
        static  public <T> boolean GT(Comparable<T> cmp1, Comparable<T> cmp2)
        {
            return cmp1.compareTo((T)cmp2) > 0;
        }
    }
    
    
    
}
