/**
 * 
 */
package com.exigen.le.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.collections.Collections;
import com.exigen.le.collections.Departament;
import com.exigen.le.collections.departament.Person;
import com.exigen.le.evaluator.function.UDFRegister;
import com.exigen.le.evaluator.selector.FunctionByDateSelector;
import com.exigen.le.evaluator.table.TableFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.emulator.JavaUDF;
import com.exigen.le.smodel.emulator.SMEmulator;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class CollectionsCalcTest {
	
	@Test
	public void testVersionFunctions(){
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


        SMEmulator provider = new SMEmulator(new File("./test-resources/LERepository/Collections/d"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);
        ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());


//		prop.put("Collections.version","a");
		ServiceModel sm =le.getServiceModel();
		le.printoutServiceModel(System.out);
		
		BeanWrapper bw = new BeanWrapper(context.getDepartament(), sm.getType("Departament"));
		List<Object> args = new ArrayList<Object>();
		args.add(bw);
		
        Map<String, String> envProps = new HashMap<String, String>();
        envProps.put(Function.EFFECTIVE_DATE, "2010/05/21-08:00");
		List<Function> servFunc = le.getServiceFunctions(envProps);

//		for(int i =10;i<13;i++){
//		for(int i =1;i<2;i++){
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer = le.calculate(servFunc.get(i).getName(),args, envProps).getArray();
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
	}
	
	@Test
	public void testEffectiveDate4Functions(){
		String[] calDate ={"2010/03/01-00:00",
							"2010/07/01-00:00",
							"2011/01/02-00:00",
				
		};
		
		String[][] etalons = {
				new String[]{"January"},  	
				new String[]{"April"},  	
				new String[]{"December"},  	
				
		};


		SMEmulator provider = new SMEmulator(new File("./test-resources/LERepository/Collections/d"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);
        ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());


		le.printoutServiceModel(System.out);
		
		List<Object> args = new ArrayList<Object>();
		

 		for(int i =0;i<calDate.length;i++){
				System.out.println("*******Calculate function(service_Date) on "+calDate[i]);
		        Map<String, String> envProps = new HashMap<String, String>();
		        envProps.put(Function.EFFECTIVE_DATE, calDate[i]);
				LE_Value[][] answer=le.calculate("service_Date".toUpperCase(),args,envProps).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
					System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
					assertEquals(etalons[i][j*answer[j].length+jj], answer[j][jj].getValue());
					}
				}
				
		}
	}
	@Test
	public void testJavaUDFunctions(){
        // JavaUDF will be set via API
		// JavaUDF2 and JavaUDF3 - via configuration properties
		String[][] etalons = {
				new String[]{ // service_JavaUDF
						"Java greets Excel",  // javaUDF
						"Java 2 greets Excel", // javaUDF2
						"Java 3 greets Excel", // javaUDF3
						"Java 4 greets Excel", // javaUDF3
						}, 
				
		};

		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		
		
		// Set our UDF via configuration  properties
		prop.put("UDFFactory.className.proba","com.exigen.le.evaluator.function.FunctionUDFFactoryImpl");
		prop.put("UDFFactory.functionList.proba","JavaUDF2,JavaUDF3");
		
		prop.put("UDF.function.JavaUDF2","com.exigen.le.smodel.emulator.JavaUDF2");
		prop.put("UDF.function.JavaUDF3","com.exigen.le.smodel.emulator.JavaUDF3");
		prop.put("LE_Function.function.JavaFunction4","com.exigen.le.smodel.emulator.JavaFunction4");
		UDFRegister.getInstance().init(prop);
		// Set new SM Emulator
		SMEmulator2 provider = new SMEmulator2(new File("./test-resources/LERepository/Collections/c"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);
        
        ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

		// register our java UDF via API
		LiveExcel.registerJavaUDF("javaUDF",new JavaUDF());
		
		le.printoutServiceModel(System.out);
		 
		List<Object> args = new ArrayList<Object>();
		
        Map<String, String> envProps = new HashMap<String, String>();
        envProps.put(Function.EFFECTIVE_DATE, "2010/05/21-08:00");
		List<Function> servFunc = le.getServiceFunctions(envProps);

//		for(int i =12;i<13;i++){
//		for(int i =10;i<11;i++){
        
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer=le.calculate(servFunc.get(i).getName(),args, envProps).getArray();
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
	}
	@Test
	public void testSM_FromXML(){
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

		// Set regular SM Provider, because it may be switched before to any Emulator
		ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/Collections/d"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);

        ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());
		// Set version "D" 
		// which contains xml with service model 
		// and certain calculation date for results comparison
		
        Map<String, String> envProps = new HashMap<String, String>();
        envProps.put(Function.EFFECTIVE_DATE, "2010/05/21-08:00");
		// Set new SM provider, which get SM from xml

		ServiceModel sm =le.getServiceModel();
		le.printoutServiceModel(System.out);
		
		BeanWrapper bw = new BeanWrapper(context.getDepartament(), sm.getType("Departament"));
		List<Object> args = new ArrayList<Object>();
		args.add(bw);
		
		List<Function> servFunc = le.getServiceFunctions(envProps);

//		for(int i =12;i<13;i++){
		
		for(int i =0;i<servFunc.size();i++){
				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
				LE_Value[][] answer=le.calculate(servFunc.get(i).getName(),args,envProps).getArray();
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
