package org.openl.types;

/**
 * Provides the possibility to add the name of the module to which this object belongs to.
 */
public interface IModuleInfo {

    /**
     * Get the name of the module to which given object belongs to
     *
     * @return the name of the module to which given object belongs to
     */
    String getModuleName();
}
