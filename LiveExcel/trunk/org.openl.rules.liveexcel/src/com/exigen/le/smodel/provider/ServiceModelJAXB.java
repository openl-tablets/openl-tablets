/**
 * 
 */
package com.exigen.le.smodel.provider;

import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.repository.RepositoryFactory;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.Function.FunctionArgument;

/**
 * Service Model provider based on JAXB
 * @author vabramovs
 *
 */
public class ServiceModelJAXB implements ServiceModelProvider {
	
	private static final Log LOG = LogFactory.getLog(ServiceModelJAXB.class);

	static 	JAXBContext jc = null; 
	static Unmarshaller u = null;


	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.provider.ServiceModelProvider#create(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public ServiceModel create(String projectName, VersionDesc versionDesc) {
		
		try {
			List<ProjectElement> elems = RepositoryFactory.getRepository().retrieveElementList(projectName, versionDesc);
			for(ProjectElement elem : elems){
				if(elem.getType()!= null && elem.getType().equals(ProjectElement.ElementType.SERVICEMODEL)){
					InputStream is = RepositoryFactory.getRepository().getProjectElement(projectName, versionDesc, elem.getElementFileName());
					if(jc == null){ // init JAXB infrastructure
						jc = JAXBContext.newInstance( "com.exigen.le.smodel" );
						u = jc.createUnmarshaller();
					}
					ServiceModel sm = (ServiceModel)u.unmarshal( is );
					
					//Restore type references
					for(Type type:sm.getTypes()){
						Vector<Type> done = new Vector<Type>();
						restoreTypeRef(type, sm,done);
					}
					
					// Bind functions return and function parameters to type by name
					for(Function func:sm.getFunctions()){
						try {
							if(func.getReturnType()== null&&func.getReturnTypeName()!= null){ // Add  second check to avoid  frightened output
								func.setReturnType(sm.getType(func.getReturnTypeName()));
							}
						} catch (Exception e) {   // Both ReturnType and ReturnTypeName may be null(it means undefined primitive type), that will fire this exception ()
						}
						
						for(FunctionArgument arg:func.getArguments()){
								try {
									if(arg.getType()== null && arg.getTypeName() != null){ // Add  second check to avoid  frightened output
										arg.setType(sm.getType(arg.getTypeName()));
									}
								} catch (Exception e) { // Both argType and argTypeName may be null(it means undefined primitive type), that will fire this exception ()
								}
						}
					}
					
					// Build paths for all types from root
					for(Type type:sm.getTypes()){
						type.setPaths(type.getName());
					}
					return sm;
				}
			}
		} catch (Exception e) {
			String msg = "Error during getting service model for project "+projectName+",version "+versionDesc.getVersion();
			LOG.error(msg, e);
			throw new RuntimeException(msg,e);
		}
		String msg = "Did not find service model for project "+projectName+",version "+versionDesc.getVersion();
		LOG.error(msg);
		throw new RuntimeException(msg);
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.provider.ServiceModelProvider#findFunctions(java.lang.String, com.exigen.le.project.VersionDesc, java.util.List)
	 */
	public List<Function> findFunctions(String projectName,
			VersionDesc versionDesc, List<Type> types) {
		return create(projectName, versionDesc).getFunctions();
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.provider.ServiceModelProvider#findTables(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public List<TableDesc> findTables(String projectName,
			VersionDesc versionDesc) {
		return create(projectName, versionDesc).getTables();
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.provider.ServiceModelProvider#findTypes(java.lang.String, com.exigen.le.project.VersionDesc)
	 */
	public List<Type> findTypes(String projectName, VersionDesc versionDesc) {
		return create(projectName, versionDesc).getTypes();
	}
	protected void restoreTypeRef(Type type,ServiceModel sm, Vector<Type> done){
		if(!done.contains(type)){
			done.add(type);
			for(Property child:type.getChilds()){
				if(child.getType()==null){
					String typeName = child.getTypeName().toUpperCase();
					child.setTypeName(typeName);
					Type childType = Type.Primary.getTypeByName(typeName); // Try primitive type
					if(childType == null){  // No, it's our complex
						childType = sm.getType(typeName);
						child.setType(childType);
						restoreTypeRef(childType,sm,done);
					}
					else{
						child.setType(childType);
					}
				}
				else{
					restoreTypeRef(child.getType(),sm,done);
				}
			}
		}	
	} 
	
}
