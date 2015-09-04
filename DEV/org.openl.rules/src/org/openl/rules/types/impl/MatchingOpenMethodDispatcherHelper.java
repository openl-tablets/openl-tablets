package org.openl.rules.types.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.properties.ITableProperties;

public class MatchingOpenMethodDispatcherHelper {

    private static class MatchingOpenMethodDispatcherHelperHolder {
        public static final MatchingOpenMethodDispatcherHelper INSTANCE = new MatchingOpenMethodDispatcherHelper();
    }

    public static MatchingOpenMethodDispatcherHelper getInstance() {
        return MatchingOpenMethodDispatcherHelperHolder.INSTANCE;
    }

    private Map<String, PropertyAccessDelegation<?, ?>> helper = new HashMap<String, PropertyAccessDelegation<?, ?>>();

    private void put(String propertyName, PropertyAccessDelegation<?, ?> propertyAccessDelegation) {
        helper.put(propertyName, propertyAccessDelegation);
    }
    
    @SuppressWarnings("unchecked")    
    public Comparable<Object> getContextValue(String propertyName, IRulesRuntimeContext context) {
        Object contextValue = helper.get(propertyName).getContextValue(context);
        return (Comparable<Object>) contextValue;
    }

    @SuppressWarnings("unchecked")
    public Comparable<Object> getPropertyValue(String propertyName, ITableProperties properties) {
        Object propertyValue = helper.get(propertyName).getPropertyValue(properties);
        if (propertyValue instanceof Comparable){
            return (Comparable<Object>) propertyValue;
        }
        throw new OpenLRuntimeException("Match expression tries to compare not comparable objects");
    }

    private MatchingOpenMethodDispatcherHelper() {
        // <<< INSERT >>>
		put("effectiveDate", new PropertyAccessDelegation<java.util.Date, java.util.Date>() { 
			@Override
			protected java.util.Date getContextValue(IRulesRuntimeContext context) {
			    return context.getCurrentDate();
			}
	
			@Override
			protected java.util.Date getPropertyValue(ITableProperties properties) {
			    return properties.getEffectiveDate();
			}
		});
		put("expirationDate", new PropertyAccessDelegation<java.util.Date, java.util.Date>() { 
			@Override
			protected java.util.Date getContextValue(IRulesRuntimeContext context) {
			    return context.getCurrentDate();
			}
	
			@Override
			protected java.util.Date getPropertyValue(ITableProperties properties) {
			    return properties.getExpirationDate();
			}
		});
		put("startRequestDate", new PropertyAccessDelegation<java.util.Date, java.util.Date>() { 
			@Override
			protected java.util.Date getContextValue(IRulesRuntimeContext context) {
			    return context.getRequestDate();
			}
	
			@Override
			protected java.util.Date getPropertyValue(ITableProperties properties) {
			    return properties.getStartRequestDate();
			}
		});
		put("endRequestDate", new PropertyAccessDelegation<java.util.Date, java.util.Date>() { 
			@Override
			protected java.util.Date getContextValue(IRulesRuntimeContext context) {
			    return context.getRequestDate();
			}
	
			@Override
			protected java.util.Date getPropertyValue(ITableProperties properties) {
			    return properties.getEndRequestDate();
			}
		});
		put("caRegions", new PropertyAccessDelegation<org.openl.rules.enumeration.CaRegionsEnum[], org.openl.rules.enumeration.CaRegionsEnum>() { 
			@Override
			protected org.openl.rules.enumeration.CaRegionsEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getCaRegion();
			}
	
			@Override
			protected org.openl.rules.enumeration.CaRegionsEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getCaRegions();
			}
		});
		put("caProvinces", new PropertyAccessDelegation<org.openl.rules.enumeration.CaProvincesEnum[], org.openl.rules.enumeration.CaProvincesEnum>() { 
			@Override
			protected org.openl.rules.enumeration.CaProvincesEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getCaProvince();
			}
	
			@Override
			protected org.openl.rules.enumeration.CaProvincesEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getCaProvinces();
			}
		});
		put("country", new PropertyAccessDelegation<org.openl.rules.enumeration.CountriesEnum[], org.openl.rules.enumeration.CountriesEnum>() { 
			@Override
			protected org.openl.rules.enumeration.CountriesEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getCountry();
			}
	
			@Override
			protected org.openl.rules.enumeration.CountriesEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getCountry();
			}
		});
		put("region", new PropertyAccessDelegation<org.openl.rules.enumeration.RegionsEnum[], org.openl.rules.enumeration.RegionsEnum>() { 
			@Override
			protected org.openl.rules.enumeration.RegionsEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getRegion();
			}
	
			@Override
			protected org.openl.rules.enumeration.RegionsEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getRegion();
			}
		});
		put("currency", new PropertyAccessDelegation<org.openl.rules.enumeration.CurrenciesEnum[], org.openl.rules.enumeration.CurrenciesEnum>() { 
			@Override
			protected org.openl.rules.enumeration.CurrenciesEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getCurrency();
			}
	
			@Override
			protected org.openl.rules.enumeration.CurrenciesEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getCurrency();
			}
		});
		put("lang", new PropertyAccessDelegation<org.openl.rules.enumeration.LanguagesEnum[], org.openl.rules.enumeration.LanguagesEnum>() { 
			@Override
			protected org.openl.rules.enumeration.LanguagesEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getLang();
			}
	
			@Override
			protected org.openl.rules.enumeration.LanguagesEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getLang();
			}
		});
		put("lob", new PropertyAccessDelegation<java.lang.String, java.lang.String>() { 
			@Override
			protected java.lang.String getContextValue(IRulesRuntimeContext context) {
			    return context.getLob();
			}
	
			@Override
			protected java.lang.String getPropertyValue(ITableProperties properties) {
			    return properties.getLob();
			}
		});
		put("usregion", new PropertyAccessDelegation<org.openl.rules.enumeration.UsRegionsEnum[], org.openl.rules.enumeration.UsRegionsEnum>() { 
			@Override
			protected org.openl.rules.enumeration.UsRegionsEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getUsRegion();
			}
	
			@Override
			protected org.openl.rules.enumeration.UsRegionsEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getUsregion();
			}
		});
		put("state", new PropertyAccessDelegation<org.openl.rules.enumeration.UsStatesEnum[], org.openl.rules.enumeration.UsStatesEnum>() { 
			@Override
			protected org.openl.rules.enumeration.UsStatesEnum getContextValue(IRulesRuntimeContext context) {
			    return context.getUsState();
			}
	
			@Override
			protected org.openl.rules.enumeration.UsStatesEnum[] getPropertyValue(ITableProperties properties) {
			    return properties.getState();
			}
		});
// <<< END INSERT >>>
    }

}
