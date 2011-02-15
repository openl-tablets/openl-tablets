/**
 * 
 */
package com.exigen.le.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.project.ProjectElement.ElementType;


/**
 * Repository Implementation for SVN
 * @author vabramovs
 *
 */
public class RepositorySVNImpl extends AbstractRepository  {

	private static final Log LOG = LogFactory.getLog(RepositorySVNImpl.class);
	private static boolean factoryInitialized = false;
	
	private String url=null;
	private String userName=null;
	private  String password=null;
	SVNRepository repository = null;

	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#init(java.util.Properties)
	 */
	public void init(Properties prop) {
		super.init(prop);
        url = prop.getProperty("repositoryManager.root");
        rootPath="";
		if(factoryInitialized == false){
			if(url.toLowerCase().startsWith("svn")){
				SVNRepositoryFactoryImpl.setup();
			}
			else if (url.toLowerCase().startsWith("file")){
				FSRepositoryFactory.setup();
			}
			else {
				LOG.error("Unsupported SVN protocol:"+url);
				throw new RuntimeException("Unsupported SVN protocol:"+url);
			}
			factoryInitialized = true;
		}
         if(url == null) {
			 LOG.error("Property 'repositoryManager.root' don't found");
			 throw new RuntimeException("Property 'repositoryManager.root' don't found");
         }
         userName = prop.getProperty("repositoryManager.login=demouserlogin","user");
         password = prop.getProperty("repositoryManager.password","password");

         
		 try { 
		     repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
		     ISVNAuthenticationManager authManager = 
		                  SVNWCUtil.createDefaultAuthenticationManager(userName, password);
		     repository.setAuthenticationManager(authManager);
		 } catch (SVNException e){
			 LOG.error("Could not connect with SVN: "+url,e);
			 throw new RuntimeException("Could not connect with SVN: "+url,e);
		 }
		this.prop = prop;

	}
	/* (non-Javadoc)
	 * @see com.exigen.le.repository.Repository#getExcel(java.lang.String, com.exigen.le.projects.VersionDesc)
	 */
	public InputStream getExcel(String project, VersionDesc versionDesc) {
		String excelFile =  prop.getProperty("project."+project, project);
		return getExcel(project, versionDesc, excelFile);
	}
	
	public InputStream getExcel(String project, VersionDesc version,
			String excelFile) {
		String file = buildFilePath(project, version.getVersion(),addExcelExtension(excelFile));
		return getProjectElement(project, version, file);
	}
	
	public InputStream getProjectElement(String project, VersionDesc version,
			String file) {
		try {
			File tmp = File.createTempFile("LE_", excelExtension);
			
			OutputStream contents = new FileOutputStream(tmp);
			
			String elementFile = buildFilePath(project, version.getVersion(),file);
			
			long receivedRevision =  repository.getFile(elementFile, (-1),null, contents);
			
			version.setRevisionXML(""+receivedRevision);
			
			LOG.info("Obtain revision "+receivedRevision+" for asked "+elementFile);
			
			
			contents.close();
			
			InputStream result = new FileInputStream(tmp);
			
			tmp.deleteOnExit();
			
			return result;
			
		} catch (FileNotFoundException e) {
			LOG.error("Could not create temporary file to get SVN content",e);
			throw new RuntimeException("Could not create temporary file to get SVN content",e);
		} catch (IOException e) {
			LOG.error("IO error while get SVN content",e);
			throw new RuntimeException("IO error while get SVN content",e);
		} catch (SVNException e) {
			LOG.error("SVN error to get file "+file,e);
			throw new RuntimeException("SVN error to get file "+file,e);
		}
	}

	@SuppressWarnings("unchecked")
	public List<RepositoryNodeDesc> getProjectList() {
		List<RepositoryNodeDesc> answer = new ArrayList<RepositoryNodeDesc>();
		try {
			Collection<SVNDirEntry> dirContent = repository.getDir(rootPath,(-1),(SVNProperties)null,(Collection)null);
			for(SVNDirEntry node : dirContent){
				SVNProperties prop = new SVNProperties();
				repository.getDir(rootPath+node.getName(),node.getRevision(),prop,(Collection)null);
				answer.add(new RepositoryNodeDesc(node.getName(),""+node.getRevision(),node.getAuthor(),node.getCommitMessage(),prop.asMap(),node.getDate()));
			}
			Arrays.sort(dirContent.toArray());
		} catch (SVNException e) {
			LOG.warn("Exception while try to get Repository ProjectList ",e);
		}
		return answer;
	}
	public List<RepositoryNodeDesc> getVersionListImpl(String projectName) {
		List<RepositoryNodeDesc> answer = new ArrayList<RepositoryNodeDesc>();
		String path = getProjectRootPath(projectName);
		// Add branches
		String brancPath = prop.getProperty("project."+projectName+".branchPath",this.branchPath);
		path = addSegmentToPath(path, brancPath);
		Collection<SVNDirEntry> dirContent;
		try {
			dirContent = repository.getDir(path,(-1),(SVNProperties)null,(Collection)null);
		} catch (SVNException e) {
			LOG.error("Exception while try to get Repository Project version List ",e);
			throw new RuntimeException("Exception while try to get Repository Project version List ",e);
		}
			for(SVNDirEntry node : dirContent){
				try {
					SVNProperties prop = new SVNProperties();
					repository.getDir(rootPath+node.getName(),node.getRevision(),prop,(Collection)null);
					answer.add(new RepositoryNodeDesc(node.getName(),""+node.getRevision(),node.getAuthor(),node.getCommitMessage(),prop.asMap(),node.getDate()));
				} catch (SVNException e) {
					LOG.warn("Exception while try to get Repository Project '"+projectName+"' version '"+node.getName()+"'  properties",e);
				}
			}
		Arrays.sort(dirContent.toArray());
		
		// Add HEAD
		path = getProjectRootPath(projectName);
		String headPath = prop.getProperty("project."+projectName+".headPath",this.headPath);
		if(headPath.length()>0){
			path = addSegmentToPath(path, headPath);
			try {
				//SVNProperties prop = new SVNProperties();
				SVNDirEntry trunk = repository.getDir(path,(-1),true,(Collection)null);
				answer.add(new RepositoryNodeDesc(">",""+trunk.getRevision(),trunk.getAuthor(),trunk.getCommitMessage(),null,trunk.getDate()));
			} catch (SVNException e) {
				LOG.warn("Exception while try to get Repository Project version List ",e);
			}
		}
			
		return answer;
	}
	
	public InputStream getMappingXML(String project, VersionDesc versionDesc) {
		String xmlFile = buildFilePath(project, versionDesc.getVersion(),project+".xml");
		return getProjectElement(project, versionDesc, xmlFile);
	}
	
	protected String getLatestBranches(String path) {
		Collection<SVNDirEntry> dirContent;
		try {
			dirContent = repository.getDir(path,(-1),(SVNProperties)null,(Collection)null);
		} catch (SVNException e) {
			LOG.error("Exception while try to get Repository Project version List ",e);
			throw new RuntimeException("Exception while try to get Repository Project version List ",e);
		}
			for(SVNDirEntry node : dirContent){
				try {
					SVNProperties prop = new SVNProperties();
					repository.getDir(rootPath+node.getName(),node.getRevision(),prop,(Collection)null);
				} catch (SVNException e) {
					LOG.warn("Exception while try to get list of contains "+path,e);
				}
			}
		Arrays.sort(dirContent.toArray());
		Iterator<SVNDirEntry> it = dirContent.iterator();
		SVNDirEntry branch = null;
		while(it.hasNext()){
			branch = it.next();
		}
		if(branch != null){
			return branch.getName();
			
		}
		return null;
	}
	
	public List<ProjectElement> retrieveElementList(String project,
			VersionDesc version) {
		List<ProjectElement> result = new ArrayList<ProjectElement>();
		String path = buildVersion(project, version.getVersion());
		
		Collection<SVNDirEntry> dirContent;
		try {
			dirContent = repository.getDir(path,(-1),(SVNProperties)null,(Collection)null);
		} catch (SVNException e) {
			LOG.error("Exception while try to get Repository Project version List ",e);
			throw new RuntimeException("Exception while try to get Repository Project version List ",e);
		}
			for(SVNDirEntry node : dirContent){
					int ind = node.getName().lastIndexOf(".");
					String ext = node.getName().substring(ind);
					ElementType type = ProjectElement.ElementType.getByExtension(ext);
					ProjectElement element = new ProjectElement(node.getName(), type);
					result.add(element);
				
			}
		return result;
		
	}
	
}
