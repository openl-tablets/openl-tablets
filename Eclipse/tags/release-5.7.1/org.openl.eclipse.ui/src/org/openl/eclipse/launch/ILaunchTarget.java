/*
 * Created on Sep 25, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 *
 *
 * @author sam
 */
public interface ILaunchTarget {
    /**
     * Creates default launch configuration that launch given target.
     */
    public ILaunchConfiguration createDefaultLaunchConfiguration(ILaunchRequest request);

    /**
     * Returns true iff this target is launched by a given configuration.
     */
    public boolean isLaunchedBy(ILaunchConfiguration c);

}
