package org.openl.rules.project.instantiation;

/**
 * Type of reloading of WebStudio.
 * 
 * @author PUdalau
 */
public enum ReloadType {
    NO, // Simple reload without reloading of wrapper
    RELOAD, // Usual reload: reload wrapper but with using of old class loader
    FORCED, // Forced reloading with new class loader after reload
}
