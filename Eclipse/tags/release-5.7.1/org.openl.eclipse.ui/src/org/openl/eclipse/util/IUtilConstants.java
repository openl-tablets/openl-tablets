/*
 * Created on Aug 6, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import org.eclipse.core.resources.IProject;

/**
 * @author sam
 *
 */
public interface IUtilConstants {
    /**
     * Default name for the resource bundle.
     */
    static public final String DEFAULT_RESOURCE_BUNDLE_NAME = "Messages";

    /**
     * Predefined Eclipse file: ".project"
     */
    static public final String _PROJECT = ".project";

    /**
     * Predefined Eclipse file: ".classpath"
     */
    static public final String _CLASSPATH = ".classpath";

    /**
     * URL protocol: "file:"
     */
    static public final String FILE_PROTOCOL = "file:";

    /**
     * Used to indicate that required message missed.
     */
    static public final String EMPTY_MESSAGE = "EMPTY_MESSAGE";

    /**
     * Empty IProject array.
     */
    static public final IProject[] NO_PROJECTS = {};

}
