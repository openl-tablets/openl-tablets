/**
 * 
 */
package com.exigen.le.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.datalogger.DataLogger;
import com.exigen.le.project.cache.Cache;
import com.exigen.le.project.cache.CacheFactory;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.provider.ServiceModelProvider;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;

/**
 * LE Project version and it's elements
 * @author vabramovs
 *
 */
public class ProjectVersion {

	private static final Log LOG = LogFactory.getLog(ProjectVersion.class);
	
	public static final String DATA_LOG = "datalog";
	public static final String DATA_LOG_FILE = "datalog.log";
	public static final String DO_LOG = "doLog";
	private String projectName;
	private VersionDesc versionDesc;
	private ServiceModel serviceModel;
	private Cache<String,ProjectElement > elements;
	private String tempDir;
	private String logFile;
	private boolean doLog = false;
	private DataLogger logger = null;
	

	/**
	 * @return the tempDir
	 */
	public String getTempDir() {
		return tempDir;
	}


	/**
	 * @return the elements
	 */
	public Cache<String, ProjectElement> getElements() {
		return elements;
	}


	/**
	 * @param projectName
	 * @param versionDesc
	 */
	public ProjectVersion(String projectName, VersionDesc versionDesc) {
		this.projectName = projectName;
		this.versionDesc = versionDesc;
		ServiceModelProvider provider = ServiceModelProviderFactory.getInstance().getProvider();
		serviceModel=provider.create(projectName, versionDesc);
		this.elements =CacheFactory.createElementCache();
		String serverTemp = ProjectManager.getInstance().getSetTempDir();
		try {
			File dir = new File(serverTemp,projectName+"_"+versionDesc.getVersion()+".dir" );
			dir.delete();
			boolean success= dir.mkdir();
			if(success){
				this.tempDir= dir.getAbsolutePath();
			}
			else{
				String msg = "Could not create tmp directory for project "+projectName+" version "+versionDesc.getVersion();
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
		} catch (Exception e) {
			String msg = "Could not create tmp directory for project "+projectName+" version "+versionDesc.getVersion();
			LOG.error(msg,e);
			throw new RuntimeException(msg, e);
		}
		Properties prop = ProjectManager.prop;
		logFile = (String)prop.getProperty(projectName+"."+versionDesc.getVersion()+"."+ DATA_LOG);

		doLog = Boolean.parseBoolean((String)prop.getProperty(projectName+"."+versionDesc.getVersion()+"."+ DO_LOG,"0"));
	}

	
	/**
	 * @return the serviceModel
	 */
	public ServiceModel getServiceModel() {
		return serviceModel;
	}


	/**
	 * @param serviceModel the serviceModel to set
	 */
	public void setServiceModel(ServiceModel serviceModel) {
		this.serviceModel = serviceModel;
	}


	/**
	 * @return
	 */
	public Set<String> getElementsPath() {
		return elements.getKeys();
	}


	/**
	 * @param element
	 */
	public void addElement(ProjectElement element) {
		elements.put(versionDesc.getVersion(),element);
	}

	public void clean(){
		
		for(ProjectElement element:elements.getValues()){
			element.dispose();
		}
		// Delete temp dir
		File dir = new File(tempDir);
		try {
			FileUtils.cleanDirectory(dir);
			FileUtils.deleteQuietly(dir);
		} catch (IOException e) {
			LOG.warn("Could not clean temporary directory "+tempDir,e);
		}
		
		// close log
		closeLog();

	}

	public boolean equals(Object a){
		if(a instanceof ProjectVersion){
			ProjectVersion prj = (ProjectVersion)a;
			return projectName.equals(prj.getProjectName()) && versionDesc.equals(prj.getVersionDesc());
		}
		return false;
			
	}

	public int hashCode(){
		return projectName.hashCode()+versionDesc.hashCode();
	}

	/**
	 * @return the projectName
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * @return the versionDesc
	 */
	public VersionDesc getVersionDesc() {
		return versionDesc;
	}


	/**
	 * @return the logFile
	 */
	public String getLogFile() {
		return logFile;
	}


	/**
	 * @param logFile the logFile to set
	 */
	public void setLogFile(String logFile) {
		closeLog();
		logger = null;
		this.logFile = logFile;
	}


	/**
	 * @return the doLog
	 */
	public boolean isDoLog() {
		return doLog;
	}


	/**
	 * @param doLog the doLog to set
	 */
	public void setDoLog(boolean doLog) {
		this.doLog = doLog;
	}
	
	private void closeLog(){
		if (logger != null){
			logger.close();
		}
	}
	
	public void logData(Object[] olist){
		if (!doLog){
			return;
		}
		if (logger == null) {
			
			String logFilePath = null;
			if (logFile == null){
				logFilePath = tempDir+"/"+DATA_LOG_FILE;
			} else {
				logFilePath = tempDir+"/"+logFile;
			}
			logger = DataLogger.createInstance(logFilePath);
		}
		logger.write(olist);
	}


	/**
	 * @param elements the elements to set
	 */
	public void setElements(Cache<String, ProjectElement> elements) {
		this.elements = elements;
	}


}
