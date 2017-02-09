package org.openl.rules.maven.gen;

/**
 * This interface should be implemented by any class whose instances are
 * intended to return some String object by calling
 * {@link #getInitialization(Object)} that represents code for initializing the
 * income value.
 * 
 * @author DLiauchuk
 * 
 */
public interface TypeInitializationWriter {
    String getInitialization(Object value);
}
