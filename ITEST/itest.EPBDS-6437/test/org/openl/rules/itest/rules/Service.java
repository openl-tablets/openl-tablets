package org.openl.rules.itest.rules;

import org.openl.generated.beans.Vehicle;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;

import java.util.Date;

public interface Service {
    Date parseDate(IRulesRuntimeContext runtimeContext, Vehicle v);
    VariationsResult<Date> parseDate(IRulesRuntimeContext runtimeContext, Vehicle v,
            VariationsPack variationPack);

    Date nowDate(IRulesRuntimeContext runtimeContext);
    VariationsResult<Date> nowDate(IRulesRuntimeContext runtimeContext,
            VariationsPack variationPack);

    Integer nowTimestamp(IRulesRuntimeContext runtimeContext);
    VariationsResult<Integer> nowTimestamp(IRulesRuntimeContext runtimeContext,
            VariationsPack variationPack);

    String checkRulesModule(IRulesRuntimeContext runtimeContext);
    VariationsResult<String> checkRulesModule(IRulesRuntimeContext runtimeContext,
            VariationsPack variationPack);

    Integer calVehicleYear(IRulesRuntimeContext runtimeContext, Vehicle v);

    VariationsResult<Integer> calVehicleYear(IRulesRuntimeContext runtimeContext, Vehicle v,
            VariationsPack variationPack);
}