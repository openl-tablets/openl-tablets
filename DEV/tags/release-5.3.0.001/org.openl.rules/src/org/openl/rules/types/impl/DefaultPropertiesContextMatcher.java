package org.openl.rules.types.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.table.properties.ITableProperties;

public class DefaultPropertiesContextMatcher implements IPropertiesContextMatcher {

    private Map<String, MatchingConstraint<?, ?>> constraints = new HashMap<String, MatchingConstraint<?, ?>>();

    public DefaultPropertiesContextMatcher() {
        initilaize();
    }

    public MatchingResult match(String propName, ITableProperties props, IRulesContext context) {
        MatchingConstraint<?, ?> mc = constraints.get(propName);

        if (mc == null)
            throw new RuntimeException("Unexpectedly could not find a constarint for the property: " + propName);

        return mc.match(props, context);
    }

    protected void initilaize() {
        // <<< INSERT >>>
		constraints.put("effectiveDate", new MatchingConstraint<java.util.Date, java.util.Date>() {

			@Override
			protected java.util.Date getContextValue(IRulesContext context) {
			    return context.getCurrentDate();
			}
			
			@Override
			protected java.util.Date getPropertyValue(ITableProperties properties) {
			    return properties.getEffectiveDate();
			}
			
			@Override
			protected boolean matchNotNulls(java.util.Date propertyValue, java.util.Date contextValue) {
			    return LE(propertyValue, contextValue);
			}
			
        });
		constraints.put("expirationDate", new MatchingConstraint<java.util.Date, java.util.Date>() {

			@Override
			protected java.util.Date getContextValue(IRulesContext context) {
			    return context.getCurrentDate();
			}
			
			@Override
			protected java.util.Date getPropertyValue(ITableProperties properties) {
			    return properties.getExpirationDate();
			}
			
			@Override
			protected boolean matchNotNulls(java.util.Date propertyValue, java.util.Date contextValue) {
			    return GT(propertyValue, contextValue);
			}
			
        });
		constraints.put("lob", new MatchingConstraint<java.lang.String, java.lang.String>() {

			@Override
			protected java.lang.String getContextValue(IRulesContext context) {
			    return context.getLob();
			}
			
			@Override
			protected java.lang.String getPropertyValue(ITableProperties properties) {
			    return properties.getLob();
			}
			
			@Override
			protected boolean matchNotNulls(java.lang.String propertyValue, java.lang.String contextValue) {
			    return EQ(propertyValue, contextValue);
			}
			
        });
		constraints.put("usregion", new MatchingConstraint<java.lang.String, java.lang.String>() {

			@Override
			protected java.lang.String getContextValue(IRulesContext context) {
			    return context.getUsRegion();
			}
			
			@Override
			protected java.lang.String getPropertyValue(ITableProperties properties) {
			    return properties.getUsregion();
			}
			
			@Override
			protected boolean matchNotNulls(java.lang.String propertyValue, java.lang.String contextValue) {
			    return EQ(propertyValue, contextValue);
			}
			
        });
		constraints.put("country", new MatchingConstraint<java.lang.String, java.lang.String>() {

			@Override
			protected java.lang.String getContextValue(IRulesContext context) {
			    return context.getCountry();
			}
			
			@Override
			protected java.lang.String getPropertyValue(ITableProperties properties) {
			    return properties.getCountry();
			}
			
			@Override
			protected boolean matchNotNulls(java.lang.String propertyValue, java.lang.String contextValue) {
			    return EQ(propertyValue, contextValue);
			}
			
        });
		constraints.put("state", new MatchingConstraint<java.lang.String, java.lang.String>() {

			@Override
			protected java.lang.String getContextValue(IRulesContext context) {
			    return context.getUsState();
			}
			
			@Override
			protected java.lang.String getPropertyValue(ITableProperties properties) {
			    return properties.getState();
			}
			
			@Override
			protected boolean matchNotNulls(java.lang.String propertyValue, java.lang.String contextValue) {
			    return EQ(propertyValue, contextValue);
			}
			
        });
        // <<< END INSERT >>>
    }
}
