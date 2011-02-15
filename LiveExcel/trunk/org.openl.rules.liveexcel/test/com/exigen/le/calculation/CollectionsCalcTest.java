/**
 * 
 */
package com.exigen.le.calculation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.collections.Collections;
import com.exigen.le.collections.Departament;
import com.exigen.le.collections.departament.Person;
import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.evaluator.table.TableFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.smodel.emulator.JavaUDF;
import com.exigen.le.smodel.emulator.SMEmulator;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelJAXB;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class CollectionsCalcTest {
	
	@Test
	public void testVersionFunctions(){
		String projectName = "Collections";
		Collections context = buildContext();
		
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
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		
		ProjectManager.getInstance().registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());
		ServiceModelProviderFactory.getInstance().setProvider(new SMEmulator());


//		prop.put("Collections.version","a");
		ServiceModel sm =le.getServiceModelMakeDefault(projectName,new VersionDesc("c") );
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
		BeanWrapper bw = new BeanWrapper(context.getDepartament(), sm.getType("Departament"));
		List<Object> args = new ArrayList<Object>();
		args.add(bw);
		
		List<Function> servFunc = le.getServiceFunctions(projectName,le.getDefaultVersionDesc(projectName));

		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("",date); 		
//		for(int i =10;i<13;i++){
//		for(int i =1;i<2;i++){
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer = le.calculate(projectName,version,servFunc.get(i).getName(),args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
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
		le.clean();

	}
	
	@Test
	public void testEffectiveDate4Functions(){
		String projectName = "Collections";
		String[] calDate ={"2010/03/01-00:00",
							"2010/07/01-00:00",
							"2011/01/02-00:00",
				
		};
		
		String[][] etalons = {
				new String[]{"January"},  	
				new String[]{"April"},  	
				new String[]{"December"},  	
				
		};


		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		
		ProjectManager.getInstance().registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());
		ServiceModelProviderFactory.getInstance().setProvider(new SMEmulator());


//		prop.put("Collections.version","a");
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		ServiceModel sm =le.getServiceModelMakeDefault(projectName,new VersionDesc("c") );
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
		List<Object> args = new ArrayList<Object>();
		

		
        VersionDesc version = new VersionDesc("");
 		for(int i =0;i<calDate.length;i++){
				System.out.println("*******Calculate function(service_Date) on "+calDate[i]);
				String ed = calDate[i];
				DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
				Date date = new Date();
				if(ed != null){
					try {
						date = df.parse(ed);
					} catch (ParseException e) {
						String msg = "Undefined or wrong "+Function.EFFECTIVE_DATE+":"+ed;
						System.out.println("Error "+msg);
						return;
						
					}
					}
				version.setDate(date);
				LE_Value[][] answer=le.calculate(projectName,version,"service_Date".toUpperCase(),args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
					System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
					assertEquals(etalons[i][j*answer[j].length+jj], answer[j][jj].getValue());
					}
				}
				
		}
 		le.clean();
	}
	@Test
	public void testJavaUDFunctions(){
        // JavaUDF will be set via API
		// JavaUDF2 and JavaUDF3 - via configuration properties
		String projectName = "Collections";
		String[][] etalons = {
				new String[]{ // service_JavaUDF
						"Java greets Excel",  // javaUDF
						"Java 2 greets Excel", // javaUDF2
						"Java 3 greets Excel", // javaUDF3
						"Java 4 greets Excel", // javaUDF3
						}, 
				
		};

		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		
		ProjectManager.getInstance().registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

		
		// Set our UDF via configuration  properties
		prop.put("UDFFactory.className.proba","com.exigen.le.evaluator.function.FunctionUDFFactoryImpl");
		prop.put("UDFFactory.functionList.proba","JavaUDF2,JavaUDF3");
		
		prop.put("UDF.function.JavaUDF2","com.exigen.le.smodel.emulator.JavaUDF2");
		prop.put("UDF.function.JavaUDF3","com.exigen.le.smodel.emulator.JavaUDF3");
		prop.put("LE_Function.function.JavaFunction4","com.exigen.le.smodel.emulator.JavaFunction4");
		// Set new SM Emulator
		ServiceModelProviderFactory.getInstance().setProvider(new SMEmulator2());

		le.setUnInitialized();
		le.init(prop);
		le.clean();
		
		// register our java UDF via API
		le.registerJavaUDF("javaUDF",new JavaUDF());
		
		ServiceModel sm =le.getServiceModelMakeDefault(projectName,new VersionDesc("c") );
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		 
		List<Object> args = new ArrayList<Object>();
		
		List<Function> servFunc = le.getServiceFunctions(projectName,le.getDefaultVersionDesc(projectName));

		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("",date); 		
//		for(int i =12;i<13;i++){
//		for(int i =10;i<11;i++){
        
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer=le.calculate(projectName,version,servFunc.get(i).getName(),args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
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
		le.clean();

	}
	@Test
	public void testSM_FromXML(){
		String projectName = "Collections";
		Collections context = buildContext();
		
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
		le.setUnInitialized();
		
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		
		le.init(prop);
		le.clean();

		ProjectManager.getInstance().registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

		// Set regular SM Provider, because it may be switched before to any Emulator
		ServiceModelProviderFactory.getInstance().setProvider(new ServiceModelJAXB());

		// Set version "D" 
		// which contains xml with service model 
		// and certain calculation date for results comparison
		
		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("d",date); 		
		// Set new SM provider, which get SM from xml
		ServiceModelProviderFactory.getInstance().setProvider(new ServiceModelJAXB());

		ServiceModel sm =le.getServiceModelMakeDefault(projectName,version );
		le.printoutServiceModel(System.out, projectName,version);
		
		BeanWrapper bw = new BeanWrapper(context.getDepartament(), sm.getType("Departament"));
		List<Object> args = new ArrayList<Object>();
		args.add(bw);
		
		List<Function> servFunc = le.getServiceFunctions(projectName,le.getDefaultVersionDesc(projectName));

//		for(int i =12;i<13;i++){
		
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer=le.calculate(projectName,version,servFunc.get(i).getName(),args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
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
		le.clean();
	}
		static public  Collections  buildContext(){ 
		    // Set context
		    Departament departament = new Departament();
		    String[] projects = new String[]{"Project 1","Project 2","Project 3"};
		    departament.setProject(projects);
		    Person chief = new Person();
		    chief.setName("St.Patrick");
		   Double[] salary = new Double[]{
		    		new Double(100),
		    		new Double(110),
		    		new Double(120),
		    		new Double(130),
		    		new Double(140),
		    		new Double(340),
		    };
		    chief.setSalary(salary);
		    Person person1 = new Person();
		    person1.setName("Anybody");
		    salary = new Double[]{
		    		new Double(100)/2,
		    		new Double(110)/2,
		    		new Double(120)/2,
		    		new Double(130)/2,
		    		new Double(140)/2,
		    };
		    person1.setSalary(salary);
		    
		    Person person2 = new Person();
		    person2.setName("Poor Jo");
		    salary = new Double[]{
		    		new Double(100)/10,
		    		new Double(110)/10,
		    		new Double(120)/10,
		    		new Double(130)/10,
		    		new Double(140)/10,
		    };
		    person2.setSalary(salary);

		    departament.setPerson(new Person[]{chief,person1,person2});
		    
		    Collections context = new Collections();
		    context.setDepartament(departament);
		    
		    return context;
			}

}
