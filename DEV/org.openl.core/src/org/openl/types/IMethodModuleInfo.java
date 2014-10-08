package org.openl.types;

/**
 * Provides the possibility to add the name of the module to which this method
 * belongs to.
 */
public interface IMethodModuleInfo {

    /**
     * Get the name of the module to which given method belongs to
     *
     * @return the name of the module to which given method belongs to
     */
    String getModuleName();
}
