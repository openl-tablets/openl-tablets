/**
 * 
 */
package com.exigen.le.repository;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;


/**
 * @author vabramovs
 *
 */
public interface Repository {
	
	
	/**
	 * Initialize LiveExcel repository
	 * @param prop
	 */
	public void init(Properties prop);
	/**
	 * get InputStream with desired version of head Excel file for LiveExcel project
	 * @param project
	 * @param version
	 * @return
	 */
	public InputStream getExcel(String project,VersionDesc version);  
	/**
	 * get InputStream with desired version of child Excel file for LiveExcel project
	 * @param project
	 * @param version
	 * @return
	 */
	public InputStream getExcel(String project,VersionDesc version,String excelFile);  
	/**
	 * get InputStream with desired version any file for LiveExcel project
	 * @param project
	 * @param version
	 * @param file
	 * @return
	 */
	public InputStream getProjectElement(String project,VersionDesc version,String file);  
	/**
	 * get InputStream with desired version of xml file for LiveExcel project
	 * @param project
	 * @param version
	 * @return
	 */
	public InputStream getMappingXML(String project,VersionDesc version);  
	/**
	 * Set version as default for this project
	 * @param project
	 * @param version
	 */
	public void setVersionAsDefault(String project,VersionDesc version);

	/**
	 * Get default VersionDesc for project
	 * @param project
	 * @param date 
	 * @return
	 */
	public VersionDesc getDefaultVersionDesc(String project, Date date);
	/**
	 * Get latest VersionDesc for project
	 * @param project
	 * @return
	 */
	public VersionDesc getLatestVersionDesc(String project);
	
	/**
	 * Resolve given version
	 * @param project
	 * @param version
	 * @return
	 */
	public VersionDesc resolveVersion(String project, VersionDesc version);
	
	/**
	 * Get list of project, registered in Repository
	 * @return
	 */
	public List<RepositoryNodeDesc> getProjectList();
	/**
	 * Get list of project's version
	 * @param projectName
	 * @return
	 */
	public List<RepositoryNodeDesc> getVersionList(String projectName);
	
	
	/**
	 * Get list of version descriptions, sorted by activation date (newest - first)
	 * @param project
	 * @return
	 */
	public  List<VersionDesc> getActivationSchedule(String project);
	
	/**
	 * Set activation Date for version
	 * @param project
	 * @param version
	 * @param activationDate
	 */
	public void setActivationDate(String project, VersionDesc version,
			Date activationDate); 

	/**
	 * get actual Properties set
	 * @return
	 */
	public Properties getProperties();
	
	/**
	 * @param project
	 * @return
	 */
	public boolean  isActivationDateSupport(String project);
	
	/**
	 * Retrieve list of Project Elements
	 * @param project
	 * @param version
	 * @return
	 */
	public List<ProjectElement> retrieveElementList(String project, VersionDesc version);
	/**
	 * Is project in repository?
	 * @param projectName
	 * @return
	 */
	public boolean isProject(String projectName); 
	/**
	 * Is version in repository?
	 * @param projectName
	 * @return
	 */
	public boolean isVersion(String projectName,VersionDesc versionDesc); 
}
