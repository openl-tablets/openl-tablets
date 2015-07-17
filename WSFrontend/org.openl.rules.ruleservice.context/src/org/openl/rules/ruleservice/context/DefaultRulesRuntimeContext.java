package org.openl.rules.ruleservice.context;

/*
 * #%L
 * OpenL - RuleService - RuleService - Context
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DefaultRulesRuntimeContext implements IRulesRuntimeContext {

    public static class IRulesRuntimeContextAdapter extends XmlAdapter<DefaultRulesRuntimeContext, IRulesRuntimeContext> {
        @Override
        public DefaultRulesRuntimeContext marshal(IRulesRuntimeContext v) throws Exception {
            // *TODO
            return (DefaultRulesRuntimeContext) v;
        }

        @Override
        public IRulesRuntimeContext unmarshal(DefaultRulesRuntimeContext v) throws Exception {
            return v;
        }
    }
    
    private Map<String, Object> internalMap = new HashMap<String, Object>();

    public void setValue(String name, Object value) {
        internalMap.put(name, value);
    }

    public Object getValue(String name) {
        return internalMap.get(name);
    }

    // <<< INSERT >>>
	public java.util.Date getCurrentDate() {
		return (java.util.Date) getValue("currentDate"); 
	}
	public void setCurrentDate(java.util.Date currentDate) {
		setValue("currentDate", currentDate);
	}	
	public java.util.Date getRequestDate() {
		return (java.util.Date) getValue("requestDate"); 
	}
	public void setRequestDate(java.util.Date requestDate) {
		setValue("requestDate", requestDate);
	}	
	public java.lang.String getLob() {
		return (java.lang.String) getValue("lob"); 
	}
	public void setLob(java.lang.String lob) {
		setValue("lob", lob);
	}	
	public org.openl.rules.ruleservice.context.enumeration.UsStatesEnum getUsState() {
		return (org.openl.rules.ruleservice.context.enumeration.UsStatesEnum) getValue("usState"); 
	}
	public void setUsState(org.openl.rules.ruleservice.context.enumeration.UsStatesEnum usState) {
		setValue("usState", usState);
	}	
	public org.openl.rules.ruleservice.context.enumeration.CountriesEnum getCountry() {
		return (org.openl.rules.ruleservice.context.enumeration.CountriesEnum) getValue("country"); 
	}
	public void setCountry(org.openl.rules.ruleservice.context.enumeration.CountriesEnum country) {
		setValue("country", country);
	}	
	public org.openl.rules.ruleservice.context.enumeration.UsRegionsEnum getUsRegion() {
		return (org.openl.rules.ruleservice.context.enumeration.UsRegionsEnum) getValue("usRegion"); 
	}
	public void setUsRegion(org.openl.rules.ruleservice.context.enumeration.UsRegionsEnum usRegion) {
		setValue("usRegion", usRegion);
	}	
	public org.openl.rules.ruleservice.context.enumeration.CurrenciesEnum getCurrency() {
		return (org.openl.rules.ruleservice.context.enumeration.CurrenciesEnum) getValue("currency"); 
	}
	public void setCurrency(org.openl.rules.ruleservice.context.enumeration.CurrenciesEnum currency) {
		setValue("currency", currency);
	}	
	public org.openl.rules.ruleservice.context.enumeration.LanguagesEnum getLang() {
		return (org.openl.rules.ruleservice.context.enumeration.LanguagesEnum) getValue("lang"); 
	}
	public void setLang(org.openl.rules.ruleservice.context.enumeration.LanguagesEnum lang) {
		setValue("lang", lang);
	}	
	public org.openl.rules.ruleservice.context.enumeration.RegionsEnum getRegion() {
		return (org.openl.rules.ruleservice.context.enumeration.RegionsEnum) getValue("region"); 
	}
	public void setRegion(org.openl.rules.ruleservice.context.enumeration.RegionsEnum region) {
		setValue("region", region);
	}	
	public org.openl.rules.ruleservice.context.enumeration.CaProvincesEnum getCaProvince() {
		return (org.openl.rules.ruleservice.context.enumeration.CaProvincesEnum) getValue("caProvince"); 
	}
	public void setCaProvince(org.openl.rules.ruleservice.context.enumeration.CaProvincesEnum caProvince) {
		setValue("caProvince", caProvince);
	}	
	public org.openl.rules.ruleservice.context.enumeration.CaRegionsEnum getCaRegion() {
		return (org.openl.rules.ruleservice.context.enumeration.CaRegionsEnum) getValue("caRegion"); 
	}
	public void setCaRegion(org.openl.rules.ruleservice.context.enumeration.CaRegionsEnum caRegion) {
		setValue("caRegion", caRegion);
	}	
// <<< END INSERT >>>
}
