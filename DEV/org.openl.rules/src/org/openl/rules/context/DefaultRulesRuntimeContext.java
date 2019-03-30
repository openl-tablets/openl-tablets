package org.openl.rules.context;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.openl.runtime.IRuntimeContext;

public class DefaultRulesRuntimeContext implements IRulesRuntimeContext, IRulesRuntimeContextMutableUUID, Serializable {

    private static final long serialVersionUID = 670283457423670894L;

    private static final UUID NEW_INSTANCE_UUID = UUID.randomUUID(); 
    
    @XmlTransient
    UUID uuid = NEW_INSTANCE_UUID;

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

    private Map<String, Object> internalMap = new HashMap<>();

    public Object getValue(String name) {
        return internalMap.get(name);
    }

    @Override
    public String toString() {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        verbosePrint(printStream, null, internalMap, new ArrayDeque<Map<?, ?>>());

        return out.toString();
    }

    private static void verbosePrint(final PrintStream out,
            final Object label,
            final Map<?, ?> map,
            final ArrayDeque<Map<?, ?>> lineage) {
        printIndent(out, lineage.size());

        if (map == null) {
            if (label != null) {
                out.print(label);
                out.print(" = ");
            }
            out.println("null");
            return;
        }
        if (label != null) {
            out.print(label);
            out.println(" = ");
        }

        printIndent(out, lineage.size());
        out.println("{");

        lineage.push(map);

        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Object childKey = entry.getKey();
            final Object childValue = entry.getValue();
            if (childValue instanceof Map && !lineage.contains(childValue)) {
                verbosePrint(out, childKey == null ? "null" : childKey, (Map<?, ?>) childValue, lineage);
            } else {
                printIndent(out, lineage.size());
                out.print(childKey);
                out.print(" = ");

                if (!lineage.contains(childValue)) {
                    out.print(childValue);
                } else if (lineage.getFirst().equals(childValue)) {
                    out.print("(this Map)");
                } else {
                    out.print("(ancestor[?] Map)");
                }

                out.println();
            }
        }

        lineage.pop();

        printIndent(out, lineage.size());
        out.println("}");
    }

    /**
     * Writes indentation to the given stream.
     *
     * @param out the stream to indent
     */
    private static void printIndent(final PrintStream out, final int indent) {
        for (int i = 0; i < indent; i++) {
            out.print("    ");
        }
    }

    @Override
    public UUID contextUUID() {
        return uuid;
    }

    // <<< INSERT >>>
    public IRuntimeContext clone() throws CloneNotSupportedException {
        DefaultRulesRuntimeContext defaultRulesRuntimeContext = (DefaultRulesRuntimeContext) super.clone();
        defaultRulesRuntimeContext.setCurrentDate(this.currentDate);
        defaultRulesRuntimeContext.setRequestDate(this.requestDate);
        defaultRulesRuntimeContext.setLob(this.lob);
        defaultRulesRuntimeContext.setNature(this.nature);
        defaultRulesRuntimeContext.setUsState(this.usState);
        defaultRulesRuntimeContext.setCountry(this.country);
        defaultRulesRuntimeContext.setUsRegion(this.usRegion);
        defaultRulesRuntimeContext.setCurrency(this.currency);
        defaultRulesRuntimeContext.setLang(this.lang);
        defaultRulesRuntimeContext.setRegion(this.region);
        defaultRulesRuntimeContext.setCaProvince(this.caProvince);
        defaultRulesRuntimeContext.setCaRegion(this.caRegion);
        return defaultRulesRuntimeContext;
    }

    public void setValue(String name, Object value) {
        if ("currentDate".equals(name)) {
            setCurrentDate((java.util.Date)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("requestDate".equals(name)) {
            setRequestDate((java.util.Date)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("lob".equals(name)) {
            setLob((java.lang.String)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("nature".equals(name)) {
            setNature((java.lang.String)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("usState".equals(name)) {
            setUsState((org.openl.rules.enumeration.UsStatesEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("country".equals(name)) {
            setCountry((org.openl.rules.enumeration.CountriesEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("usRegion".equals(name)) {
            setUsRegion((org.openl.rules.enumeration.UsRegionsEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("currency".equals(name)) {
            setCurrency((org.openl.rules.enumeration.CurrenciesEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("lang".equals(name)) {
            setLang((org.openl.rules.enumeration.LanguagesEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("region".equals(name)) {
            setRegion((org.openl.rules.enumeration.RegionsEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("caProvince".equals(name)) {
            setCaProvince((org.openl.rules.enumeration.CaProvincesEnum)value);
            uuid = UUID.randomUUID();
            return;
        }
        if ("caRegion".equals(name)) {
            setCaRegion((org.openl.rules.enumeration.CaRegionsEnum)value);
            uuid = UUID.randomUUID();
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
        uuid = UUID.randomUUID();
    }

    private java.util.Date requestDate = null;
    public java.util.Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(java.util.Date requestDate) {
        this.requestDate = requestDate;
        internalMap.put("requestDate", requestDate);
        uuid = UUID.randomUUID();
    }

    private java.lang.String lob = null;
    public java.lang.String getLob() {
        return lob;
    }

    public void setLob(java.lang.String lob) {
        this.lob = lob;
        internalMap.put("lob", lob);
        uuid = UUID.randomUUID();
    }

    private java.lang.String nature = null;
    public java.lang.String getNature() {
        return nature;
    }

    public void setNature(java.lang.String nature) {
        this.nature = nature;
        internalMap.put("nature", nature);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.UsStatesEnum usState = null;
    public org.openl.rules.enumeration.UsStatesEnum getUsState() {
        return usState;
    }

    public void setUsState(org.openl.rules.enumeration.UsStatesEnum usState) {
        this.usState = usState;
        internalMap.put("usState", usState);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.CountriesEnum country = null;
    public org.openl.rules.enumeration.CountriesEnum getCountry() {
        return country;
    }

    public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
        this.country = country;
        internalMap.put("country", country);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.UsRegionsEnum usRegion = null;
    public org.openl.rules.enumeration.UsRegionsEnum getUsRegion() {
        return usRegion;
    }

    public void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion) {
        this.usRegion = usRegion;
        internalMap.put("usRegion", usRegion);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.CurrenciesEnum currency = null;
    public org.openl.rules.enumeration.CurrenciesEnum getCurrency() {
        return currency;
    }

    public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency) {
        this.currency = currency;
        internalMap.put("currency", currency);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.LanguagesEnum lang = null;
    public org.openl.rules.enumeration.LanguagesEnum getLang() {
        return lang;
    }

    public void setLang(org.openl.rules.enumeration.LanguagesEnum lang) {
        this.lang = lang;
        internalMap.put("lang", lang);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.RegionsEnum region = null;
    public org.openl.rules.enumeration.RegionsEnum getRegion() {
        return region;
    }

    public void setRegion(org.openl.rules.enumeration.RegionsEnum region) {
        this.region = region;
        internalMap.put("region", region);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.CaProvincesEnum caProvince = null;
    public org.openl.rules.enumeration.CaProvincesEnum getCaProvince() {
        return caProvince;
    }

    public void setCaProvince(org.openl.rules.enumeration.CaProvincesEnum caProvince) {
        this.caProvince = caProvince;
        internalMap.put("caProvince", caProvince);
        uuid = UUID.randomUUID();
    }

    private org.openl.rules.enumeration.CaRegionsEnum caRegion = null;
    public org.openl.rules.enumeration.CaRegionsEnum getCaRegion() {
        return caRegion;
    }

    public void setCaRegion(org.openl.rules.enumeration.CaRegionsEnum caRegion) {
        this.caRegion = caRegion;
        internalMap.put("caRegion", caRegion);
        uuid = UUID.randomUUID();
    }

// <<< END INSERT >>>
}
