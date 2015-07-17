package org.openl.rules.ruleservice.context;

import org.openl.rules.context.RulesRuntimeContextFactory;

public class RuntimeContextConvertor {
 // <<< INSERT >>>
    private static org.openl.rules.enumeration.UsStatesEnum convertUsState(org.openl.rules.ruleservice.context.enumeration.UsStatesEnum usState) {
		if (usState == null) {
			return null;
		}
    	return org.openl.rules.enumeration.UsStatesEnum.fromString(usState.toString());
    }
    
    private static org.openl.rules.enumeration.CountriesEnum convertCountry(org.openl.rules.ruleservice.context.enumeration.CountriesEnum country) {
		if (country == null) {
			return null;
		}
    	return org.openl.rules.enumeration.CountriesEnum.fromString(country.toString());
    }
    
    private static org.openl.rules.enumeration.UsRegionsEnum convertUsRegion(org.openl.rules.ruleservice.context.enumeration.UsRegionsEnum usRegion) {
		if (usRegion == null) {
			return null;
		}
    	return org.openl.rules.enumeration.UsRegionsEnum.fromString(usRegion.toString());
    }
    
    private static org.openl.rules.enumeration.CurrenciesEnum convertCurrency(org.openl.rules.ruleservice.context.enumeration.CurrenciesEnum currency) {
		if (currency == null) {
			return null;
		}
    	return org.openl.rules.enumeration.CurrenciesEnum.fromString(currency.toString());
    }
    
    private static org.openl.rules.enumeration.LanguagesEnum convertLang(org.openl.rules.ruleservice.context.enumeration.LanguagesEnum lang) {
		if (lang == null) {
			return null;
		}
    	return org.openl.rules.enumeration.LanguagesEnum.fromString(lang.toString());
    }
    
    private static org.openl.rules.enumeration.RegionsEnum convertRegion(org.openl.rules.ruleservice.context.enumeration.RegionsEnum region) {
		if (region == null) {
			return null;
		}
    	return org.openl.rules.enumeration.RegionsEnum.fromString(region.toString());
    }
    
    private static org.openl.rules.enumeration.CaProvincesEnum convertCaProvince(org.openl.rules.ruleservice.context.enumeration.CaProvincesEnum caProvince) {
		if (caProvince == null) {
			return null;
		}
    	return org.openl.rules.enumeration.CaProvincesEnum.fromString(caProvince.toString());
    }
    
    private static org.openl.rules.enumeration.CaRegionsEnum convertCaRegion(org.openl.rules.ruleservice.context.enumeration.CaRegionsEnum caRegion) {
		if (caRegion == null) {
			return null;
		}
    	return org.openl.rules.enumeration.CaRegionsEnum.fromString(caRegion.toString());
    }
    
	public static org.openl.rules.context.IRulesRuntimeContext covert(org.openl.rules.ruleservice.context.IRulesRuntimeContext context) {
	    org.openl.rules.context.IRulesRuntimeContext rulesRuntimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
		rulesRuntimeContext.setCurrentDate(context.getCurrentDate());
		rulesRuntimeContext.setRequestDate(context.getRequestDate());
		rulesRuntimeContext.setLob(context.getLob());
		rulesRuntimeContext.setUsState(convertUsState(context.getUsState()));
		rulesRuntimeContext.setCountry(convertCountry(context.getCountry()));
		rulesRuntimeContext.setUsRegion(convertUsRegion(context.getUsRegion()));
		rulesRuntimeContext.setCurrency(convertCurrency(context.getCurrency()));
		rulesRuntimeContext.setLang(convertLang(context.getLang()));
		rulesRuntimeContext.setRegion(convertRegion(context.getRegion()));
		rulesRuntimeContext.setCaProvince(convertCaProvince(context.getCaProvince()));
		rulesRuntimeContext.setCaRegion(convertCaRegion(context.getCaRegion()));
		return rulesRuntimeContext;
	}
// <<< END INSERT >>>
}
