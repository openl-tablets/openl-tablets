package org.openl.rules.types.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.properties.ITableProperties;

public class DefaultPropertiesContextMatcher implements IPropertiesContextMatcher {

    private Map<String, MatchingConstraint<?, ?>> constraints = new HashMap<String, MatchingConstraint<?, ?>>();

    public DefaultPropertiesContextMatcher() {
        initilaize();
    }

    public MatchingResult match(String propName, ITableProperties props, IRulesRuntimeContext context) {
        MatchingConstraint<?, ?> mc = constraints.get(propName);

        if (mc == null)
            throw new RuntimeException("Unexpectedly could not find a constarint for the property: " + propName);

        return mc.match(props, context);
    }

    public void addConstraint(String propertyName, MatchingConstraint<?, ?> ctr) {
        constraints.put(propertyName, ctr);
    }

    protected void initilaize() {
        // <<< INSERT >>>
        constraints.put("effectiveDate", new MatchingConstraint<java.util.Date, java.util.Date>() {

            @Override
            protected java.util.Date getContextValue(IRulesRuntimeContext context) {
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
            protected java.util.Date getContextValue(IRulesRuntimeContext context) {
                return context.getCurrentDate();
            }

            @Override
            protected java.util.Date getPropertyValue(ITableProperties properties) {
                return properties.getExpirationDate();
            }

            @Override
            protected boolean matchNotNulls(java.util.Date propertyValue, java.util.Date contextValue) {
                return GE(propertyValue, contextValue);
            }

        });
        constraints.put("startRequestDate", new MatchingConstraint<java.util.Date, java.util.Date>() {

            @Override
            protected java.util.Date getContextValue(IRulesRuntimeContext context) {
                return context.getRequestDate();
            }

            @Override
            protected java.util.Date getPropertyValue(ITableProperties properties) {
                return properties.getStartRequestDate();
            }

            @Override
            protected boolean matchNotNulls(java.util.Date propertyValue, java.util.Date contextValue) {
                return LE(propertyValue, contextValue);
            }

        });
        constraints.put("endRequestDate", new MatchingConstraint<java.util.Date, java.util.Date>() {

            @Override
            protected java.util.Date getContextValue(IRulesRuntimeContext context) {
                return context.getRequestDate();
            }

            @Override
            protected java.util.Date getPropertyValue(ITableProperties properties) {
                return properties.getEndRequestDate();
            }

            @Override
            protected boolean matchNotNulls(java.util.Date propertyValue, java.util.Date contextValue) {
                return GE(propertyValue, contextValue);
            }

        });
        constraints.put("caRegions", new MatchingConstraint<org.openl.rules.enumeration.CaRegionsEnum[], org.openl.rules.enumeration.CaRegionsEnum>() {

            @Override
            protected org.openl.rules.enumeration.CaRegionsEnum getContextValue(IRulesRuntimeContext context) {
                return context.getCaRegion();
            }

            @Override
            protected org.openl.rules.enumeration.CaRegionsEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getCaRegions();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.CaRegionsEnum[] propertyValue, org.openl.rules.enumeration.CaRegionsEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("caProvinces", new MatchingConstraint<org.openl.rules.enumeration.CaProvincesEnum[], org.openl.rules.enumeration.CaProvincesEnum>() {

            @Override
            protected org.openl.rules.enumeration.CaProvincesEnum getContextValue(IRulesRuntimeContext context) {
                return context.getCaProvince();
            }

            @Override
            protected org.openl.rules.enumeration.CaProvincesEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getCaProvinces();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.CaProvincesEnum[] propertyValue, org.openl.rules.enumeration.CaProvincesEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("country", new MatchingConstraint<org.openl.rules.enumeration.CountriesEnum[], org.openl.rules.enumeration.CountriesEnum>() {

            @Override
            protected org.openl.rules.enumeration.CountriesEnum getContextValue(IRulesRuntimeContext context) {
                return context.getCountry();
            }

            @Override
            protected org.openl.rules.enumeration.CountriesEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getCountry();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.CountriesEnum[] propertyValue, org.openl.rules.enumeration.CountriesEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("region", new MatchingConstraint<org.openl.rules.enumeration.RegionsEnum[], org.openl.rules.enumeration.RegionsEnum>() {

            @Override
            protected org.openl.rules.enumeration.RegionsEnum getContextValue(IRulesRuntimeContext context) {
                return context.getRegion();
            }

            @Override
            protected org.openl.rules.enumeration.RegionsEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getRegion();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.RegionsEnum[] propertyValue, org.openl.rules.enumeration.RegionsEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("currency", new MatchingConstraint<org.openl.rules.enumeration.CurrenciesEnum[], org.openl.rules.enumeration.CurrenciesEnum>() {

            @Override
            protected org.openl.rules.enumeration.CurrenciesEnum getContextValue(IRulesRuntimeContext context) {
                return context.getCurrency();
            }

            @Override
            protected org.openl.rules.enumeration.CurrenciesEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getCurrency();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.CurrenciesEnum[] propertyValue, org.openl.rules.enumeration.CurrenciesEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("lang", new MatchingConstraint<org.openl.rules.enumeration.LanguagesEnum[], org.openl.rules.enumeration.LanguagesEnum>() {

            @Override
            protected org.openl.rules.enumeration.LanguagesEnum getContextValue(IRulesRuntimeContext context) {
                return context.getLang();
            }

            @Override
            protected org.openl.rules.enumeration.LanguagesEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getLang();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.LanguagesEnum[] propertyValue, org.openl.rules.enumeration.LanguagesEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("lob", new MatchingConstraint<java.lang.String[], java.lang.String>() {

            @Override
            protected java.lang.String getContextValue(IRulesRuntimeContext context) {
                return context.getLob();
            }

            @Override
            protected java.lang.String[] getPropertyValue(ITableProperties properties) {
                return properties.getLob();
            }

            @Override
            protected boolean matchNotNulls(java.lang.String[] propertyValue, java.lang.String contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("usregion", new MatchingConstraint<org.openl.rules.enumeration.UsRegionsEnum[], org.openl.rules.enumeration.UsRegionsEnum>() {

            @Override
            protected org.openl.rules.enumeration.UsRegionsEnum getContextValue(IRulesRuntimeContext context) {
                return context.getUsRegion();
            }

            @Override
            protected org.openl.rules.enumeration.UsRegionsEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getUsregion();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.UsRegionsEnum[] propertyValue, org.openl.rules.enumeration.UsRegionsEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("state", new MatchingConstraint<org.openl.rules.enumeration.UsStatesEnum[], org.openl.rules.enumeration.UsStatesEnum>() {

            @Override
            protected org.openl.rules.enumeration.UsStatesEnum getContextValue(IRulesRuntimeContext context) {
                return context.getUsState();
            }

            @Override
            protected org.openl.rules.enumeration.UsStatesEnum[] getPropertyValue(ITableProperties properties) {
                return properties.getState();
            }

            @Override
            protected boolean matchNotNulls(org.openl.rules.enumeration.UsStatesEnum[] propertyValue, org.openl.rules.enumeration.UsStatesEnum contextValue) {
                return CONTAINS(propertyValue, contextValue);
            }

        });
        constraints.put("nature", new MatchingConstraint<java.lang.String, java.lang.String>() {

            @Override
            protected java.lang.String getContextValue(IRulesRuntimeContext context) {
                return context.getNature();
            }

            @Override
            protected java.lang.String getPropertyValue(ITableProperties properties) {
                return properties.getNature();
            }

            @Override
            protected boolean matchNotNulls(java.lang.String propertyValue, java.lang.String contextValue) {
                return EQ(propertyValue, contextValue);
            }

        });
// <<< END INSERT >>>
    }

}
