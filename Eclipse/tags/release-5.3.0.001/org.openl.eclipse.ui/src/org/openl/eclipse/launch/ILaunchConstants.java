/*
 * Created on Sep 26, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.openl.eclipse.util.IOpenlConstants;

/**
 *
 * @author sam
 */
public interface ILaunchConstants {
    /*
     * Main type that launch openl applications.
     */
    static public final String OPENL_MAIN_TYPE_NAME = "org.openl.main.OpenlMain";

    /*
     * Launch configuration attribute: openl file to launch
     */
    static final String ATTR_OPENL_FILE = IOpenlConstants.OPENL_PLUGIN_ID + ".OPENL_FILE";

    /**
     * Empty ILaunchConfiguration array.
     */
    static final ILaunchConfiguration[] NO_CONFIGURATIONS = {};

    /**
     * Empty ILaunchTarget array.
     */
    static final ILaunchTarget[] NO_TARGETS = {};

    /**
     * Launching messages: failed.
     */
    static final String MSG_LAUNCH_FAILED = "Launch_failed";

    static final String MSG_LAUNCHING_0 = "Launching{0}...";

    // static final String MSG_VERIFYING_LAUNCH_ATTRIBUTES =
    // "Verifying_launch_attributes...";

    // static final String MSG_CREATING_SOURCE_LOCATOR =
    // "Creating_source_locator...";

    /**
     * Title of the dialog 'Launch configuration selection'.
     */
    static final String MSG_LAUNCH_CONFIG_SELECTION_TITLE = "Launch_Configuration_Selection";

    /**
     * Message of the dialog 'Launch configuration selection'. Parameter:
     * launchMode: run, debug
     */
    static final String MSG_LAUNCH_CONFIG_SELECTION_MESSAGE = "Choose_launch_configuration_to_{0}";

    /**
     * Title of the dialog 'Launch target selection'.
     */
    static final String MSG_LAUNCH_TARGET_SELECTION_TITLE = "Launch_Target_Selection";

    /**
     * Message of the dialog 'Launch target selection'. Parameter: launchMode:
     * run, debug
     */
    static final String MSG_LAUNCH_TARGET_SELECTION_MESSAGE = "Choose_launch_target_to_{0}";

}
