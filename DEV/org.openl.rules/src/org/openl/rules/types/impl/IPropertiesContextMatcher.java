package org.openl.rules.types.impl;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.properties.ITableProperties;

public interface IPropertiesContextMatcher {

    /**
     * Matcher compares a single Table property and returns one of the 3 values NO_MATCH, if a property does not match
     * with a context variable MATCH_BY_DEFAULT, for example, context has value for LOB, but properties have empty value
     * MATCH
     *
     * @param propertyName
     * @param tableProperties
     * @param context
     * @return
     */
    MatchingResult match(String propertyName, ITableProperties tableProperties, IRulesRuntimeContext context);

    /**
     * Adds a new constraint dynamically to the existing matcher, if constraint already exists, it overrides it
     *
     * @param propertyName
     * @param ctr
     */
    void addConstraint(String propertyName, MatchingConstraint<?, ?> ctr);

}
