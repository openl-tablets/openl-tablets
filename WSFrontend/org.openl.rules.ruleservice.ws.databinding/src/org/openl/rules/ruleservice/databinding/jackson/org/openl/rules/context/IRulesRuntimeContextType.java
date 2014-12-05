package org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.context;

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
@JsonDeserialize(as=DefaultRulesRuntimeContext.class)
public class IRulesRuntimeContextType {
}
