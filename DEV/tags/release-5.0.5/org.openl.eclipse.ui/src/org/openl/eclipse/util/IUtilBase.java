/*
 * Created on Aug 28, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import org.eclipse.core.runtime.CoreException;
import org.openl.syntax.ISyntaxError;
import org.openl.util.ASelector;
import org.openl.util.ISelector;

/**
 * Write once - use everywhere :)
 * 
 * @author sam
 */
public interface IUtilBase extends IUtilConstants {

    static public final ISelector NOT_NULLS = new ASelector() {
        public boolean select(Object o) {
            return o != null;
        }
    };

    static public final ISelector FALSE_SELECTOR = new ASelector() {
        public boolean select(Object o) {
            return false;
        }
    };

    /**
     * Returns localized string from the resource bundle for a given key.
     * Returns the key itself if it is not in the resource bundle.
     */
    public String getString(String key);

    /**
     * Aka MessageFormat.format() but with localized formatKey.
     */
    public String getFormattedString(String formatKey, Object[] args);

    /**
     * Shortcut for one argument getFormattedString(formatKey, new Object[]{ arg
     * }).
     */
    public String getFormattedString(String key, Object arg);

    /**
     * Generic exception handler.
     */
    public CoreException handleException(Throwable t);

    /**
     * Generic exception handler.
     */
    public CoreException handleException(String message);

    /**
     * Generic message extractor.
     */
    public String getMessage(ISyntaxError error);

    /**
     * Generic message extractor.
     */
    public String getMessage(Throwable t);

}