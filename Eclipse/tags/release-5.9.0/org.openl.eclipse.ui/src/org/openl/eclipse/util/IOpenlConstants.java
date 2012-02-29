/*
 * Created on Jul 9, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.util;

import org.openl.eclipse.base.OpenlBasePlugin;

/**
 * Openl plugin constants.
 *
 * @author sam
 *
 */
public interface IOpenlConstants {

    /**
     * The identifier for the OpenL plug-in. Must match the plugin id defined in
     * plugin.xml!
     */
    public static final String OPENL_PLUGIN_ID = OpenlBasePlugin.PLUGIN_ID;

    /**
     * The identifier for the OpenL nature. The presence of this nature on a
     * project indicates that it is OpenL - capable.
     *
     * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
     */
    public static final String OPENL_NATURE_ID = "org.openl.eclipse.ui.OpenlNature";
    public static final String OLD_OPENL_NATURE_ID = "org.openl.base.nature";

    /**
     * The identifier for the OpenL builder.
     */
    public static final String OPENL_BUILDER_NAME = OPENL_PLUGIN_ID + ".openlbuilder";

    /**
     * The identifier for the OpenL editor.
     */
    public static final String OPENL_EDITOR_ID = "org.eclipse.odt.ui.editor.OpenlEditor";

}
