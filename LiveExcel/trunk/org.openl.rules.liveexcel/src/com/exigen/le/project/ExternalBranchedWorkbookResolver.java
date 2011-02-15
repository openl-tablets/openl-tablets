/**
 * 
 */
package com.exigen.le.project;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PathUtils;

import com.exigen.le.repository.RepositoryFactory;

/**
 * @author vabramovs
 *
 */
public class ExternalBranchedWorkbookResolver implements
		IExternalWorkbookResolver {
	private String project;
	private VersionDesc version;
	

	public ExternalBranchedWorkbookResolver(String project,VersionDesc version){
		this.project = project;
		this.version = RepositoryFactory.getInstance().resolveVersion(project, version);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.ss.formula.IExternalWorkbookResolver#resolveExternalExcel(java.lang.String)
	 */
	
	public InputStream resolveExternalExcel(String externalWorkbookReference)
			throws FileNotFoundException {
		// This method resolve excel file as Input stream from repository, does not touch Cache 
		String file = PathUtils.extractFile(externalWorkbookReference);
		return RepositoryFactory.getInstance().getExcel(project, version,file);
	}
	/* (non-Javadoc)
	 * @see org.apache.poi.ss.formula.IExternalWorkbookResolver#resolveExternalWorkbook(java.lang.String)
	 */
	public Workbook resolveExternalWorkbook(String externalWorkbookReference){
		// This method resolve excel file as workbook  from Cache 
		
		String file = PathUtils.extractFile(externalWorkbookReference);
		return ProjectManager.getInstance().getWorkbook(project, version, file);
	}

	/**
	 * @return the project

 */
	
	public String getProject() {
		return project;
	}

	/**
	 * @return the version
	 */
	public VersionDesc getVersion() {
		return version;
	}

}
