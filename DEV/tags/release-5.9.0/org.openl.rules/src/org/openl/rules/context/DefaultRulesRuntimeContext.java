package org.openl.rules.context;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.openl.runtime.IRuntimeContext;

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

    public IRuntimeContext clone() throws CloneNotSupportedException{
        return (IRuntimeContext)super.clone();
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
	public org.openl.rules.enumeration.UsStatesEnum getUsState() {
		return (org.openl.rules.enumeration.UsStatesEnum) getValue("usState"); 
	}
	public void setUsState(org.openl.rules.enumeration.UsStatesEnum usState) {
		setValue("usState", usState);
	}	
	public org.openl.rules.enumeration.CountriesEnum getCountry() {
		return (org.openl.rules.enumeration.CountriesEnum) getValue("country"); 
	}
	public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
		setValue("country", country);
	}	
	public org.openl.rules.enumeration.UsRegionsEnum getUsRegion() {
		return (org.openl.rules.enumeration.UsRegionsEnum) getValue("usRegion"); 
	}
	public void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion) {
		setValue("usRegion", usRegion);
	}	
	public org.openl.rules.enumeration.CurrenciesEnum getCurrency() {
		return (org.openl.rules.enumeration.CurrenciesEnum) getValue("currency"); 
	}
	public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency) {
		setValue("currency", currency);
	}	
	public org.openl.rules.enumeration.LanguagesEnum getLang() {
		return (org.openl.rules.enumeration.LanguagesEnum) getValue("lang"); 
	}
	public void setLang(org.openl.rules.enumeration.LanguagesEnum lang) {
		setValue("lang", lang);
	}	
	public org.openl.rules.enumeration.RegionsEnum getRegion() {
		return (org.openl.rules.enumeration.RegionsEnum) getValue("region"); 
	}
	public void setRegion(org.openl.rules.enumeration.RegionsEnum region) {
		setValue("region", region);
	}	
	// <<< END INSERT >>>
}
