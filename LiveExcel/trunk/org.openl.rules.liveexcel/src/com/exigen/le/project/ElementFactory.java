/**
 * 
 */
package com.exigen.le.project;

import java.io.InputStream;

import com.exigen.le.smodel.ServiceModel;

/**
 * @author vabramovs
 *
 */
public interface ElementFactory {
	static final String TEMP_DIR_PROPERTY = "temp.dir" ;
	ProjectElement create(String elementFileName,InputStream is, ServiceModel serviceModel);
}
