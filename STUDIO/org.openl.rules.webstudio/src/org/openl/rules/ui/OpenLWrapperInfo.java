package org.openl.rules.ui;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class OpenLWrapperInfo {
    String wrapperClassName;
    OpenLWebProjectInfo projectInfo;

/**
         * @param name
         * @param projectInfo
         */
    public OpenLWrapperInfo(String name, OpenLWebProjectInfo projectInfo) {
        this.wrapperClassName = name;
        this.projectInfo = projectInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the name.
     */
    public String getWrapperClassName() {
        return wrapperClassName;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name The name to set.
     */
    public void setWrapperClassName(String name) {
        this.wrapperClassName = name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the projectInfo.
     */
    public OpenLWebProjectInfo getProjectInfo() {
        return projectInfo;
    }

    /**
     * DOCUMENT ME!
     *
     * @param projectInfo The projectInfo to set.
     */
    public void setProjectInfo(OpenLWebProjectInfo projectInfo) {
        this.projectInfo = projectInfo;
    }

    public String getDisplayName() {
        return projectInfo.getDisplayName(wrapperClassName);
    }

    /**
     *
     */
    public void reset() {
        projectInfo.reset();
    }
}
