/*
 * Created on Sep 29, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.openl.eclipse.builder.OpenlBuilder;
import org.openl.eclipse.util.UrlUtil;
import org.openl.main.OpenlMain;

/**
 *
 * @author sam
 */
public class OpenlLaunchTarget extends ALaunchTarget {
    protected IResource source;

    public OpenlLaunchTarget(IResource source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenlLaunchTarget)) {
            return false;
        }

        OpenlLaunchTarget that = (OpenlLaunchTarget) o;

        return source.equals(that.source);
    }

    @Override
    public String generateUniqueLaunchConfigurationName() {
        String prefix = source.getName();
        return getLaunchManager().generateUniqueLaunchConfigurationNameFrom(prefix);
    }

    public String getCanonicalURL(IResource resource) {
        String url = resource.getLocation().toString();
        return UrlUtil.toCanonicalUrl(url);
    }

    @Override
    public String getDefaultLaunchConfigurationTypeID() {
        return IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION;
    }

    public String getJavaVMArgs() {
        StringBuffer s = new StringBuffer();

        // pass configuration as -D (System.properties)
        // Properties properties =
        // getOpenlIdeExtensionManager().getAllOpenlExtensionsProperties();

        Properties properties = new Properties();

        for (Enumeration it = properties.propertyNames(); it.hasMoreElements();) {
            String key = (String) it.nextElement();
            String value = properties.getProperty(key);
            s.append(" -D").append(key).append("=").append(value);
        }

        return s.toString();
    }

    public String getOpenlName() {
        return OpenlBuilder.getOpenlName(getSourceFileName());
    }

    public String getSourceFileName() {
        String sourceFileName = getCanonicalURL(source);

        return sourceFileName;
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    /**
     * Set launch parameters to launch target with OpenlMain.
     */
    @Override
    public void initDefaultLaunchConfiguration(ILaunchConfigurationWorkingCopy wc, ILaunchRequest request)
            throws Exception {
        OpenlMain main = new OpenlMain(getOpenlName());

        setOpenlMainArgs(main);

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, main.getClass().getName());

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, main.makeArgs());

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, getJavaVMArgs());

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, source.getProject().getName());

        // wc.setAttribute(
        // ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID,
        // JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
        //
        // wc.setAttribute(
        // IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE,
        // IDebugUIConstants.PERSPECTIVE_DEFAULT);
        //
        // wc.setAttribute(
        // IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE,
        // IDebugUIConstants.PERSPECTIVE_DEFAULT);
    }

    /**
     * Launching determinators: - attrProject is my Project - attrMain is
     * OpenlMain - attrArgs contains sourceUrl
     */
    public boolean isLaunchedBy(ILaunchConfiguration c) {
        try {
            String project = source.getProject().getName();
            String sourceUrl = getCanonicalURL(source);

            String attrMain = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
            String attrProject = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
            String attrArgs = c.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");

            return project.equals(attrProject) && OpenlMain.class.getName().equals(attrMain)
                    && attrArgs.indexOf(sourceUrl) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void setOpenlMainArgs(OpenlMain main) {
        main.sourceFileName = getSourceFileName();
    }

    @Override
    public String toString() {
        return source.toString();
    }

}
