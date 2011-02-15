/**
 * 
 */
package com.exigen.le.repository;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Type;

/**
 * @author vabramovs
 *
 */
public abstract class AbstractRepository implements Repository {
	
	private static final Log LOG = LogFactory.getLog(RepositoryFileImpl.class);
	protected Properties prop;
	protected String rootPath;
	protected String branchPath = "branches";
	protected String headPath = "trunk";
	protected String excelExtension =".xlsm";

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#init(java.util.Properties)
	 */
	public void init(Properties prop) {
		rootPath = prop.getProperty("repositoryManager.root","./LERepository");
		excelExtension = prop.getProperty("repositoryManager.excelExtension",".xlsm");
		branchPath= prop.getProperty("repositoryManager.branchPath",branchPath);
		headPath = prop.getProperty("repositoryManager.headPath",headPath);
		this.prop = prop;

	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getActivationSchedule(java.lang.String)
	 */
	public List<VersionDesc> getActivationSchedule(String project) {
		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		List<VersionDesc> history = new ArrayList<VersionDesc>();
		String activationPrefix = project+".activationDate.";
		Enumeration<?> en = prop.propertyNames();
		while (en.hasMoreElements()){
			String s = (String)en.nextElement();
			if (s.startsWith(activationPrefix)){
				String revision = s.substring(activationPrefix.length());
				String strDate = prop.getProperty(s);
				try {
					Date date = df.parse(strDate);
					history.add(new VersionDesc(revision,date));
				} catch (ParseException e) {
					LOG.warn("Could not interpretate '"+ strDate+"' as Date. Property "+s,e);
				}
			}
		}
		if(history.size()>0){
			Collections.sort (history,VersionDesc.NEWEST_DATE_ORDER);
		}
		return history;
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getDefaultVersionDesc(java.lang.String, java.util.Date)
	 */
	public VersionDesc getDefaultVersionDesc(String project, Date date) {
		List<VersionDesc> activation = getActivationSchedule(project);
		String version = "";
		if(activation.size() > 0){ // Activation by date is on
			Iterator<VersionDesc> it = activation.iterator();
			VersionDesc current = it.next();
			while(it.hasNext() &&  current.getDate().compareTo(date)>0 ){
				current = it.next();
			}
			if(current.getDate().compareTo(date)<=0){ // we find right activation
				version = current.getVersion();
			}
			else{
				String msg = "Calculation date "+date+" is earler than any activation date for project "+ project;
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
		}
		else{   // Activation by date is off - constant default for project
			version = prop.getProperty(project+".version");
			if(version == null){
				VersionDesc latestVersion = getLatestVersionDesc(project);
				setVersionAsDefault(project, latestVersion);
				version = latestVersion.getVersion();
			}
		}	
		if(version.equals(""))
		{
			return new VersionDesc(version,date);
		}
		return resolveVersion(project, new VersionDesc(version,date));
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getExcel(java.lang.String, com.exigen.le.projects.VersionDesc)
	 */
	public abstract InputStream getExcel(String project, VersionDesc version);

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getProjectList()
	 */
	public abstract  List<RepositoryNodeDesc> getProjectList();

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getProperties()
	 */
	public Properties getProperties() {
		return prop;
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getRevisionList(java.lang.String)
	 */
	public List<RepositoryNodeDesc> getVersionList(String projectName) {
		List<RepositoryNodeDesc> result = getVersionListImpl(projectName);
		List<VersionDesc> activations = getActivationSchedule(projectName);
		for(VersionDesc act:activations){
			for(RepositoryNodeDesc node:result){
				if(node.getVersion().equals(act.getVersion())){
					node.setActivationDate(act.getDate());
					break;
				}
			}
		}
		return result;
	}
	

	/**
	 * Get list of version of project
	 * @param projectName
	 * @return
	 */
	abstract List<RepositoryNodeDesc> getVersionListImpl(String projectName);


	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#isActivationDateSupport(java.lang.String)
	 */
	public boolean isActivationDateSupport(String project) {
		String activationPrefix = project+".activationDate.";
		Enumeration<?> en = prop.propertyNames();
		while (en.hasMoreElements()){
			String s = (String)en.nextElement();
			if (s.startsWith(activationPrefix)){
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#resolveVersion(java.lang.String, com.exigen.le.projects.VersionDesc)
	 */
	public VersionDesc resolveVersion(String project, VersionDesc versionDesc) {
		String version;
		VersionDesc answer = versionDesc;
		if(versionDesc != null){
			version = versionDesc.getVersion();
			if(version.equals(">")){
				answer = new VersionDesc(getLatestVersionDesc(project).getVersion(),versionDesc.getDate());
			}
			else if(version.equals("")||version.equals(" ") ){
					return  getDefaultVersionDesc(project, versionDesc.getDate());
				}
			else{
				if(!version.contains("/")&& !version.contains("\\") ){
					String branchPath = prop.getProperty("project."+project+".branchPath",this.branchPath);
					version = addSegmentToPath(branchPath, version);
				}
				answer = new VersionDesc(version,versionDesc.getDate());
			}
		}	
		else {  // Version - null
			answer =  getLatestVersionDesc(project);
		}
		return answer;
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#setActivationDate(java.lang.String, com.exigen.le.projects.VersionDesc, java.util.Date)
	 */
	public void setActivationDate(String project, VersionDesc version,
			Date activationDate) {
		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		prop.put(project+".activationDate."+version.getVersion(),df.format(activationDate));
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#setVersionAsDefault(java.lang.String, com.exigen.le.projects.VersionDesc)
	 */
	public void setVersionAsDefault(String project, VersionDesc version) {
			prop.put(project+".version", version.getVersion());

	}
	protected String addSegmentToPath(String path,String segment){
		if(segment== null || segment.length() ==0){
			return path;
		}
		if(path != null && path.length() != 0){
				if(!path.endsWith("\\") && !path.endsWith("/"))
				{
					path = path+"/";
				}
				return path+segment;
			}
		
		else
		{ 
		return segment;
		}
	}
	protected String getProjectRootPath(String project){
		return addSegmentToPath(rootPath, project);
	}
	protected String buildFilePath(String project, String version,String file){
		
		String path = buildVersion(project, version);
		return addSegmentToPath(path, file);
	}
 protected String buildVersion(String project, String version){
	String path = getProjectRootPath(project);
	
	if(!version.isEmpty())
	{
		path=addSegmentToPath(path,version);
	}
	return path;
 }
	protected abstract  String getLatestBranches(String path);
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getLatestVersionDesc(java.lang.String)
	 */
	public VersionDesc getLatestVersionDesc(String project) {
				String path = getProjectRootPath(project);
				String version;
				String headPath = prop.getProperty("project."+project+".headPath",this.headPath);
				if(headPath.length()==0){
					String branchPath = prop.getProperty("project."+project+".branchPath",this.branchPath);
					String branchName = getLatestBranches(addSegmentToPath(path, branchPath)); 
					version = addSegmentToPath(branchPath, branchName);
					
				}
				else{
					version = headPath;
								
				}
				return new VersionDesc(version);
			}
	public String addExcelExtension(String file ){
		String excelFile = file;
		if(!excelFile.endsWith(".xls")&&!excelFile.endsWith(".xlsx")&&!excelFile.endsWith(".xlsm"))
		{
			excelFile = excelFile+excelExtension;
		}
		return excelFile;
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#isProject(java.lang.String)
	 */
	public boolean isProject(String projectName) {
		List<RepositoryNodeDesc> prjs = getProjectList();
		for(RepositoryNodeDesc prj:prjs){
			if(prj.getName().equals(projectName))
				return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#isVersion(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public boolean isVersion(String projectName, VersionDesc versionDesc) {
		versionDesc = resolveVersion(projectName, versionDesc);
		List<RepositoryNodeDesc> nodes = getVersionList(projectName);
		for(RepositoryNodeDesc node:nodes){
			if(node.getVersion().equals(versionDesc.getVersion()))
				return true;
		}
		return false;
	}
	
}
