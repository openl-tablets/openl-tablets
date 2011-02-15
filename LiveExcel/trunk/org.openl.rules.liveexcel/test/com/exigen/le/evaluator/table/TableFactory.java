/**
 * 
 */
package com.exigen.le.evaluator.table;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.ElementFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.usermodel.LiveExcelWorkbookFactory;

/**
 * @author vabramovs
 *
 */
public class TableFactory implements ElementFactory {
	
	private static final Log LOG = LogFactory.getLog(ElementFactory.class);

	private static TableFactory INSTANCE = new TableFactory();
	
	/**
	 * @return the iNSTANCE
	 */
	public static TableFactory getInstance() {
		return INSTANCE;
	}
	private TableFactory(){
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.project.ElementFactory#create(java.lang.String, com.exigen.le.project.VersionDesc, java.lang.String, java.io.InputStream, com.exigen.le.smodel.ServiceModel)
	 */
	public ProjectElement create(String projectName, VersionDesc versionDesc,
			String elementFileName, InputStream is, ServiceModel serviceModel,
			Properties configuration) {
		 // TODO rewrite to create real Table impl object
		TableImpl table = new TableImpl(null,null);
		ProjectElement result = new ProjectElement(elementFileName,ProjectElement.ElementType.TABLE);
		result.setElement(table);
		return result;
	}

}
