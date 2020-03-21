package org.openl.rules.context;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenMethod;

public class DefaultRulesRuntimeContext implements IRulesRuntimeContext, IRulesRuntimeContextOptimizationForOpenMethodDispatcher, Serializable {

    private static final long serialVersionUID = 670283457423670894L;

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

    @Override
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

    private transient Map<IOpenMethod, IOpenMethod> cache = null;

    @Override
    public IOpenMethod getMethodForOpenMethodDispatcher(OpenMethodDispatcher openMethodDispatcher) {
        if (cache == null) {
            return null;
        }
        return cache.get(openMethodDispatcher);
    }

    @Override
    public void putMethodForOpenMethodDispatcher(OpenMethodDispatcher openMethodDispatcher, IOpenMethod method) {
        if (cache == null) {
            cache = new HashMap<>();
        }
        cache.put(openMethodDispatcher, method);
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
            out.print("null\r\n");
            return;
        }
        if (label != null) {
            out.print(label);
            out.print(" = \r\n");
        }

        printIndent(out, lineage.size());
        out.print("{\r\n");

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

                out.print("\r\n");
            }
        }

        lineage.pop();

        printIndent(out, lineage.size());
        out.print("}\r\n");
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

    // <<< INSERT >>>
    @Override
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

    @Override
    public void setValue(String name, Object value) {
        if ("currentDate".equals(name)) {
            setCurrentDate((java.util.Date) value);
            return;
        }
        if ("requestDate".equals(name)) {
            setRequestDate((java.util.Date) value);
            return;
        }
        if ("lob".equals(name)) {
            setLob((java.lang.String) value);
            return;
        }
        if ("nature".equals(name)) {
            setNature((java.lang.String) value);
            return;
        }
        if ("usState".equals(name)) {
            setUsState((org.openl.rules.enumeration.UsStatesEnum) value);
            return;
        }
        if ("country".equals(name)) {
            setCountry((org.openl.rules.enumeration.CountriesEnum) value);
            return;
        }
        if ("usRegion".equals(name)) {
            setUsRegion((org.openl.rules.enumeration.UsRegionsEnum) value);
            return;
        }
        if ("currency".equals(name)) {
            setCurrency((org.openl.rules.enumeration.CurrenciesEnum) value);
            return;
        }
        if ("lang".equals(name)) {
            setLang((org.openl.rules.enumeration.LanguagesEnum) value);
            return;
        }
        if ("region".equals(name)) {
            setRegion((org.openl.rules.enumeration.RegionsEnum) value);
            return;
        }
        if ("caProvince".equals(name)) {
            setCaProvince((org.openl.rules.enumeration.CaProvincesEnum) value);
            return;
        }
        if ("caRegion".equals(name)) {
            setCaRegion((org.openl.rules.enumeration.CaRegionsEnum) value);
            return;
        }
    }

    private java.util.Date currentDate = null;

    @Override
    public java.util.Date getCurrentDate() {
        return currentDate;
    }

    @Override
    public void setCurrentDate(java.util.Date currentDate) {
        this.currentDate = currentDate;
        internalMap.put("currentDate", currentDate);
        cache = null;
    }

    private java.util.Date requestDate = null;

    @Override
    public java.util.Date getRequestDate() {
        return requestDate;
    }

    @Override
    public void setRequestDate(java.util.Date requestDate) {
        this.requestDate = requestDate;
        internalMap.put("requestDate", requestDate);
        cache = null;
    }

    private java.lang.String lob = null;

    @Override
    public java.lang.String getLob() {
        return lob;
    }

    @Override
    public void setLob(java.lang.String lob) {
        this.lob = lob;
        internalMap.put("lob", lob);
        cache = null;
    }

    private java.lang.String nature = null;

    @Override
    public java.lang.String getNature() {
        return nature;
    }

    @Override
    public void setNature(java.lang.String nature) {
        this.nature = nature;
        internalMap.put("nature", nature);
        cache = null;
    }

    private org.openl.rules.enumeration.UsStatesEnum usState = null;

    @Override
    public org.openl.rules.enumeration.UsStatesEnum getUsState() {
        return usState;
    }

    @Override
    public void setUsState(org.openl.rules.enumeration.UsStatesEnum usState) {
        this.usState = usState;
        internalMap.put("usState", usState);
        cache = null;
    }

    private org.openl.rules.enumeration.CountriesEnum country = null;

    @Override
    public org.openl.rules.enumeration.CountriesEnum getCountry() {
        return country;
    }

    @Override
    public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
        this.country = country;
        internalMap.put("country", country);
        cache = null;
    }

    private org.openl.rules.enumeration.UsRegionsEnum usRegion = null;

    @Override
    public org.openl.rules.enumeration.UsRegionsEnum getUsRegion() {
        return usRegion;
    }

    @Override
    public void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion) {
        this.usRegion = usRegion;
        internalMap.put("usRegion", usRegion);
        cache = null;
    }

    private org.openl.rules.enumeration.CurrenciesEnum currency = null;

    @Override
    public org.openl.rules.enumeration.CurrenciesEnum getCurrency() {
        return currency;
    }

    @Override
    public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency) {
        this.currency = currency;
        internalMap.put("currency", currency);
        cache = null;
    }

    private org.openl.rules.enumeration.LanguagesEnum lang = null;

    @Override
    public org.openl.rules.enumeration.LanguagesEnum getLang() {
        return lang;
    }

    @Override
    public void setLang(org.openl.rules.enumeration.LanguagesEnum lang) {
        this.lang = lang;
        internalMap.put("lang", lang);
        cache = null;
    }

    private org.openl.rules.enumeration.RegionsEnum region = null;

    @Override
    public org.openl.rules.enumeration.RegionsEnum getRegion() {
        return region;
    }

    @Override
    public void setRegion(org.openl.rules.enumeration.RegionsEnum region) {
        this.region = region;
        internalMap.put("region", region);
        cache = null;
    }

    private org.openl.rules.enumeration.CaProvincesEnum caProvince = null;

    @Override
    public org.openl.rules.enumeration.CaProvincesEnum getCaProvince() {
        return caProvince;
    }

    @Override
    public void setCaProvince(org.openl.rules.enumeration.CaProvincesEnum caProvince) {
        this.caProvince = caProvince;
        internalMap.put("caProvince", caProvince);
        cache = null;
    }

    private org.openl.rules.enumeration.CaRegionsEnum caRegion = null;

    @Override
    public org.openl.rules.enumeration.CaRegionsEnum getCaRegion() {
        return caRegion;
    }

    @Override
    public void setCaRegion(org.openl.rules.enumeration.CaRegionsEnum caRegion) {
        this.caRegion = caRegion;
        internalMap.put("caRegion", caRegion);
        cache = null;
    }

    public static final Map<String, Class<?>> CONTEXT_PROPERTIES;

    static {
        Map<String, Class<?>> contextFields = new TreeMap<>();
        contextFields.put("currentDate", java.util.Date.class);
        contextFields.put("requestDate", java.util.Date.class);
        contextFields.put("lob", java.lang.String.class);
        contextFields.put("nature", java.lang.String.class);
        contextFields.put("usState", org.openl.rules.enumeration.UsStatesEnum.class);
        contextFields.put("country", org.openl.rules.enumeration.CountriesEnum.class);
        contextFields.put("usRegion", org.openl.rules.enumeration.UsRegionsEnum.class);
        contextFields.put("currency", org.openl.rules.enumeration.CurrenciesEnum.class);
        contextFields.put("lang", org.openl.rules.enumeration.LanguagesEnum.class);
        contextFields.put("region", org.openl.rules.enumeration.RegionsEnum.class);
        contextFields.put("caProvince", org.openl.rules.enumeration.CaProvincesEnum.class);
        contextFields.put("caRegion", org.openl.rules.enumeration.CaRegionsEnum.class);
        CONTEXT_PROPERTIES = Collections.unmodifiableMap(contextFields);
    }
    // <<< END INSERT >>>

}
