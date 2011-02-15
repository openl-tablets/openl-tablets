/**
 * 
 */
package com.exigen.le.project;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.exigen.le.smodel.ServiceModel;

/**
 * @author vabramovs
 *
 */
public interface ElementFactory {
	static final String TEMP_DIR_PROPERTY = "temp.dir" ;
	ProjectElement create(String projectName, VersionDesc versionDesc,String elementFileName,InputStream is, ServiceModel serviceModel,Properties configuration);
}
