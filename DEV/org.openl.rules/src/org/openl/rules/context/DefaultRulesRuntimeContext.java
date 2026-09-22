package org.openl.rules.context;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import lombok.Getter;

import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;

public class DefaultRulesRuntimeContext implements IRulesRuntimeContext, IRulesRuntimeContextOptimizationForOpenMethodDispatcher, Serializable {

    private static final String CURRENT_DATE_KEY = "currentDate";
    private static final String REQUEST_DATE_KEY = "requestDate";
    private static final String LOB_KEY = "lob";
    private static final String NATURE_KEY = "nature";
    private static final String US_STATE_KEY = "usState";
    private static final String COUNTRY_KEY = "country";
    private static final String US_REGION_KEY = "usRegion";
    private static final String CURRENCY_KEY = "currency";
    private static final String LANG_KEY = "lang";
    private static final String REGION_KEY = "region";
    private static final String CA_PROVINCE_KEY = "caProvince";
    private static final String CA_REGION_KEY = "caRegion";
    private static final String LOCALE_KEY = "locale";

    @Serial
    private static final long serialVersionUID = 670283457423670894L;

    public static class IRulesRuntimeContextAdapter extends XmlAdapter<DefaultRulesRuntimeContext, IRulesRuntimeContext> {
        @Override
        public DefaultRulesRuntimeContext marshal(IRulesRuntimeContext v) {
            // *TODO
            return (DefaultRulesRuntimeContext) v;
        }

        @Override
        public IRulesRuntimeContext unmarshal(DefaultRulesRuntimeContext v) {
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

        var out = new ByteArrayOutputStream();
        var printStream = new PrintStream(out);
        verbosePrint(printStream, null, internalMap, new ArrayDeque<>());

        return out.toString();
    }

    private transient Map<IOpenMethod, IOpenMethod> cache;

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
            final var childKey = entry.getKey();
            final var childValue = entry.getValue();
            if (childValue instanceof Map<?, ?> map1 && !lineage.contains(childValue)) {
                verbosePrint(out, childKey == null ? "null" : childKey, map1, lineage);
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
        for (var i = 0; i < indent; i++) {
            out.print("    ");
        }
    }

    // <<< INSERT >>>

    /**
     * The default implementation Object.clone() method returns a Shallow Copy.
     * <p>
     * In shallow copy, if the field value is a primitive type, it copies its value; otherwise,
     * if the field value is a reference to an object, it copies the reference, hence referring to the same object.
     * Now, if one of these objects is modified, the change is visible in the other.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#clone()">Object#clone()</a>
     * @see <a href="https://en.wikipedia.org/wiki/Clone_(Java_method)">Clone (Java_method)</a>
     */
    @Override
    public IRulesRuntimeContext clone() throws CloneNotSupportedException {
        var defaultRulesRuntimeContext = (DefaultRulesRuntimeContext) super.clone();
        // create a new instance of `Hashmap`. By default clone creates a shallow copy in defaultRulesRuntimeContext.
        defaultRulesRuntimeContext.internalMap = new HashMap<>(this.internalMap);
        return defaultRulesRuntimeContext;
    }

    @Override
    public void setValue(String name, Object value) {
        if (CURRENT_DATE_KEY.equals(name)) {
            setCurrentDate((java.util.Date) value);
            return;
        }
        if (REQUEST_DATE_KEY.equals(name)) {
            setRequestDate((java.util.Date) value);
            return;
        }
        if (LOB_KEY.equals(name)) {
            setLob((java.lang.String) value);
            return;
        }
        if (NATURE_KEY.equals(name)) {
            setNature((java.lang.String) value);
            return;
        }
        if (US_STATE_KEY.equals(name)) {
            setUsState((org.openl.rules.enumeration.UsStatesEnum) value);
            return;
        }
        if (COUNTRY_KEY.equals(name)) {
            setCountry((org.openl.rules.enumeration.CountriesEnum) value);
            return;
        }
        if (US_REGION_KEY.equals(name)) {
            setUsRegion((org.openl.rules.enumeration.UsRegionsEnum) value);
            return;
        }
        if (CURRENCY_KEY.equals(name)) {
            setCurrency((org.openl.rules.enumeration.CurrenciesEnum) value);
            return;
        }
        if (LANG_KEY.equals(name)) {
            setLang((org.openl.rules.enumeration.LanguagesEnum) value);
            return;
        }
        if (REGION_KEY.equals(name)) {
            setRegion((org.openl.rules.enumeration.RegionsEnum) value);
            return;
        }
        if (CA_PROVINCE_KEY.equals(name)) {
            setCaProvince((org.openl.rules.enumeration.CaProvincesEnum) value);
            return;
        }
        if (CA_REGION_KEY.equals(name)) {
            setCaRegion((org.openl.rules.enumeration.CaRegionsEnum) value);
            return;
        }
        if (LOCALE_KEY.equals(name)) {
            setLocale((java.util.Locale) value);
            return;
        }
    }

    @Getter
    private java.util.Date currentDate;

    @Override
    public void setCurrentDate(java.util.Date currentDate) {
        this.currentDate = currentDate;
        internalMap.put(CURRENT_DATE_KEY, currentDate);
        cache = null;
    }

    @Getter
    private java.util.Date requestDate;

    @Override
    public void setRequestDate(java.util.Date requestDate) {
        this.requestDate = requestDate;
        internalMap.put(REQUEST_DATE_KEY, requestDate);
        cache = null;
    }

    @Getter
    private java.lang.String lob;

    @Override
    public void setLob(java.lang.String lob) {
        this.lob = lob;
        internalMap.put(LOB_KEY, lob);
        cache = null;
    }

    @Getter
    private java.lang.String nature;

    @Override
    public void setNature(java.lang.String nature) {
        this.nature = nature;
        internalMap.put(NATURE_KEY, nature);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.UsStatesEnum usState;

    @Override
    public void setUsState(org.openl.rules.enumeration.UsStatesEnum usState) {
        this.usState = usState;
        internalMap.put(US_STATE_KEY, usState);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.CountriesEnum country;

    @Override
    public void setCountry(org.openl.rules.enumeration.CountriesEnum country) {
        this.country = country;
        internalMap.put(COUNTRY_KEY, country);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.UsRegionsEnum usRegion;

    @Override
    public void setUsRegion(org.openl.rules.enumeration.UsRegionsEnum usRegion) {
        this.usRegion = usRegion;
        internalMap.put(US_REGION_KEY, usRegion);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.CurrenciesEnum currency;

    @Override
    public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum currency) {
        this.currency = currency;
        internalMap.put(CURRENCY_KEY, currency);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.LanguagesEnum lang;

    @Override
    public void setLang(org.openl.rules.enumeration.LanguagesEnum lang) {
        this.lang = lang;
        internalMap.put(LANG_KEY, lang);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.RegionsEnum region;

    @Override
    public void setRegion(org.openl.rules.enumeration.RegionsEnum region) {
        this.region = region;
        internalMap.put(REGION_KEY, region);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.CaProvincesEnum caProvince;

    @Override
    public void setCaProvince(org.openl.rules.enumeration.CaProvincesEnum caProvince) {
        this.caProvince = caProvince;
        internalMap.put(CA_PROVINCE_KEY, caProvince);
        cache = null;
    }

    @Getter
    private org.openl.rules.enumeration.CaRegionsEnum caRegion;

    @Override
    public void setCaRegion(org.openl.rules.enumeration.CaRegionsEnum caRegion) {
        this.caRegion = caRegion;
        internalMap.put(CA_REGION_KEY, caRegion);
        cache = null;
    }

    @Getter
    private java.util.Locale locale;

    @Override
    public void setLocale(java.util.Locale locale) {
        this.locale = locale;
        internalMap.put(LOCALE_KEY, locale);
        cache = null;
    }

    public static final Map<String, Class<?>> CONTEXT_PROPERTIES;

    static {
        var contextFields = new TreeMap<String, Class<?>>();
        contextFields.put(CURRENT_DATE_KEY, java.util.Date.class);
        contextFields.put(REQUEST_DATE_KEY, java.util.Date.class);
        contextFields.put(LOB_KEY, java.lang.String.class);
        contextFields.put(NATURE_KEY, java.lang.String.class);
        contextFields.put(US_STATE_KEY, org.openl.rules.enumeration.UsStatesEnum.class);
        contextFields.put(COUNTRY_KEY, org.openl.rules.enumeration.CountriesEnum.class);
        contextFields.put(US_REGION_KEY, org.openl.rules.enumeration.UsRegionsEnum.class);
        contextFields.put(CURRENCY_KEY, org.openl.rules.enumeration.CurrenciesEnum.class);
        contextFields.put(LANG_KEY, org.openl.rules.enumeration.LanguagesEnum.class);
        contextFields.put(REGION_KEY, org.openl.rules.enumeration.RegionsEnum.class);
        contextFields.put(CA_PROVINCE_KEY, org.openl.rules.enumeration.CaProvincesEnum.class);
        contextFields.put(CA_REGION_KEY, org.openl.rules.enumeration.CaRegionsEnum.class);
        contextFields.put(LOCALE_KEY, java.util.Locale.class);
        CONTEXT_PROPERTIES = Collections.unmodifiableMap(contextFields);
    }
    // <<< END INSERT >>>

}
