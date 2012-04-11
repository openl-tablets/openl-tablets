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
	public java.lang.String getLob() {
		return (java.lang.String) internalMap.get("lob"); 
	}
	public void setLob(java.lang.String lob) {
		internalMap.put("lob", lob);
	}	
	public java.lang.String getUsRegion() {
		return (java.lang.String) internalMap.get("usRegion"); 
	}
	public void setUsRegion(java.lang.String usRegion) {
		internalMap.put("usRegion", usRegion);
	}	
	public java.lang.String getUsState() {
		return (java.lang.String) internalMap.get("usState"); 
	}
	public void setUsState(java.lang.String usState) {
		internalMap.put("usState", usState);
	}	
	public org.openl.rules.enumeration.CountriesEnum getCountry() {
		return (org.openl.rules.enumeration.CountriesEnum) internalMap.get("country"); 
	}
	public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
		internalMap.put("country", country);
	}	
	// <<< END INSERT >>>
}
