/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.rules.ui;

/**
 * @author snshor
 *
 */
public class OpenLWrapperInfo 
{
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
	 * @return Returns the name.
	 */
	public String getWrapperClassName() {
		return wrapperClassName;
	}

	/**
	 * @param name The name to set.
	 */
	public void setWrapperClassName(String name) {
		this.wrapperClassName = name;
	}

	/**
	 * @return Returns the projectInfo.
	 */
	public OpenLWebProjectInfo getProjectInfo() {
		return projectInfo;
	}

	/**
	 * @param projectInfo The projectInfo to set.
	 */
	public void setProjectInfo(OpenLWebProjectInfo projectInfo) {
		this.projectInfo = projectInfo;
	}
	
	
	public String getDisplayName()
	{
		return projectInfo.getDisplayName(wrapperClassName);
	}

	/**
	 * 
	 */
	public void reset()
	{
		projectInfo.reset();
	}

}
