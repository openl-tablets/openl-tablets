package org.openl.rules.serialization.jackson.org.openl.rules.context;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Custom mapping for {@link IRulesRuntimeContext}.
 * 
 * @author Marat Kamalov
 */
@JsonDeserialize(as=DefaultRulesRuntimeContext.class)
public class IRulesRuntimeContextType {
}
