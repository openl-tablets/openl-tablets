/**
 * 
 */
package com.exigen.le.repository;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;





/**
 * @author vabramovs
 *
 */
public class RepositoryFactory  implements Repository {
	private static final Log LOG = LogFactory.getLog(RepositoryFactory.class);
	
	
	
	static private final RepositoryFactory INSTANCE = new RepositoryFactory();
	
	static private Repository repository = new RepositoryFileImpl();
	static private boolean initialized = false;
	private RepositoryFactory(){
	}
	public static  RepositoryFactory getInstance(){
		return INSTANCE;
	}
	public static  Repository getRepository(){
		return repository;
	}
	
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#init(java.util.Properties)
	 */
	public void init(Properties prop) {
		String factoryClass = prop.getProperty("repositoryManager.className");
		if(factoryClass == null){
			LOG.warn("Property 'repositoryManager.className' not found in configuration properties. Will use default file factory");
			
		}
		else{
			try {
				repository = (Repository)Class.forName(factoryClass).newInstance();
			} catch (InstantiationException e) {
				LOG.error("Class "+factoryClass+" did not instantiated.",e);
			} catch (IllegalAccessException e) {
				LOG.error("Class "+factoryClass+" did not instantiated.",e);
			} catch (ClassNotFoundException e) {
				LOG.error("Class "+factoryClass+" not found.");
			}
		}
		repository.init(prop);
		initialized = true;
	}
     
 protected void checkInitialized() {
 if(initialized==false){
		LOG.error("LiveExcel was not initialized before using.");
		throw new RuntimeException("LiveExcel was not initialized before using.");
	}
 }

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getActivationSchedule(java.lang.String)
 */
public List<VersionDesc> getActivationSchedule(String project) {
	checkInitialized();
	return repository.getActivationSchedule(project);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getDefaultVersionDesc(java.lang.String, java.util.Date)
 */
public VersionDesc getDefaultVersionDesc(String project, Date date) {
	checkInitialized();
	return repository.getDefaultVersionDesc(project, date);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getExcel(java.lang.String, com.exigen.le.project.VersionDesc)
 */
public InputStream getExcel(String project, VersionDesc version) {
	checkInitialized();
	return repository.getExcel(project, version);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getLatestVersionDesc(java.lang.String)
 */
public VersionDesc getLatestVersionDesc(String project) {
	checkInitialized();
	return repository.getLatestVersionDesc(project);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getProjectList()
 */
public List<RepositoryNodeDesc> getProjectList() {
	checkInitialized();
	return repository.getProjectList();
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getProperties()
 */
public Properties getProperties() {
	checkInitialized();
	return repository.getProperties();
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getVersionList(java.lang.String)
 */
public List<RepositoryNodeDesc> getVersionList(String projectName) {
	checkInitialized();
	return repository.getVersionList(projectName);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#isActivationDateSupport(java.lang.String)
 */
public boolean isActivationDateSupport(String project) {
	checkInitialized();
	return repository.isActivationDateSupport(project);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#resolveVersion(java.lang.String, com.exigen.le.project.VersionDesc)
 */
public VersionDesc resolveVersion(String project, VersionDesc version) {
	checkInitialized();
	return repository.resolveVersion(project, version);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#setActivationDate(java.lang.String, com.exigen.le.project.VersionDesc, java.util.Date)
 */
public void setActivationDate(String project, VersionDesc version,
		Date activationDate) {
	checkInitialized();
	repository.setActivationDate(project, version, activationDate);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#setVersionAsDefault(java.lang.String, com.exigen.le.project.VersionDesc)
 */
public void setVersionAsDefault(String project, VersionDesc version) {
	checkInitialized();
	repository.setVersionAsDefault(project, version);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getMappingXML(java.lang.String, com.exigen.le.project.VersionDesc)
 */
@Deprecated
public InputStream getMappingXML(String project, VersionDesc version) {
	return repository.getMappingXML(project, version);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getProjectElement(java.lang.String, com.exigen.le.project.VersionDesc, java.lang.String)
 */
public InputStream getProjectElement(String project, VersionDesc version,
		String file) {
	return repository.getProjectElement(project, version, file);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#getExcel(java.lang.String, com.exigen.le.project.VersionDesc, java.lang.String)
 */
public InputStream getExcel(String project, VersionDesc version,
		String excelFile) {
	return repository.getExcel(project, version, excelFile);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#retrieveElementList(java.lang.String, com.exigen.le.project.VersionDesc)
 */
public List<ProjectElement> retrieveElementList(String project,
		VersionDesc version) {
	return repository.retrieveElementList(project, version);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#isProject(java.lang.String)
 */
public boolean isProject(String projectName) {
	return repository.isProject(projectName);
}

/* (non-Javadoc)
 * @see com.exigen.le.repository.Repository#isVersion(java.lang.String, com.exigen.le.project.VersionDesc)
 */
public boolean isVersion(String projectName, VersionDesc versionDesc) {
	return repository.isVersion(projectName, versionDesc);
}

}
