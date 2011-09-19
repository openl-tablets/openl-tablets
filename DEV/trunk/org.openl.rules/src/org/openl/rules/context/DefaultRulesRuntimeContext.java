package org.openl.rules.context;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

public class DefaultRulesRuntimeContext implements IRulesRuntimeContext {

    private Map<String, Object> internalMap = new HashMap<String, Object>();

    public void setValue(String name, Object value) {
        internalMap.put(name, value);
    }

    public Object getValue(String name) {
        return internalMap.get(name);
    }

    @Override
    public synchronized String toString() {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        MapUtils.verbosePrint(printStream, null, internalMap);

        return out.toString();
    }

    // <<< INSERT >>>
	public java.util.Date getCurrentDate() {
		return (java.util.Date) internalMap.get("currentDate"); 
	}
	public void setCurrentDate(java.util.Date currentDate) {
		internalMap.put("currentDate", currentDate);
	}	
	public java.util.Date getRequestDate() {
		return (java.util.Date) internalMap.get("requestDate"); 
	}
	public void setRequestDate(java.util.Date requestDate) {
		internalMap.put("requestDate", requestDate);
	}	
	public java.lang.String getLob() {
		return (java.lang.String) internalMap.get("lob"); 
	}
	public void setLob(java.lang.String lob) {
		internalMap.put("lob", lob);
	}	
	public org.openl.rules.enumeration.UsStatesEnum getUsState() {
		return (org.openl.rules.enumeration.UsStatesEnum) internalMap.get("usState"); 
	}
	public void setUsState(org.openl.rules.enumeration.UsStatesEnum usState) {
		internalMap.put("usState", usState);
	}	
	public org.openl.rules.enumeration.CountriesEnum getCountry() {
		return (org.openl.rules.enumeration.CountriesEnum) internalMap.get("country"); 
	}
	public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
		internalMap.put("country", country);
	}	
	public org.openl.rules.enumeration.UsRegionsEnum getUsRegion() {
		return (org.openl.rules.enumeration.UsRegionsEnum) internalMap.get("usRegion"); 
	}
	public void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion) {
		internalMap.put("usRegion", usRegion);
	}	
	public org.openl.rules.enumeration.CurrenciesEnum getCurrency() {
		return (org.openl.rules.enumeration.CurrenciesEnum) internalMap.get("currency"); 
	}
	public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency) {
		internalMap.put("currency", currency);
	}	
	public org.openl.rules.enumeration.LanguagesEnum getLang() {
		return (org.openl.rules.enumeration.LanguagesEnum) internalMap.get("lang"); 
	}
	public void setLang(org.openl.rules.enumeration.LanguagesEnum lang) {
		internalMap.put("lang", lang);
	}	
	public org.openl.rules.enumeration.RegionsEnum getRegion() {
		return (org.openl.rules.enumeration.RegionsEnum) internalMap.get("region"); 
	}
	public void setRegion(org.openl.rules.enumeration.RegionsEnum region) {
		internalMap.put("region", region);
	}	
	// <<< END INSERT >>>
}
