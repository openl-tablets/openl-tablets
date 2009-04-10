/*
 * Created on Aug 9, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchManager;
import org.openl.util.IOpenIterator;
import org.openl.util.ISelector;

/**
 * Base for the launch classes.
 *
 * @author sam
 */
public interface ILaunchBase extends ILaunchConstants {

    /**
     * Returns all ILaunchConfiguration's of the current ILaunchManager.
     */
    public IOpenIterator getLaunchConfigurations() throws CoreException;

    /**
     * Convenience method to get the launch manager.
     */
    public ILaunchManager getLaunchManager();

    /**
     * Selector <code>launch-configuration-supports-mode</code>. Mode: run,
     * debug.
     */
    public ISelector launchConfigurationSupportsMode(String mode);

    /**
     * Examples: - No_configurations_to_run - No_configurations_to_debug
     */
    public String MSG_NO_CONFIGURATIONS_TO_LAUNCH(ILaunchRequest selection);

    /**
     * Examples: - No_targets_in_active_editor_to_run -
     * No_targets_in_selection_to_debug
     */
    public String MSG_NO_TARGETS_TO_LAUNCH(ILaunchRequest selection);

    /**
     * Convenience method to generate unique launch configuration name.
     */
    public String uniqueLCName(String prefix);
}
