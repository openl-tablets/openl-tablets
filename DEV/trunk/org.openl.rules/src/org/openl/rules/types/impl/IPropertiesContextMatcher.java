package org.openl.rules.types.impl;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.table.properties.ITableProperties;

public interface IPropertiesContextMatcher {

    public enum MatchingResult {
        NO_MATCH, MATCH_BY_DEFAULT, MATCH;
    }


    /**
     * Matcher compares a single Table property and returns one of the 3 values
     * NO_MATCH, if a property does not match with a context variable
     * MATCH_BY_DEFAULT, for example, context has value for LOB, but properties have empty value
     * MATCH
     * 
     * @param propName
     * @param props
     * @param context
     * @return
     */
    MatchingResult match(String propName, ITableProperties props, IRulesContext context);
    
    
 }
