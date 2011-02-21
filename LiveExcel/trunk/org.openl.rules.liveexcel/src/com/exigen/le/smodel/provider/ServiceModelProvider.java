/**
 * 
 */
package com.exigen.le.smodel.provider;

import java.io.File;
import java.util.List;

import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;

/**
 * @author vabramovs
 *
 */
public interface ServiceModelProvider {
	
	List<Function> findFunctions(List<Type> types) ;

	List<Type> findTypes() ;

	List<TableDesc> findTables();
	
	ServiceModel create();
	
	File getProjectLocation();
}
