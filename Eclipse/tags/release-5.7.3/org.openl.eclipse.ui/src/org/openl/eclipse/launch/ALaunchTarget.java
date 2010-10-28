/*
 * Created on Sep 29, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.resources.IContainer;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * ALaunchTarget specifies some functionalities from ILaunchTarget - creating of
 * the default launch configuration
 *
 * @author sam
 */
public abstract class ALaunchTarget extends LaunchBase implements ILaunchTarget {
    /**
     * Creates default launch configuration that launch given target.
     */
    public ILaunchConfiguration createDefaultLaunchConfiguration(ILaunchRequest request)

    {
        try {
            // Configuration will reside in workspace meta area.
            IContainer configContainer = null;

            ILaunchConfigurationWorkingCopy wc = getLaunchManager().getLaunchConfigurationType(
                    getDefaultLaunchConfigurationTypeID()).newInstance(configContainer,
                    generateUniqueLaunchConfigurationName());

            initDefaultLaunchConfiguration(wc, request);

            return wc.doSave();

        } catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    /**
     * Returns unique configuration name for this target.
     *
     * @see org.eclipse.debug.core.ILaunchManager#generateUniqueLaunchConfigurationNameFrom(java.lang.String)
     */
    abstract public String generateUniqueLaunchConfigurationName();

    /**
     * Returns default configuration type ID for this target. Used when the new
     * default launch configuration is created. Example,
     * IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION.
     */
    abstract public String getDefaultLaunchConfigurationTypeID();

    /**
     * Set default launch parameters to launch this target.
     */
    abstract public void initDefaultLaunchConfiguration(ILaunchConfigurationWorkingCopy wc, ILaunchRequest request)
            throws Exception;

}
