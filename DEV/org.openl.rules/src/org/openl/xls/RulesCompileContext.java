package org.openl.xls;

import org.openl.impl.DefaultCompileContext;

/**
 * The current implementation of compile context used for rules projects and
 * contains part of code that is auto generated to simplify rules projects
 * configuration. Do not used this class separately.
 * 
 */
public class RulesCompileContext extends DefaultCompileContext {

    // <<< INSERT >>>
	{
        addValidator(new org.openl.rules.validation.UniquePropertyValueValidator("name"));
        addValidator(new org.openl.rules.validation.ActivePropertyValidator());
        addValidator(new org.openl.rules.validation.UniquePropertyValueValidator("id"));
        addValidator(new org.openl.rules.validation.RegexpPropertyValidator("id", "regexp:([a-zA-Z_][a-zA-Z0-9_]*)"));
        addValidator(new org.openl.rules.validation.RegexpPropertyValidator("datatypePackage", "regexp:([a-zA-Z_]{1}[a-zA-Z0-9_]*(\\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*)"));
        addValidator(new org.openl.rules.validation.RegexpPropertyValidator("precision", "regexp:(-?[0-9]+)"));
	}
// <<< END INSERT >>>
	
	// implicit validators
	{
        //addValidator(new org.openl.rules.validation.properties.dimentional.DimensionPropertiesValidator());
        addValidator(new org.openl.rules.validation.GapOverlapValidator());
        addValidator(new org.openl.rules.validation.DimentionalPropertyValidator());
//        see comment at AuxiliaryMethodsValidator          
//        addValidator(new org.openl.rules.validation.AuxiliaryMethodsValidator());
	}
}
