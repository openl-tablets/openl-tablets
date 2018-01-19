package org.openl.rules.ruleservice.context;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/*
 * #%L
 * OpenL - RuleService - RuleService - Context
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

/*
 * Important notice:
 * If you add any methods, verify org.openl.rules.validation.properties.dimentional.DispatcherTableBuilder class works properly.
 * Refer to static initialization section were context attributes are gathered.
 * Add your method to exclusions the same way as it's done for "Object getValue(String variable)"
 */

@XmlRootElement
@XmlJavaTypeAdapter(DefaultRulesRuntimeContext.IRulesRuntimeContextAdapter.class)
public interface IRulesRuntimeContext {
    
    Object getValue(String variable);
    void setValue(String name, Object value);
    
    // <<< INSERT >>>
	java.util.Date getCurrentDate();
	void setCurrentDate(java.util.Date currentDate);	
	java.util.Date getRequestDate();
	void setRequestDate(java.util.Date requestDate);	
	java.lang.String getLob();
	void setLob(java.lang.String lob);	
	java.lang.String getNature();
	void setNature(java.lang.String nature);	
	org.openl.rules.ruleservice.context.enumeration.UsStatesEnum getUsState();
	void setUsState(org.openl.rules.ruleservice.context.enumeration.UsStatesEnum usState);	
	org.openl.rules.ruleservice.context.enumeration.CountriesEnum getCountry();
	void setCountry(org.openl.rules.ruleservice.context.enumeration.CountriesEnum country);	
	org.openl.rules.ruleservice.context.enumeration.UsRegionsEnum getUsRegion();
	void setUsRegion(org.openl.rules.ruleservice.context.enumeration.UsRegionsEnum usRegion);	
	org.openl.rules.ruleservice.context.enumeration.CurrenciesEnum getCurrency();
	void setCurrency(org.openl.rules.ruleservice.context.enumeration.CurrenciesEnum currency);	
	org.openl.rules.ruleservice.context.enumeration.LanguagesEnum getLang();
	void setLang(org.openl.rules.ruleservice.context.enumeration.LanguagesEnum lang);	
	org.openl.rules.ruleservice.context.enumeration.RegionsEnum getRegion();
	void setRegion(org.openl.rules.ruleservice.context.enumeration.RegionsEnum region);	
	org.openl.rules.ruleservice.context.enumeration.CaProvincesEnum getCaProvince();
	void setCaProvince(org.openl.rules.ruleservice.context.enumeration.CaProvincesEnum caProvince);	
	org.openl.rules.ruleservice.context.enumeration.CaRegionsEnum getCaRegion();
	void setCaRegion(org.openl.rules.ruleservice.context.enumeration.CaRegionsEnum caRegion);	
// <<< END INSERT >>>
}
