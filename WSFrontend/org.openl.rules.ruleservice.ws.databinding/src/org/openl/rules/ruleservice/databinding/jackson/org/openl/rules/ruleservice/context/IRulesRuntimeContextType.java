package org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.ruleservice.context;

import org.openl.rules.context.IRulesRuntimeContext;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

/**
 * Custom mapping for {@link IRulesRuntimeContext}.
 * 
 * @author Marat Kamalov
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class IRulesRuntimeContextType {
    @JsonCreator
    public static org.openl.rules.ruleservice.context.IRulesRuntimeContext makeInstanceIRulesRuntimeContext() {
        return new org.openl.rules.ruleservice.context.DefaultRulesRuntimeContext();
    }
}
