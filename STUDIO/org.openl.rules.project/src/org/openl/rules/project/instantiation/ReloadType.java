package org.openl.rules.project.instantiation;

/**
 * Type of reloading of WebStudio.
 *
 * @author PUdalau
 */
public enum ReloadType {
    NO, // Simple reload without reloading of wrappers
    SINGLE, // Only for current project: reload wrapper but with using of old
    // class loader
    RELOAD, // Usual reload: reload wrapper but with using of old class loader
    // and clear all compiled wrappers from cache
    FORCED, // Forced reloading with new class loader after reload and clear all
    // compiled wrappers from cache
}
