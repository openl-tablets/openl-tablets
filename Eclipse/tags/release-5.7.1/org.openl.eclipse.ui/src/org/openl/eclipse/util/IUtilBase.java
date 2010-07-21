/*
 * Created on Aug 28, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import org.eclipse.core.runtime.CoreException;
import org.openl.syntax.exception.SyntaxNodeException;
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
     * Shortcut for one argument getFormattedString(formatKey, new Object[]{ arg
     * }).
     */
    public String getFormattedString(String key, Object arg);

    /**
     * Aka MessageFormat.format() but with localized formatKey.
     */
    public String getFormattedString(String formatKey, Object[] args);

    /**
     * Generic message extractor.
     */
    public String getMessage(SyntaxNodeException error);

    /**
     * Generic message extractor.
     */
    public String getMessage(Throwable t);

    /**
     * Returns localized string from the resource bundle for a given key.
     * Returns the key itself if it is not in the resource bundle.
     */
    public String getString(String key);

    /**
     * Generic exception handler.
     */
    public CoreException handleException(String message);

    /**
     * Generic exception handler.
     */
    public CoreException handleException(Throwable t);

}