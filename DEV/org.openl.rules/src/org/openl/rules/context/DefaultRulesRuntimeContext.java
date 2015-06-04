package org.openl.rules.context;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.collections4.MapUtils;
import org.openl.runtime.IRuntimeContext;

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

    public Object getValue(String name) {
        return internalMap.get(name);
    }

    @Override
    public String toString() {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        MapUtils.verbosePrint(printStream, null, internalMap);

        return out.toString();
    }

    // <<< INSERT >>>
    public IRuntimeContext clone() throws CloneNotSupportedException {
        DefaultRulesRuntimeContext defaultRulesRuntimeContext = (DefaultRulesRuntimeContext) super.clone();
        defaultRulesRuntimeContext.setCurrentDate(this.currentDate);
        defaultRulesRuntimeContext.setRequestDate(this.requestDate);
        defaultRulesRuntimeContext.setLob(this.lob);
        defaultRulesRuntimeContext.setUsState(this.usState);
        defaultRulesRuntimeContext.setCountry(this.country);
        defaultRulesRuntimeContext.setUsRegion(this.usRegion);
        defaultRulesRuntimeContext.setCurrency(this.currency);
        defaultRulesRuntimeContext.setLang(this.lang);
        defaultRulesRuntimeContext.setRegion(this.region);
        return defaultRulesRuntimeContext;
    }

    public void setValue(String name, Object value){
        if ("currentDate".equals(name)){
            setCurrentDate((java.util.Date)value);
            return;
        }
        if ("requestDate".equals(name)){
            setRequestDate((java.util.Date)value);
            return;
        }
        if ("lob".equals(name)){
            setLob((java.lang.String)value);
            return;
        }
        if ("usState".equals(name)){
            setUsState((org.openl.rules.enumeration.UsStatesEnum)value);
            return;
        }
        if ("country".equals(name)){
            setCountry((org.openl.rules.enumeration.CountriesEnum)value);
            return;
        }
        if ("usRegion".equals(name)){
            setUsRegion((org.openl.rules.enumeration.UsRegionsEnum)value);
            return;
        }
        if ("currency".equals(name)){
            setCurrency((org.openl.rules.enumeration.CurrenciesEnum)value);
            return;
        }
        if ("lang".equals(name)){
            setLang((org.openl.rules.enumeration.LanguagesEnum)value);
            return;
        }
        if ("region".equals(name)){
            setRegion((org.openl.rules.enumeration.RegionsEnum)value);
            return;
        }
    }

    private java.util.Date currentDate = null;
    public java.util.Date getCurrentDate() {
        return currentDate;
    }
    public void setCurrentDate(java.util.Date currentDate) {
        this.currentDate = currentDate;
        internalMap.put("currentDate", currentDate);
    }
        
    private java.util.Date requestDate = null;
    public java.util.Date getRequestDate() {
        return requestDate;
    }
    public void setRequestDate(java.util.Date requestDate) {
        this.requestDate = requestDate;
        internalMap.put("requestDate", requestDate);
    }
        
    private java.lang.String lob = null;
    public java.lang.String getLob() {
        return lob;
    }
    public void setLob(java.lang.String lob) {
        this.lob = lob;
        internalMap.put("lob", lob);
    }
        
    private org.openl.rules.enumeration.UsStatesEnum usState = null;
    public org.openl.rules.enumeration.UsStatesEnum getUsState() {
        return usState;
    }
    public void setUsState(org.openl.rules.enumeration.UsStatesEnum usState) {
        this.usState = usState;
        internalMap.put("usState", usState);
    }
        
    private org.openl.rules.enumeration.CountriesEnum country = null;
    public org.openl.rules.enumeration.CountriesEnum getCountry() {
        return country;
    }
    public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
        this.country = country;
        internalMap.put("country", country);
    }
        
    private org.openl.rules.enumeration.UsRegionsEnum usRegion = null;
    public org.openl.rules.enumeration.UsRegionsEnum getUsRegion() {
        return usRegion;
    }
    public void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion) {
        this.usRegion = usRegion;
        internalMap.put("usRegion", usRegion);
    }
        
    private org.openl.rules.enumeration.CurrenciesEnum currency = null;
    public org.openl.rules.enumeration.CurrenciesEnum getCurrency() {
        return currency;
    }
    public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency) {
        this.currency = currency;
        internalMap.put("currency", currency);
    }
        
    private org.openl.rules.enumeration.LanguagesEnum lang = null;
    public org.openl.rules.enumeration.LanguagesEnum getLang() {
        return lang;
    }
    public void setLang(org.openl.rules.enumeration.LanguagesEnum lang) {
        this.lang = lang;
        internalMap.put("lang", lang);
    }
        
    private org.openl.rules.enumeration.RegionsEnum region = null;
    public org.openl.rules.enumeration.RegionsEnum getRegion() {
        return region;
    }
    public void setRegion(org.openl.rules.enumeration.RegionsEnum region) {
        this.region = region;
        internalMap.put("region", region);
    }
        
// <<< END INSERT >>>
}
