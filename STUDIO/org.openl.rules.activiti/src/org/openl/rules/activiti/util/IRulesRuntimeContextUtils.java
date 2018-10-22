package org.openl.rules.activiti.util;

import org.activiti.engine.delegate.DelegateExecution;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;

public final class IRulesRuntimeContextUtils {
    // IRulesRuntimeContextUtils converts only simple OpenL types, so we can hold it in static field.
    private static ObjectToDataOpenCastConvertor convertor = new ObjectToDataOpenCastConvertor();

    private IRulesRuntimeContextUtils() {
    }
    
    public static IRulesRuntimeContext buildRuntimeContext(DelegateExecution execution) {
        DefaultRulesRuntimeContext defaultRulesRuntimeContext = new DefaultRulesRuntimeContext();
// <<< INSERT >>>
        Object currentDate = execution.getVariable("currentDate");
        if (currentDate != null) {
            if (currentDate instanceof java.util.Date) {
                defaultRulesRuntimeContext.setValue("currentDate", currentDate);
            } else {
                IOpenCast opencast = convertor.getConvertor(java.util.Date .class, (currentDate).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(currentDate);
                    defaultRulesRuntimeContext.setValue("currentDate", o);
                }
            }
        }
        Object requestDate = execution.getVariable("requestDate");
        if (requestDate != null) {
            if (requestDate instanceof java.util.Date) {
                defaultRulesRuntimeContext.setValue("requestDate", requestDate);
            } else {
                IOpenCast opencast = convertor.getConvertor(java.util.Date .class, (requestDate).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(requestDate);
                    defaultRulesRuntimeContext.setValue("requestDate", o);
                }
            }
        }
        Object lob = execution.getVariable("lob");
        if (lob != null) {
            if (lob instanceof java.lang.String) {
                defaultRulesRuntimeContext.setValue("lob", lob);
            } else {
                IOpenCast opencast = convertor.getConvertor(java.lang.String .class, (lob).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(lob);
                    defaultRulesRuntimeContext.setValue("lob", o);
                }
            }
        }
        Object nature = execution.getVariable("nature");
        if (nature != null) {
            if (nature instanceof java.lang.String) {
                defaultRulesRuntimeContext.setValue("nature", nature);
            } else {
                IOpenCast opencast = convertor.getConvertor(java.lang.String .class, (nature).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(nature);
                    defaultRulesRuntimeContext.setValue("nature", o);
                }
            }
        }
        Object usState = execution.getVariable("usState");
        if (usState != null) {
            if (usState instanceof org.openl.rules.enumeration.UsStatesEnum) {
                defaultRulesRuntimeContext.setValue("usState", usState);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.UsStatesEnum .class, (usState).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(usState);
                    defaultRulesRuntimeContext.setValue("usState", o);
                }
            }
        }
        Object country = execution.getVariable("country");
        if (country != null) {
            if (country instanceof org.openl.rules.enumeration.CountriesEnum) {
                defaultRulesRuntimeContext.setValue("country", country);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.CountriesEnum .class, (country).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(country);
                    defaultRulesRuntimeContext.setValue("country", o);
                }
            }
        }
        Object usRegion = execution.getVariable("usRegion");
        if (usRegion != null) {
            if (usRegion instanceof org.openl.rules.enumeration.UsRegionsEnum) {
                defaultRulesRuntimeContext.setValue("usRegion", usRegion);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.UsRegionsEnum .class, (usRegion).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(usRegion);
                    defaultRulesRuntimeContext.setValue("usRegion", o);
                }
            }
        }
        Object currency = execution.getVariable("currency");
        if (currency != null) {
            if (currency instanceof org.openl.rules.enumeration.CurrenciesEnum) {
                defaultRulesRuntimeContext.setValue("currency", currency);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.CurrenciesEnum .class, (currency).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(currency);
                    defaultRulesRuntimeContext.setValue("currency", o);
                }
            }
        }
        Object lang = execution.getVariable("lang");
        if (lang != null) {
            if (lang instanceof org.openl.rules.enumeration.LanguagesEnum) {
                defaultRulesRuntimeContext.setValue("lang", lang);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.LanguagesEnum .class, (lang).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(lang);
                    defaultRulesRuntimeContext.setValue("lang", o);
                }
            }
        }
        Object region = execution.getVariable("region");
        if (region != null) {
            if (region instanceof org.openl.rules.enumeration.RegionsEnum) {
                defaultRulesRuntimeContext.setValue("region", region);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.RegionsEnum .class, (region).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(region);
                    defaultRulesRuntimeContext.setValue("region", o);
                }
            }
        }
        Object caProvince = execution.getVariable("caProvince");
        if (caProvince != null) {
            if (caProvince instanceof org.openl.rules.enumeration.CaProvincesEnum) {
                defaultRulesRuntimeContext.setValue("caProvince", caProvince);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.CaProvincesEnum .class, (caProvince).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(caProvince);
                    defaultRulesRuntimeContext.setValue("caProvince", o);
                }
            }
        }
        Object caRegion = execution.getVariable("caRegion");
        if (caRegion != null) {
            if (caRegion instanceof org.openl.rules.enumeration.CaRegionsEnum) {
                defaultRulesRuntimeContext.setValue("caRegion", caRegion);
            } else {
                IOpenCast opencast = convertor.getConvertor(org.openl.rules.enumeration.CaRegionsEnum .class, (caRegion).getClass());
                if (opencast != null) {
                    Object o = opencast.convert(caRegion);
                    defaultRulesRuntimeContext.setValue("caRegion", o);
                }
            }
        }
// <<< END INSERT >>>
        return defaultRulesRuntimeContext;
    }
}
