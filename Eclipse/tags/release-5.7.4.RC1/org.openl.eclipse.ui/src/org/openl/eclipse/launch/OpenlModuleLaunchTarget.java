/*
 * Created on Sep 29, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.openl.main.OpenlMain;

/**
 *
 * @author sam
 */
public class OpenlModuleLaunchTarget extends ALaunchTarget {

    OpenlLaunchTarget module;

    String methodName;

    public OpenlModuleLaunchTarget(IResource source, String methodName) {
        module = new OpenlLaunchTarget(source);
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenlModuleLaunchTarget)) {
            return false;
        }

        OpenlModuleLaunchTarget that = (OpenlModuleLaunchTarget) o;

        return module.equals(that.module) && methodName.equals(that.methodName);
    }

    @Override
    public String generateUniqueLaunchConfigurationName() {
        return module.generateUniqueLaunchConfigurationName();
    }

    @Override
    public String getDefaultLaunchConfigurationTypeID() {
        return module.getDefaultLaunchConfigurationTypeID();
    }

    @Override
    public int hashCode() {
        return module.hashCode() + methodName.hashCode();
    }

    @Override
    public void initDefaultLaunchConfiguration(ILaunchConfigurationWorkingCopy wc, ILaunchRequest request)
            throws Exception {
        module.initDefaultLaunchConfiguration(wc, request);

        OpenlMain main = new OpenlMain(module.getOpenlName());

        module.setOpenlMainArgs(main);
        main.methodName = methodName;

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, main.makeArgs());
    }

    /**
     * Module is launched by configuration and methodName is in args.
     */
    public boolean isLaunchedBy(ILaunchConfiguration c) {
        if (!module.isLaunchedBy(c)) {
            return false;
        }

        try {
            String attrArgs = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");

            return attrArgs.indexOf(" " + methodName + " ") >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return module.toString() + methodName + "()";
    }

}
