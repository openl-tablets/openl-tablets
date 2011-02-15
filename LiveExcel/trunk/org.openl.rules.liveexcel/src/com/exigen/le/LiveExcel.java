/**
 * 
 */
package com.exigen.le;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.evaluator.LiveExcelEvaluator;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.function.UDFRegister;
import com.exigen.le.evaluator.selector.SelectorFactory;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.project.ExternalBranchedWorkbookResolver;
import com.exigen.le.repository.Repository;
import com.exigen.le.repository.RepositoryFactory;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.accessor.ValueHolder;


/**
 * LEEvaluator entry point
 * @author vabramovs
 *
 */
public class LiveExcel {

	private static final Log LOG = LogFactory.getLog(LiveExcel.class);
	
	static private final LiveExcel INSTANCE = new LiveExcel();
	static private boolean initialized = false;

	private LiveExcel(){
	}
	public static  LiveExcel getInstance(){
		return INSTANCE;
	}

	/**
	 * Initialize LiveExcel infrastructure
	 * @param prop
	 */
	public void init(Properties prop) {
		
		if(initialized == false){
		RepositoryFactory.getInstance().init(prop);
		ProjectManager.getInstance().init(prop);
		SelectorFactory.getInstance().init(prop);
		UDFRegister.getInstance().init(prop);
		initialized = true;
		}
	}
	/**
	 * Calculate LiveExcel "service" function
	 * @param projectName
	 * @param versionDesc
	 * @param functionName
	 * @param context
	 * @return
	 */
	public LE_Value calculate(String projectName,VersionDesc versionDesc,String functionName, List<Object> args){
		LE_Value answer;
		LOG.debug("Start calculate function '"+functionName+"' for project: "+projectName+"("+versionDesc.getVersion()+","+versionDesc.getRevisionExcel()+")");
		checkInit();
		if(LOG.isTraceEnabled()){
			String arguments = "Context of function "+functionName+":";
			for(Object arg:args){	
				arguments=arguments+"\n"+SMHelper.valueToString(arg);
			}
			LOG.trace(arguments);
		}
		
	   	try {
	   		
	   		versionDesc = RepositoryFactory.getRepository().resolveVersion(projectName, versionDesc);
	   		
	   		ServiceModel sm = ProjectManager.getInstance().getServiceModel(projectName, versionDesc);
	   		
	   		ThreadEvaluationContext.buildEvalContext(projectName,versionDesc,UDFRegister.getInstance().getJavaUDF());
	   		
		   	
	   		Function func = SelectorFactory.getInstance().getFunctionSelector().selectFunction(functionName, sm.getFunctions(),ThreadEvaluationContext.getInstance());
	   		
	   		Workbook workbook = ProjectManager.getInstance().getWorkbook(projectName, versionDesc, func.getExcel());
	   		
	   		//Build args array and wrap arguments to BeanWrapper if they are basic, has complex type and do not support ValueHolder
	   		Object[] argsArray = new Object[args.size()];
	   		for(int i=0;i<args.size();i++){
	   			if(args.get(i) instanceof ValueHolder || i>=func.getArguments().size() ||func.getArguments().get(i).getType()==null||func.getArguments().get(i).getType().isComplex()==false){
						argsArray[i] = args.get(i);
	   			}
	   			else{
	   					argsArray[i] = new BeanWrapper(args.get(i),func.getArguments().get(i).getType());
	   				}
	   		}
	   		// log data
	   		ProjectManager.getInstance().logData(projectName, versionDesc, argsArray);

			IExternalWorkbookResolver resolver = new ExternalBranchedWorkbookResolver(projectName,versionDesc);
			LiveExcelEvaluator evaluator = new LiveExcelEvaluator(workbook, resolver);
			ValueEval value = evaluator.evaluateServiceModelUDF(functionName, argsArray);
			answer= LE_Value.fromValueEval(value);


		} catch(Throwable e){
			LOG.error("Error during function '"+functionName+ "' calculation.Project:"+projectName+",version:"+versionDesc.getVersion(),e);
			e.printStackTrace();
			throw new RuntimeException("Error during function '"+functionName+ "' calculation.Project:"+projectName+",version:"+versionDesc.getVersion(),e);
			
		}
		finally{
			ThreadEvaluationContext.freeEvalContext();
		}
		
		LOG.debug("Stop calculate function '"+functionName+"' for roject: "+projectName+"("+versionDesc.getVersion()+")");
		if(LOG.isTraceEnabled()){
			String result = "Result of function "+functionName+":";
			result=result+"\n"+SMHelper.valueToString(answer);
			LOG.trace(result);
		}
		return answer;

	}
	
	/**
	 * Get List of service function
	 * @param projectName
	 * @param versionDesc
	 * @return
	 */
	public List<Function> getServiceFunctions(String projectName, VersionDesc versionDesc ){
   		ThreadEvaluationContext.buildEvalContext(projectName,versionDesc);
   		ServiceModel sm = ProjectManager.getInstance().getServiceModel(projectName, versionDesc);
		return sm.getServiceFunctions();
	}
	/**
	 * Get root of service model
	 * @param projectName
	 * @param defaultVersion - version to became default
	 * @return
	 */
	public ServiceModel getServiceModelMakeDefault(String projectName,VersionDesc defaultVersion){
		checkInit();
		if(ProjectManager.getInstance().isProjectActive(projectName)){
			throw new RuntimeException("Project "+projectName+" is active already");
		}
		ServiceModel result = ProjectManager.getInstance().getServiceModel(projectName, defaultVersion);
		setVersionAsDefault(projectName, defaultVersion);
		return result;
		
	}
	/**
	 * Get root of service model
	 * @param projectName
	 * @param version - version to became default
	 * @return
	 */
	public ServiceModel getServiceModel(String projectName,VersionDesc version){
		checkInit();
		ServiceModel result = ProjectManager.getInstance().getServiceModel(projectName, version);
		return result;
		
	}
	/**
	 * Print out service model
	 * @param out
	 * @param projectName
	 * @param versionDesc
	 */
	public void printoutServiceModel(PrintStream out,String projectName,VersionDesc versionDesc){
		checkInit();
		versionDesc = RepositoryFactory.getInstance().resolveVersion(projectName, versionDesc);
		ServiceModel sm = ProjectManager.getInstance().getServiceModel(projectName, versionDesc);
		out.println("Types"+":");
		SMHelper.printoutStructure(out, "", sm.getTypes());
		SMHelper.printoutFunctions(out, "", sm.getFunctions());
		SMHelper.printoutTables(out, "", sm.getTables());
		
	}
	
	/** 
	 * Refresh desired project and set this version as default for it  
	 * @param projectName
	 * @param versionDesc
	 */
	public void refresh(String projectName,VersionDesc versionDesc){
		checkInit();
		ProjectManager.getInstance().refresh(projectName,versionDesc);

	}
	/** 
	 * Refresh all version of  project   
	 * @param projectName
	 */
	public void refresh(String projectName){
		checkInit();
		ProjectManager.getInstance().refresh(projectName);
	}
	/**
	 * Refresh all projects
	 */
	public void refresh(){
		checkInit();
		ProjectManager.getInstance().refresh();
		
	}
	
	/**
	 * free resources for desired version project
	 * @param projectName
	 * @param versionDesc
	 */
	public void clean(String projectName,VersionDesc versionDesc){
		checkInit();
		ProjectManager.getInstance().clean(projectName,versionDesc);
	}
	/** 
	 * free resources for  project
	 * @param projectName
	 */
	public void clean(String projectName){
		checkInit();
		ProjectManager.getInstance().clean(projectName);
	}	
	/**
	 * free resources for all projects
	 * 
	 */
	public void clean(){
		checkInit();
		LOG.info("Server LE try to shutdown by user request");
		ProjectManager.getInstance().clean();
		LOG.info("Server LE has been shutdown by user request");
	}
	
	private void checkInit(){
		if(initialized==false){
			LOG.error("LiveExcel was not initialized before using.");
			throw new RuntimeException("LiveExcel was not initialized before using.");
			
		}
	}
	/**
	 * Get default VersionDesc for project
	 * @param project
	 * @return
	 */
	public VersionDesc getDefaultVersionDesc(String project) {
		checkInit();
		return getDefaultVersionDesc(project,new Date());
	}

	/**
	 * Get default VersionDesc for project
	 * @param project
	 * @return
	 */
	public VersionDesc getDefaultVersionDesc(String project, Date date) {
		checkInit();
		return RepositoryFactory.getInstance().getDefaultVersionDesc(project, date);
	}
	/**
	 * Get list of active projects
	 * @return
	 */
	public List<String> getProjectList(){
		checkInit();
		return ProjectManager.getInstance().getProjectNameList();
	}
	
	
	/**
	 * Get list of active versions for project
	 * @param projectName
	 * @return
	 */
	public List<VersionDesc> getVersionList(String projectName){
		checkInit();
		return ProjectManager.getInstance().getVersionList(projectName);
	} 
	/**
	 * Set version as default for this project
	 * @param project
	 * @param version
	 */
	public void setVersionAsDefault(String project,VersionDesc version){
		checkInit();
		RepositoryFactory.getInstance().setVersionAsDefault(project, version);
	}
	/** get instance of current Repository
	 * @return
	 */
	public Repository getRepository(){
		checkInit();
		return RepositoryFactory.getInstance();
	}

	/**
	 * get actual Properties set
	 * @return
	 */
	public Properties getProperties(){
		checkInit();
		return RepositoryFactory.getInstance().getProperties();
	}
	
	/**
	 * Register new java Class to calculate UDF
	 * New registered UDF will effect to new lifted workbooks, not to cached 
	 * @param functionName
	 * @param executor
	 */
	public void registerJavaFunction(String functionName, LE_Function executor){
		UDFRegister.getInstance().registerJavaFunction(functionName,executor);
	}
	/**
	 * Register new java Class to calculate UDF
	 * New registered UDF will effect to new lifted workbooks, not to cached 
	 * @param functionName
	 * @param executor
	 */
	public void registerJavaUDF(String functionName, FreeRefFunction executor){
		UDFRegister.getInstance().registerJavaUDF(functionName,executor);
	}
	/**
	 * @return the UDFRegister.getInstance().getJavaUDF()
	 */
	public  Map<String, FreeRefFunction> getJavaUDF() {
		return UDFRegister.getInstance().getJavaUDF();
	}
	/**
	 *  Use this method with caution if you REALLY need to reinitilize server
	 * @param initialized the initialized to set
	 */
	public  void setUnInitialized() {
		LiveExcel.initialized = false;
	}

}
