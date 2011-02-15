/**
 * 
 */
package com.exigen.le.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.project.ProjectElement.ElementType;
import com.exigen.le.project.cache.Cache;
import com.exigen.le.project.cache.CacheFactory;
import com.exigen.le.repository.AbstractRepository;
import com.exigen.le.repository.RepositoryFactory;
import com.exigen.le.repository.RepositoryFileImpl;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.usermodel.LiveExcelWorkbookFactory;

/**
 * LE Projects manipulation
 * @author vabramovs
 *
 */
public class ProjectManager {
	private static final Log LOG = LogFactory.getLog(ProjectManager.class);

	static private ProjectManager INSTANCE = new ProjectManager();
	
	static private Cache<String,Project> projects = null;
	
	static private HashMap<ProjectElement.ElementType,ElementFactory> creators = initCreators();
	
	static Properties  prop;
	
	private ProjectManager(){
		
	}

private static HashMap<ElementType, ElementFactory> initCreators() {
	HashMap<ElementType, ElementFactory> result = new HashMap<ElementType, ElementFactory>();
	for(ElementType type:ProjectElement.ElementType.values()){
		result.put(type,getDefaultCreators(type) );
	}
	return result;
}
private static ElementFactory getDefaultCreators(ElementType type) {
	ElementFactory result=null;
	switch(type){
	case WORKBOOK:
		result= LiveExcelWorkbookFactory.getInstance();
		break;
	case MAPPING:
		result= null;
		break;
	case TABLE:
		result= new LETableFactory();
		break;
	default:
		result= null;
		break;
	}
	return result;
}

	/**
	 * Init component
	 * @param properties
	 */
	public void init(Properties properties){
		prop = properties;
		creators = initCreators();
		CacheFactory.init(prop);
		projects = CacheFactory.createProjectCache();
		projects.init(prop);
		getSetTempDir();
	}
	
	/**
	 * Get server Temporary directory
	 * @return
	 */
	public String getSetTempDir(){
		String tempDir;
		if(!prop.containsKey(ElementFactory.TEMP_DIR_PROPERTY)){
			tempDir = createTempDir();
			if(tempDir != null){
					prop.put(ElementFactory.TEMP_DIR_PROPERTY,tempDir);
				}
		}
		else {
			tempDir = prop.getProperty(ElementFactory.TEMP_DIR_PROPERTY); 
		} 
		return tempDir;
	}
	/**
	 * Get Temporary directory for project
	 * @param projectName
	 * @param versionDesc
	 * @return
	 */
	public String getSetTempDir(String projectName, VersionDesc versionDesc ){
		return getProjectVersion(projectName, versionDesc).getTempDir();
	}
	/**
	 * Create common Temp Dir
	 * @return
	 */
	public static String createTempDir(){
		try {
			File dir = File.createTempFile("LEtmp", ".dir");
			dir.delete();
			boolean success= dir.mkdir();
			if(success){
				dir.deleteOnExit();
				return dir.getAbsolutePath();
			}
			else{
				String msg = "Could not create tmp directory";
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
		} catch (Exception e) {
			String msg = "Could not create tmp directory";
			LOG.error(msg,e);
			throw new RuntimeException(msg, e);
		}
	}	
	private void  deleteTempDir(){
		String tempDir = getSetTempDir();
		File dir = new File(tempDir);
		try {
			FileUtils.cleanDirectory(dir);
			// We don't delete dir itself because it will be need for future work
			// Dir will be deleted on exit
		} catch (IOException e) {
			LOG.warn("Could not clean temporary directory "+tempDir,e);
		}
	}

	/** Get component instance
	 * @return
	 */
	public static ProjectManager getInstance(){
		return INSTANCE;
	}
	/**
	 * Get/create LiveExcel workbook 
	 * @param projectName
	 * @param versionDesc
	 * @return
	 */
	public Workbook getWorkbook(String projectName, VersionDesc versionDesc, String elementName ){
		String excelFile =  ((AbstractRepository)RepositoryFactory.getRepository()).addExcelExtension(elementName);
		return (Workbook)getProjectElement(projectName, versionDesc, excelFile,ProjectElement.ElementType.WORKBOOK);
	}
	/**
	 * Get/create LiveExcel project element 
	 * @param projectName
	 * @param versionDesc
	 * @param elementName
	 * @param type
	 * @return
	 */
	public Object getProjectElement(String projectName, VersionDesc versionDesc, String elementName, ElementType type ){
		versionDesc = RepositoryFactory.getInstance().resolveVersion(projectName, versionDesc);
		Project project = getProject(projectName);
		ProjectVersion projectVersion = getProjectVersion(project, versionDesc);
		ProjectElement element = projectVersion.getElements().get(elementName);
		if(element == null){
			synchronized(projectVersion.getElements()){
				element = projectVersion.getElements().get(elementName);
				if(element == null){
					InputStream is = RepositoryFactory.getInstance().getProjectElement(projectName, versionDesc, elementName);
					element = creators.get(type).create(projectName,versionDesc,elementName, is, projectVersion.getServiceModel(),prop);
					projectVersion.getElements().put(elementName, element);
				}
			}
		}	
		
		return element.getElement();
	}
private Project  getProject(String projectName){
		Project project = projects.get(projectName);
		if(project == null){
			synchronized(projects){
				project = projects.get(projectName);  // Once more to check if concurrent putting just was done
				if(project == null){
					if(RepositoryFactory.getInstance().isProject(projectName)){
						project = new Project(projectName);
						projects.put(projectName,project);
						
					}
					else{
						String msg="Project "+projectName+" is absent in reposotory";
						LOG.error(msg);
						throw new RuntimeException(msg);
					}
				}
			}
		}
		return project;
}
private ProjectVersion getProjectVersion(Project project, VersionDesc versionDesc){
		
	ProjectVersion projectVersion = project.getVersions().get(versionDesc.getVersion());
	if(projectVersion == null)
	{
		synchronized(project.getVersions()){
			projectVersion = project.getVersions().get(versionDesc.getVersion());
			if(projectVersion == null){
				if(RepositoryFactory.getInstance().isVersion(project.getName(), versionDesc)){
						projectVersion = new ProjectVersion(project.getName(),versionDesc);
						project.addVersion(projectVersion);
				}
				else{
					String msg="Project "+project.getName()+" has no version:"+versionDesc.getVersion();
					LOG.error(msg);
					throw new RuntimeException(msg);
					
				}
			}
		}
	}
	return projectVersion;
}
private ProjectVersion getProjectVersion(String  projectName, VersionDesc versionDesc){
	versionDesc = RepositoryFactory.getInstance().resolveVersion(projectName, versionDesc);
	Project project = getProject(projectName);
	return getProjectVersion(project, versionDesc);
}
	/**
	 * @deprecated
	 * Get/create Mapping object 
	 * @param projectName
	 * @param versionDesc
	 * @return
	 */
	public Object getMapping(String projectName, VersionDesc versionDesc, String elementName ){
		return getProjectElement(projectName, versionDesc, elementName,ProjectElement.ElementType.MAPPING);
	}
	public ServiceModel getServiceModel(String projectName, VersionDesc versionDesc){
			versionDesc = RepositoryFactory.getInstance().resolveVersion(projectName, versionDesc);
			return getProjectVersion(projectName, versionDesc).getServiceModel();
		
	}
	/**
	 * Refresh version of project
	 * @param projectName
	 * @param versionDesc
	 */
	public void refresh(String projectName, VersionDesc versionDesc){
		clean(projectName,versionDesc);
	}
	/**
	 * Refresh all version of  project   
	 * @param projectName
	 */
	
	public void refresh(String projectName){
		clean(projectName);
		
	}
	/**
	 * Refresh all active projects
	 */
	public void refresh(){
		clean();
	}
	/**
	 * Free resources for version of project 
	 * @param projectName
	 * @param versionDesc
	 */
	public void clean(String projectName, VersionDesc versionDesc){
		Project prj =  projects.get(projectName);
		if(prj != null){
			ProjectVersion  version = projects.get(projectName).getVersions().get(versionDesc.getVersion());
			if(version != null){
				version.clean();
				projects.get(projectName).getVersions().remove(versionDesc.getVersion());
			}
		}	
	}
	/**
	 * Free resources for desired project
	 * @param projectName
	 */
	public void clean(String projectName){
		try {
			Cache<String,ProjectVersion>  versions = projects.get(projectName).getVersions();
				for(ProjectVersion  version:versions.getValues()){
					clean(projectName, version.getVersionDesc());
				}
		} catch (Exception e) {
		}
		projects.remove(projectName);
	}
	/**
	 * Free resources for all active  projects
	 * 
	 */
	public void clean(){
		for(Project project:projects.getValues()){
			clean(project.getName());
		}
		projects.removeAll();
		LETableFactory.shutdownDerby();
		// force clean temporary dir
		deleteTempDir();
	}
	/**
	 * Get name list of active projects
	 * @return
	 */
	public List<String> getProjectNameList() {
		List<String> answer = new ArrayList<String>();
		Set<String> prjs = projects.getKeys();
		answer.addAll(prjs);
		return answer;
	}

	/**
	 * Get list of active projects
	 * @return
	 */
	public List<VersionDesc> getVersionList(String projectName) {
		List<VersionDesc> answer = new ArrayList<VersionDesc>();
		Project prj = projects.get(projectName);
		if (prj != null){
			Collection<ProjectVersion> prjVers = projects.get(projectName).getVersions().getValues();
			for(ProjectVersion prjVer:prjVers){
				answer.add(prjVer.getVersionDesc());
			}
		}
	return answer;
	}
	/**
	 * Is this project active (in cache)
	 * @param projectName
	 * @return
	 */
	public boolean isProjectActive(String projectName){
		try{
			Set<String> prjs = projects.getKeys();
			return prjs.contains(projectName);
		} catch (NullPointerException e){
			return false;
		}
	}
	/**
	 * Register new Project Element creator
	 * @param type
	 * @param creator
	 */
	public void registerElementFactory(ProjectElement.ElementType type, ElementFactory creator){
		creators.put(type, creator);
		
	}
	/**
	 * Unregister creator for Element type
	 * @param type
	 */
	public void unRegisterElementFactory(ProjectElement.ElementType type){
		creators.put(type, getDefaultCreators(type));
	}
	
	/**
	 * Return log regime
	 * @param projectName
	 * @param versionDesc
	 * @return
	 */
	public boolean isDoLog(String projectName, VersionDesc versionDesc){
		return getProjectVersion(projectName, versionDesc).isDoLog();
	}
	
	/**
	 * Set log regime
	 * @param projectName
	 * @param versionDesc
	 * @param doLog
	 */
	public void setDoLog(String projectName, VersionDesc versionDesc, boolean doLog){
		getProjectVersion(projectName, versionDesc).setDoLog(doLog);
	}
	
	/**
	 * Get log file path
	 * @param projectName
	 * @param versionDesc
	 * @return
	 */
	public String getLogFile(String projectName, VersionDesc versionDesc){
		return getProjectVersion(projectName, versionDesc).getLogFile();
	}
	
	/** Set Log File
	 * @param projectName
	 * @param versionDesc
	 * @param logFileName
	 */
	public void setLogFile(String projectName, VersionDesc versionDesc, String logFileName){
		getProjectVersion(projectName, versionDesc).setLogFile(logFileName);
	}
	
	
	
	/** Log Data
	 * @param projectName
	 * @param versionDesc
	 * @param log
	 */
	public void logData(String projectName, VersionDesc versionDesc, Object[] log){
		getProjectVersion(projectName, versionDesc).logData(log);
	}
}
