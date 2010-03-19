/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

/**
 * @author snshor
 *
 */
public class OpenLWrapperInfo {
    
    private String wrapperClassName;

    private OpenLWebProjectInfo projectInfo;

    public OpenLWrapperInfo(String name, OpenLWebProjectInfo projectInfo) {
        wrapperClassName = name;
        this.projectInfo = projectInfo;
    }

    public String getDisplayName() {
        return projectInfo.getDisplayName(wrapperClassName);
    }

    /**
     * @return Returns the projectInfo.
     */
    public OpenLWebProjectInfo getProjectInfo() {
        return projectInfo;
    }

    /**
     * @return Returns the name.
     */
    public String getWrapperClassName() {
        return wrapperClassName;
    }

    /**
     *
     */
    public void reset() {
        projectInfo.reset();
    }

    /**
     * @param projectInfo The projectInfo to set.
     */
    public void setProjectInfo(OpenLWebProjectInfo projectInfo) {
        this.projectInfo = projectInfo;
    }

    /**
     * @param name The name to set.
     */
    public void setWrapperClassName(String name) {
        wrapperClassName = name;
    }

}
