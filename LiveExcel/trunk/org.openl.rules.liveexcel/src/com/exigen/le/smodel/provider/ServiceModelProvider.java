/**
 * 
 */
package com.exigen.le.smodel.provider;

import java.util.List;

import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;

/**
 * @author vabramovs
 *
 */
public interface ServiceModelProvider {
	
	public List<Function> findFunctions(String projectName, VersionDesc versionDesc, List<Type> types) ;

	public List<Type> findTypes(String projectName, VersionDesc versionDesc) ;

	public List<TableDesc> findTables(String projectName, VersionDesc versionDesc);
	
	public ServiceModel create(String projectName, VersionDesc versionDesc);

}
