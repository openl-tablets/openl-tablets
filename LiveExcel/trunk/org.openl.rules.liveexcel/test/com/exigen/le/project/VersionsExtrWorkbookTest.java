package com.exigen.le.project;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.exigen.le.LiveExcel;

import com.exigen.le.collections.Collections;
import com.exigen.le.collections.Departament;
import com.exigen.le.collections.departament.Person;
import com.exigen.le.smodel.Function;

import junit.framework.TestCase;


public class VersionsExtrWorkbookTest extends TestCase {
	
	@Test
	public void testVersionFunctions(){
		String[] etalon = {"d","d"};
		String projectName = "Collections";
		Collections context = buildContext();
		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");

//		prop.put("Collections.version","a");
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		le.getServiceModelMakeDefault(projectName,new VersionDesc("d") );
		le.printoutServiceModel(System.out, projectName,new VersionDesc(" "));
		
		List<Function> servFunc = le.getServiceFunctions(projectName, le.getDefaultVersionDesc(projectName));
		Object[] func =  servFunc.toArray();
		
//        VersionDesc version = new VersionDesc(""); 		
//		for(int i =0;i<1;i++){
//			try {
//				if(((String)func[i]).contains("AID")){
//					List<LE_Value> answer = le.function(projectName,version,(String)func[i],context);
//					System.out.println("Aid="+answer.get(0).getValue());
//					assertEquals("1000.0",answer.get(0).getValue());
//				}
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
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
