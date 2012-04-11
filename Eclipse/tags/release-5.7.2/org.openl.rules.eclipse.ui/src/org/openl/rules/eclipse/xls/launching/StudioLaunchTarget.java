/*
 * Created on Sep 29, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.eclipse.xls.launching;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.openl.eclipse.launch.ALaunchTarget;
import org.openl.eclipse.launch.ILaunchRequest;
import org.openl.eclipse.launch.OpenlLaunchTarget;
import org.openl.rules.lang.xls.main.IRulesLaunchConstants;

/**
 *
 * @author sam
 */
public class StudioLaunchTarget extends ALaunchTarget {

    static public final String STUDIO_PROJECT_NAME = "org.openl.rules.webstudio";

    static public final String MAIN_CLASS_NAME = "org.openl.rules.webstudio.util.StartTomcat";

    static public final String VM_ARGS = "-Xms256M -Xmx1024M -D" + IRulesLaunchConstants.START_PROJECT_PROPERTY_NAME
            + "=";

    private IResource resource;

    OpenlLaunchTarget module;

    public StudioLaunchTarget(IResource source) {
        resource = source;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StudioLaunchTarget)) {
            return false;
        }

        StudioLaunchTarget that = (StudioLaunchTarget) o;

        return resource.equals(that.resource);
    }

    @Override
    public String generateUniqueLaunchConfigurationName() {
        String prefix = "Open " + resource.getName() + " in WebStudio";
        return getLaunchManager().generateUniqueLaunchConfigurationNameFrom(prefix);
    }

    @Override
    public String getDefaultLaunchConfigurationTypeID() {
        return IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION;
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public void initDefaultLaunchConfiguration(ILaunchConfigurationWorkingCopy wc, ILaunchRequest request)
            throws Exception {
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, MAIN_CLASS_NAME);

        // wc.setAttribute(
        // IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, main
        // .makeArgs());

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, makeVMArgs());

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, STUDIO_PROJECT_NAME);

    }

    /**
     * Studio is launched by configuration and methodName is in args.
     */
    public boolean isLaunchedBy(ILaunchConfiguration c) {
        try {

            // String project = resource.getProject().getName();
            String attrMain = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
            String attrProject = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
            String vmArgs = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
            // String attrArgs =
            // c.getAttribute(
            // IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
            // "");

            return STUDIO_PROJECT_NAME.equals(attrProject) && MAIN_CLASS_NAME.equals(attrMain)
                    && vmArgs.contains(makeVMArgs());
        } catch (Exception ex) {
            return false;
        }
    }

    private String makeVMArgs() {
        return VM_ARGS + resource.getName();
    }

    @Override
    public String toString() {
        return "Launch " + resource.toString();
    }

}
