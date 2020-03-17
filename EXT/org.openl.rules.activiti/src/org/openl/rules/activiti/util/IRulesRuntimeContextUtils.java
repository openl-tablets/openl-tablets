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

    private static void populate(IRulesRuntimeContext context, DelegateExecution execution, String propName, Class<?> propType) {
        Object currentDate = execution.getVariable(propName);
        if (currentDate != null) {
            if (propType.isInstance(currentDate)) {
                context.setValue(propName, currentDate);
            } else {
                IOpenCast opencast = convertor.getConvertor(propType, currentDate.getClass());
                if (opencast != null) {
                    Object o = opencast.convert(currentDate);
                    context.setValue(propName, o);
                }
            }
        }
    }

    public static IRulesRuntimeContext buildRuntimeContext(DelegateExecution execution) {
        DefaultRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        // <<< INSERT >>>
        populate(context, execution, "currentDate", java.util.Date.class);
        populate(context, execution, "requestDate", java.util.Date.class);
        populate(context, execution, "lob", java.lang.String.class);
        populate(context, execution, "nature", java.lang.String.class);
        populate(context, execution, "usState", org.openl.rules.enumeration.UsStatesEnum.class);
        populate(context, execution, "country", org.openl.rules.enumeration.CountriesEnum.class);
        populate(context, execution, "usRegion", org.openl.rules.enumeration.UsRegionsEnum.class);
        populate(context, execution, "currency", org.openl.rules.enumeration.CurrenciesEnum.class);
        populate(context, execution, "lang", org.openl.rules.enumeration.LanguagesEnum.class);
        populate(context, execution, "region", org.openl.rules.enumeration.RegionsEnum.class);
        populate(context, execution, "caProvince", org.openl.rules.enumeration.CaProvincesEnum.class);
        populate(context, execution, "caRegion", org.openl.rules.enumeration.CaRegionsEnum.class);
        // <<< END INSERT >>>
        return context;
    }
}
