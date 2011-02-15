/**
 * 
 */
package com.exigen.le.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.project.ProjectElement.ElementType;


/**
 * Implement Reposotory for File System
 * @author vabramovs
 *
 */
public class RepositoryFileImpl extends AbstractRepository {
	
	private static final Log LOG = LogFactory.getLog(RepositoryFileImpl.class);

	
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.AbstractRepository#getExcel(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public InputStream getExcel(String project, VersionDesc version) {
		String excelFile =  prop.getProperty("project."+project, project);
		excelFile = addExcelExtension(excelFile);
		return getProjectElement(project, version, excelFile);
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getExcel(java.lang.String, com.exigen.le.projects.VersionDesc)
	 */
	public InputStream getExcel(String project, VersionDesc version,String excelFile) {
		excelFile = addExcelExtension(excelFile);
		return getProjectElement(project, version, excelFile);
	}
	
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getProjectElement(java.lang.String, com.exigen.le.project.VersionDesc, java.lang.String)
	 */
	public InputStream getProjectElement(String project, VersionDesc version,
			String file) {
		String elementFile = buildFilePath(project, version.getVersion(),file);	
		InputStream result = RepositoryFileImpl.class.getClassLoader().getResourceAsStream(elementFile);
		if (result == null){
			LOG.error("File '"+elementFile+" does not found");
			throw new RuntimeException("File '"+elementFile+" does not found");
		}
		LOG.debug("Try to open Project element file: "+elementFile+" for version "+version.getVersion()+"("+version.getRevisionExcel()+")");
		return result;
	}

	protected String getLatestBranches(String path){
		URL url = RepositoryFileImpl.class.getClassLoader().getResource(path);
		File dir = new File(url.getFile());
		File[] files = dir.listFiles(fileFilter);
		if(files != null && files.length>0){
			Arrays.sort(files);
			return files[files.length-1].getName();
		}
		else{
			return "";
		}

	}
	
	// This filter  returns  only directories 
	FileFilter fileFilter = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory(); }
		};

	
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getProjectList()
	 */
	public List<RepositoryNodeDesc> getProjectList() {
		List<RepositoryNodeDesc> answer = new ArrayList<RepositoryNodeDesc>();
		URL url = RepositoryFileImpl.class.getClassLoader().getResource(rootPath);
		File dir = new File(url.getFile());
		File[] files = dir.listFiles(fileFilter);
		if(files != null && files.length>0){
			for(File file: files)
			{
				Map<String,Object> map = new HashMap<String,Object>();
				RepositoryNodeDesc desc = new RepositoryNodeDesc(file.getName(),"","","",map,new Date(file.lastModified()));
				answer.add(desc);
			}
		}
		Arrays.sort(answer.toArray());
		return answer;
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getRevisionList(java.lang.String)
	 */
	public List<RepositoryNodeDesc> getVersionListImpl(String projectName) {
		List<RepositoryNodeDesc> answer = new ArrayList<RepositoryNodeDesc>();
		String path = 	getProjectRootPath(projectName);		
		String brancPath = prop.getProperty("project."+projectName+".branchPath",this.branchPath);
		path = addSegmentToPath(path, brancPath);

		URL url = RepositoryFileImpl.class.getClassLoader().getResource(path);
		if(url == null){
			String msg = "Repository path "+path + " was not found";
			LOG.error(msg);
			throw new RuntimeException(msg);
		}
		File dir = new File(url.getFile());
		File[]  vers = dir.listFiles(fileFilter);
		Map<String,Object> map = new HashMap<String,Object>();
		if(vers != null && vers.length>0){  // support versions
			Arrays.sort(vers);
			for(File ver: vers)
			{
				File excelFile = findProjectElementWithProjectName(projectName, ver.getName(),ProjectElement.ElementType.WORKBOOK);
				if(excelFile != null){ // Dir contains excel file with same name
					RepositoryNodeDesc revDesc = new RepositoryNodeDesc("",ver.getName(),"","",map,new Date(excelFile.lastModified()));
					answer.add(revDesc);
				}
				else{ 
					LOG.warn("Did not found default excel file in :"+addSegmentToPath(dir.getAbsolutePath(),ver.getName()));
					File smFile = findProjectElementWithProjectName(projectName, ver.getName(),ProjectElement.ElementType.SERVICEMODEL);
					if(smFile != null){
						RepositoryNodeDesc revDesc = new RepositoryNodeDesc("",ver.getName(),"","",map,new Date(smFile.lastModified()));
						answer.add(revDesc);
					}
					else
						LOG.warn("Did not found also default sm file in :"+addSegmentToPath(dir.getAbsolutePath(),ver.getName()));
				}
			}
		}
		else {
			File excelFile = findProjectElementWithProjectName(projectName, "",ProjectElement.ElementType.WORKBOOK);
			if(excelFile != null){ // Dir contains excel file with same name
				RepositoryNodeDesc revDesc = new RepositoryNodeDesc("","","","",map,new Date(excelFile.lastModified()));
				answer.add(revDesc);
			}
			else{ 
				LOG.warn("Did not found default excel file in :"+dir.getAbsolutePath());
				File smFile = findProjectElementWithProjectName(projectName, "",ProjectElement.ElementType.SERVICEMODEL);
				if(smFile != null){
					RepositoryNodeDesc revDesc = new RepositoryNodeDesc("","","","",map,new Date(smFile.lastModified()));
					answer.add(revDesc);
				}
				else
					LOG.warn("Did not found also default sm file in :"+dir.getAbsolutePath());
			}
		}
		return answer;
	}
	private File findProjectElementWithProjectName(String projectName,String version,ProjectElement.ElementType elementType){
		for(String ext :elementType.getExtensions()){
			String elemPath = buildFilePath(projectName, version,projectName+ext);
			URL fileUrl = RepositoryFileImpl.class.getClassLoader().getResource(elemPath);
			if(fileUrl != null){ // Dir contains elem file with same name
				File elemFile	= new File(fileUrl.getFile());
				return elemFile;
			}
		}
		return null;
	}  
	
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getMappingXML(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public InputStream getMappingXML(String project, VersionDesc version)
	{
		String file = project+".xml";
		return getProjectElement(project, version, file);
	}
	
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#retrieveElementList(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public List<ProjectElement> retrieveElementList(String project,
			VersionDesc version) {
		List<ProjectElement> result = new ArrayList<ProjectElement>();
		String path = buildVersion(project, version.getVersion());
		URL url = RepositoryFileImpl.class.getClassLoader().getResource(path);
		File dir = new File(url.getFile());
		File[] elems = dir.listFiles();
		Map<String,Object> map = new HashMap<String,Object>();
		if(elems != null && elems.length>0){  // project not empty and 
			for(File elem: elems)
			{
				int ind = elem.getName().indexOf(".");
				if(ind != (-1)){
					String ext = elem.getName().substring(ind);
					ElementType type = ProjectElement.ElementType.getByExtension(ext);
					ProjectElement element = new ProjectElement(elem.getName(), type);
					result.add(element);
				}
			}
		
		}
		return result;
		
	}
    
}
