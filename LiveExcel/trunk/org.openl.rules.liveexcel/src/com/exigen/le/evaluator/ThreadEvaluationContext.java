/**
 * 
 */
package com.exigen.le.evaluator;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;

/**
 * Keep LE Evaluation context for each thread
 * @author vabramovs
 * 
 *
 */
public class ThreadEvaluationContext {
	   private static ThreadEvaluationContext INSTANCE = new ThreadEvaluationContext(); 
	   private static final Log LOG = LogFactory.getLog(ThreadEvaluationContext.class);
	
	   private static ThreadLocal<Map<String, String>> envProperties = new ThreadLocal<Map<String, String>>() {
	         protected synchronized Map<String,String> initialValue() {
	             return  new HashMap<String,String>();
	         }
	     };
	 
	   private static ThreadLocal<String> project   = new ThreadLocal<String>() {
	         protected synchronized String initialValue() {
	             return  new String();
	         }
	     };
	     
	   private static ThreadLocal<VersionDesc> version   = new ThreadLocal<VersionDesc>() {
	         protected synchronized VersionDesc initialValue() {
		             return  new VersionDesc(" ");
		         }
		     };
		     
	   private static ThreadLocal<DataPool> dataPool   = new ThreadLocal<DataPool>() {
			         protected synchronized DataPool initialValue() {
				             return  new DataPool();
				         }
				     };
	   private static ThreadLocal<ServiceModel> serviceModel   = new ThreadLocal<ServiceModel>() {
					         protected synchronized ServiceModel initialValue() {
						             return  new ServiceModel();
						         }
						     };
						     
      private static ThreadLocal<Connection> connection = new ThreadLocal<Connection>(){
    	  protected synchronized Connection initialValue() {
    		  return null;
    	  }
      };
      
      
						     
	public static ThreadEvaluationContext getInstance(){
		return INSTANCE;
	}     
	/**
	 * @return
	 */
	public static Map<String,String> getEnvProperties(){
		return envProperties.get();
	}

	/**
	 * @param newContext
	 */
	public static void  setEnvProperties(Map<String,String> newContext){
		envProperties.set(newContext);
	}
	/**
	 * @return the project
	 */
	public static String getProject() {
		return project.get();
	}

	/**
	 * @param project the project to set
	 */
	public static void setProject(String newProject) {
		project.set(newProject);
	}

	/**
	 * @return the version
	 */
	public static VersionDesc getVersion() {
		return version.get();
	}
	/**
	 * @return the Data Pool
	 */
	public static DataPool getDataPool() {
		return dataPool.get();
	}

	/**
	 * @param version the version to set
	 */
	public static void setVersion(VersionDesc newVersion) {
		version.set(newVersion);
	}
	/**
	 * @return the Service Model
	 */
	public static ServiceModel getServiceModel() {
		return serviceModel.get();
	}

	/**
	 * @param ServiceModel the project to set
	 */
	public static void setServiceModel(ServiceModel newServiceModel) {
		serviceModel.set(newServiceModel);
	}
	
	public static Connection getConnection(){
		return connection.get();
	}
	
	public static void setConnection(Connection conn){
		connection.set(conn);
	}
	
	
	
	/**
	 * @param newProject
	 * @param newVersion
	 */
	public static void buildEvalContext(String newProject,VersionDesc newVersion){
		buildEvalContext(newProject, newVersion, null);
	}

	/**
	 * @param newProject
	 * @param newVersion
	 * @param javaUDF
	 */
	public static void buildEvalContext(String newProject,VersionDesc newVersion,Map<String,FreeRefFunction> javaUDF){
		freeEvalContext();
   		Map<String,String> envProperties = new HashMap<String,String>();
   		Date date = newVersion.getDate();
		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		envProperties.put(Function.EFFECTIVE_DATE,df.format(date));
		setEnvProperties(envProperties);
		setProject(newProject);
		setVersion(newVersion);
		getDataPool().removeAll();
		
		ServiceModel newServiceModel = ProjectManager.getInstance().getServiceModel(newProject, newVersion);
		setServiceModel(newServiceModel);

	}
	/**
	 * Free context resources
	 * 
	 */
	public static void freeEvalContext(){
		setEnvProperties(null);
		setProject(null);
		setVersion(null);
		getDataPool().removeAll();
		// clean and close connection
		try {
			Connection conn = getConnection();
			if (conn!=null){
				conn.close();
				setConnection(null);
			}
		} catch (SQLException se){
			LOG.error("failed to close connection on thread", se);
		}
	}
}
