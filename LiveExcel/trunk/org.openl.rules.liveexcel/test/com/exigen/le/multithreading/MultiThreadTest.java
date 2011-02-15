package com.exigen.le.multithreading;

import static junit.framework.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.calculation.CollectionsCalcTest;
import com.exigen.le.collections.Collections;
import com.exigen.le.democase.BuildDemoCaseObjects;
import com.exigen.le.democase.Coverage;
import com.exigen.le.democase.Driver;
import com.exigen.le.democase.Policy;
import com.exigen.le.democase.Vehicle;
import com.exigen.le.evaluator.table.TableFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.emulator.SMEmulator;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;



@Ignore
public class MultiThreadTest {
	static Log log = LogFactory.getLog(MultiThreadTest.class);	

	static final int THREAD_NUMBER = 5;
	static final int TASK_NUMBER = 10;
	static final int TASK_PORTION = 10000;
	static final List<Vehicle> vehicles = BuildDemoCaseObjects.createVehicles();
	static final List<Policy> policies = BuildDemoCaseObjects.createPolicies();
	static final List<Driver> drivers = BuildDemoCaseObjects.createDrivers();
	static final List<Coverage> coverages = BuildDemoCaseObjects.createCoverages();
	static final Collections context =CollectionsCalcTest.buildContext();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		
		// first param - thread pool size. 
		int threadNumber = THREAD_NUMBER;
		int taskNumber = TASK_NUMBER;
		int taskPortion = TASK_PORTION;
		if (args.length >0){
			threadNumber = Integer.parseInt(args[0]);
		}
		if (args.length >1){
			taskNumber = Integer.parseInt(args[1]);
		}
		if (args.length >2){
			taskPortion = Integer.parseInt(args[2]);
		}
		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		le.init(prop);
		
		VersionDesc versionDesc = new VersionDesc("");
		String projectName = "DemoCase2";
		ServiceModel sm =le.getServiceModel(projectName,versionDesc );
		le.printoutServiceModel(System.out, projectName,versionDesc);
		
		projectName = "Collections";
		versionDesc = new VersionDesc("d");
		sm =le.getServiceModel(projectName,versionDesc );
		le.printoutServiceModel(System.out, projectName,versionDesc);
		
		projectName = "Tables";
		versionDesc = new VersionDesc("0");
		sm =le.getServiceModel(projectName,versionDesc);
		le.printoutServiceModel(System.out, projectName,versionDesc);
		
		MultiThreadTest test = new MultiThreadTest();
		test.dummy(0);
		test.dummy(1);
		test.dummy(2);
	
		test.test(threadNumber, taskNumber, taskPortion);
		le.clean();
	    System.out.println("Done");
		
	}
	
	public void test(int poolSize, int taskListSize, int portion)throws Exception{
		  final int timeout = 5; // timeout in minuts
		   double sum = 0;
		   int cntr = 0;
		  
		  long starttime = System.nanoTime();
		  int iterationCount = (int)Math.ceil((double)taskListSize/(double)portion);
		  for(int portionIndex=0;portionIndex < iterationCount;portionIndex++){
			  ExecutorService es = Executors.newFixedThreadPool(poolSize);
			  List<Future<Long>> result;
				  try{
					  if(portionIndex==(iterationCount-1)){ // Last(single) loop
						  result = es.invokeAll(getCallableListPortion(taskListSize-portion*portionIndex,portion*portionIndex));
					  }
					  else{
						  result = es.invokeAll(getCallableListPortion(portion,portion*portionIndex));
					  }
					  es.shutdown();
				  
					  if (!es.awaitTermination(timeout*60, TimeUnit.SECONDS))
						  log.error("failed to calculate in time");
					  for (Future<Long>f : result){
						  if (f.isDone()){
							  cntr++;
							  sum += f.get().longValue();
						  }
					  }
				  }finally{
					   if(!es.isShutdown())
					    es.shutdownNow();
					  }
			  }
	   log.info("Number of calculations: " + cntr);
	   if(taskListSize >= portion){
		   log.info(" WARNING! Tasks have been input  by portion. So Total time includes delay for portion wait. ");
	   }
	   if (cntr != 0) 
	     log.info("average(milisec.) : " + (sum/cntr)/(1.E6));
		long endtime = System.nanoTime();
	    log.info("Total (milisec.) : " + (endtime-starttime)/(1.E6));
	}
	
	@SuppressWarnings("static-access")
	public List<Callable<Long>> getCallableListPortion(int listSize, int base){
		List <Callable<Long>> result = new LinkedList<Callable<Long>>();
		for (int i=0; i< listSize;i++){
//			result.add(getCallable(i%10));
			result.add(getCallable(i+base));
		}
		return result;
	}
	
	
	public Callable<Long> getCallable(final int i){
		return new Callable<Long>(){
			public Long call(){
				long startTime = System.nanoTime();
				dummy(i);
			    long endTime = System.nanoTime();
			    return endTime-startTime;
			}
		};
	}

	private void dummy(int taskID){
	try {
		switch(taskID%3){
		case 1:
			testDemocase2(taskID);
			break;
		case 0:
			testCollections(taskID);
			break;
		case 2:
			testTables(taskID);
			break;
			
		}
	} catch (Exception e) {
		// TODO Auto-generated catch block
		System.out.println("!!!!!!!!!!!!! Error: ");
		e.printStackTrace();
	}	
//	if(taskID>0&&taskID%52==0){
//		try {
//			LiveExcel.getInstance().clean();
//			System.out.println("$$$$$$$$$$$$$$$$$$$$ REsources was freed");
//		} catch (Exception e) {
//			log.error("Error while clean",e);
//			e.printStackTrace();
//		}
//		finally{
////			System.exit(0);
//		}
//	}
	}
	private void testCollections(int taskID){
		String projectName = "Collections";
		
		String[][] etalons = {
				new String[]{     // SERVICE_CHIEFSALARY
						"100.0",
						"110.0",
						"120.0",
						"130.0",
						"140.0",
						"340.0",
			
				},
			new String[]{"156.66666666666666"}, // SERVICE_SAVEARAGE 	
			new String[]{"11.0",},     // SERVICE_JOSALARY 
			new String[]{"1000.0",},     // SERVICE_JOSAID 
			new String[]{"Project 1",},  // SERVICE_MAINPROJECT
			new String[]{				// 'SERVICE_PROJECTS
						"Project 1",
						"Project 2",
						"Project 3",
				},
			new String[]{"1220.0",},     // SERVICE_JODebt 
			new String[]{"15",},     // SERVICE_DATE - ERROR due to wrong argument count
			new String[]{            // SERVICE_AGGRERATE
						"Project 1",
						"Project 1",
						"100.0",
						"11.0",
						"156.66666666666666",
						"1000.0",
						"April",
						"1220.0",
				},
			new String[]{"Project 12010/05/21-08:00",}, // SERVICE_TABLEPROJECT
			new String[]{}, // STUFF  - service_GetObject
			new String[]{}, // STUFF  - service_GetVObject
			new String[]{}, // STUFF   - service_GetRefObject
			new String[]{     // SERVICE_CHIEFSALREFOBJECT
						"100.0",
						"110.0",
						"120.0",
						"130.0",
						"140.0",
						"340.0",
			
				},
		};


		LiveExcel le = LiveExcel.getInstance();
 		
		
		List<Function> servFunc = le.getServiceFunctions(projectName,le.getDefaultVersionDesc(projectName));

		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("",date); 		
 		ServiceModel sm =le.getServiceModel(projectName,new VersionDesc("d") );
		BeanWrapper bw = new BeanWrapper(context.getDepartament(), sm.getType("Departament"));
		List<Object> args = new ArrayList<Object>();
		args.add(bw);
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer=le.calculate(projectName,version,servFunc.get(i).getName(),args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
						System.out.printf("[Collections]Thread - %s;Task -%d ",
								Thread.currentThread().getName(),
								taskID);
					System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
					if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
						SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
					}
					else{  // 
						assertEquals(etalons[i][j*answer[j].length+jj], answer[j][jj].getValue());
					}
					}
				}
				
		}
}
	private void testTables(int taskID){
			String projectName = "Tables";
			int[] arg1Set = new int[]{1,2,3,4,5,6,};
			String[] arg2Set = new String[]{"X1","X2","X3"};
			
			String[][] etalons = {
					new String[]{"V1"},
					new String[]{"V1"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"7.0"},
					new String[]{"7.0"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"42"},
					new String[]{"V3"},
					new String[]{"V3"},
			};


			LiveExcel le = LiveExcel.getInstance();

			String function = "service_calcSimplestTable";

			DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
			Date date=new Date();
			try {
				date = df.parse("2010/05/21-08:00");
			} catch (ParseException e) {
				e.printStackTrace();
			}
	        VersionDesc version = new VersionDesc("0",date); 		
			System.out.println("*******Calculate function(service) "+function);
			for(int i=0;i<arg2Set.length;i++){
				String arg2 = arg2Set[i];
				for(int ii=0;ii<arg1Set.length;ii++){
					int arg1= arg1Set[ii];
					List<Object> args = new ArrayList<Object>();
					args.add(new Double(arg1));
					args.add(arg2);
					LE_Value[][] answer=le.calculate(projectName,version,function,args).getArray();
					for(int j=0;j<answer.length;j++){
						for(int jj=0;jj<answer[j].length;jj++){
							System.out.printf("[Tables]Thread - %s;Task -%d ",
									Thread.currentThread().getName(),
									taskID);
							System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
							if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
								SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
							}
							else{  // 
								assertEquals(etalons[i*arg1Set.length+ii][j*answer[j].length+jj], answer[j][jj].getValue());
							}
						}
					}
				}
					
			}
		
	}
	private void testDemocase2(int taskID){	
		String projectName = "DemoCase2";
		
		String[][][][] etalons = {
		   new String[][][]{	
			 new String[][] {  
				 new String[]	{
						 "282.19",
						 "343.07",
					 },
				 new String[]	{
						 "212.31",
						 "257.83",
					},
				new String[]	{
						"196.14",
						"238.11",
				},
			 },	
			 new String[][] {  
				new String[]	{
						"254.19",
						"308.91",
				},
				new String[]	{
						"191.35",
						"232.27",
				},
				new String[]	{
						"226.66",
						"275.32",
				},
			 },
		   },
	   new String[][][]{
			 new String[][] {  
						new String[]	{
								"264.1",
								"320.98",
						},
						new String[]	{
								"198.76",
								"241.3",
						},
						 new String[]	{
								"183.65",
								"222.86"},
						},
			 new String[][] {  
				 new String[]	{
						 "237.9",
						 "289.02",
				},
				 new String[]	{
						 "179.16",
						 "217.39",
				},
				 new String[]	{
						 "212.17",
						 "257.65",
				},
			 },	
		   },	 
	   new String[][][]{
			 new String[][] {  
				 new String[]	{
						 "794.94",
						 "966.22",
				},
				 new String[]	{
						 "514.52",
						 "624.22",
				},
				 new String[]	{
						 "482.38",
						 "585.04",
				},
			 },	
			 new String[][] {  
				 new String[]	{
						 "766.69",
						 "931.76",
				},
				 new String[]	{
						 "496.42",
						 "602.15",
				},
				 new String[]	{
						 "506.02",
				 		"613.86"},
				},
		   },	
		};


		LiveExcel le = LiveExcel.getInstance();
		String function = "rateAutoLE";

		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("",date); 
        
		ServiceModel sm =le.getServiceModel(projectName,new VersionDesc("") );

		System.out.println("*******Calculate function(service) "+function);
//		for(Coverage coverage:coverages){
		for(int i=0;i<3;i++){
			Coverage coverage = coverages.get(i);
			BeanWrapper bw1 = new BeanWrapper(coverage, sm.getType("Coverage"));
//			for(Vehicle vehicle:vehicles){
			for(int ii = 0;ii<2;ii++){
				Vehicle vehicle=vehicles.get(ii);
				BeanWrapper bw2 = new BeanWrapper(vehicle, sm.getType("Vehicle"));
//				for(Driver driver:drivers){
				for(int iii = 0;iii<3;iii++){
					Driver driver=drivers.get(iii);
					BeanWrapper bw3 = new BeanWrapper(driver, sm.getType("Driver"));
//					for(Policy policy:policies){
					for(int iiii=taskID%2;iiii<2;iiii++){
						Policy policy=policies.get(iiii);
						BeanWrapper bw4 = new BeanWrapper(policy, sm.getType("Policy"));
						List<Object> args = new ArrayList<Object>();
						args.add(bw1);
						args.add(bw2);
						args.add(bw3);
						args.add(bw4);
						LE_Value[][] answer=le.calculate(projectName,version,function,args).getArray();
						for(int j=0;j<answer.length;j++){
							for(int jj=0;jj<answer[j].length;jj++){
								System.out.printf("[DemoCase]Thread - %s;Task -%d [v=%d][c=%d][d=%d][p=%d]",
										Thread.currentThread().getName(),
										taskID,
										vehicles.indexOf(vehicle)+1,coverages.indexOf(coverage)+1,
										drivers.indexOf(driver)+1,policies.indexOf(policy)+1);
								System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
								if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
									SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
								}
								else{  // 
									assertEquals(etalons[i][ii][iii][iiii], answer[j][jj].getValue());
								}
							}
						}
					}
				}
			}
		}
		return ;
	}
	
}
