package org.openl.rules.context;

/**
 * Provides user runtime context.
 */
public interface IRulesRuntimeContextProvider {

    /**
     * Gets runtime context.
     *
     * @return {@link IRulesRuntimeContext} instance
     */
    IRulesRuntimeContext getRuntimeContext();
}
