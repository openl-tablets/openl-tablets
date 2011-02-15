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


public class VersionsTest extends TestCase {
	
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
		le.init(prop);
		le.clean();
		le.getServiceModelMakeDefault(projectName,new VersionDesc("d") );
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
		List<Function> servFunc = le.getServiceFunctions(projectName,le.getDefaultVersionDesc(projectName));
		Object[] func =  servFunc.toArray();
		
        VersionDesc version = new VersionDesc(""); 		
		for(int i =0;i<1;i++){
			try {
//				le.calculate(projectName,version,(String)func[i],context);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for(VersionDesc ver:le.getVersionList(projectName)){
			System.out.println("1.Project " +projectName+" Version :"+ver.getVersion());
			assertEquals("d",ver.getVersion());

		}

        version.setVersion(">"); 		
		servFunc = le.getServiceFunctions(projectName,version);
//		for(int i =0;i<1;i++){
//			try {
//				le.function(projectName,version,(String)func[i],context);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		List<VersionDesc> vers = le.getVersionList(projectName);
		for(int index=0;index<etalon.length;index++){
			System.out.println("2.Project " +projectName+" Version :"+vers.get(index).getVersion());
			assertEquals(etalon[index],vers.get(index).getVersion());
			index++;
		}

        version.setVersion("a"); 		
//		for(int i =0;i<1;i++){
//			try {
//				le.function(projectName,version,(String)func[i],context);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
	assertEquals("a",version.getVersion());
	for(VersionDesc ver:le.getVersionList(projectName)){
		System.out.println("3.Project " +projectName+" Version :"+ver.getVersion());
	}
	
	System.out.println("4.Project " +projectName+" Default Version :"+le.getDefaultVersionDesc(projectName).getVersion());
	assertEquals("d",le.getDefaultVersionDesc(projectName).getVersion());
	
	le.setVersionAsDefault(projectName, new VersionDesc("b"));
	
	// Incompatible version - exception
//	try {
//		le.setVersionAsDefault(projectName, new VersionDesc("0"));
//		assertTrue("Incompatibily of service model was not detected",false);
//		
//	} catch (RuntimeException e) {
//		assertEquals("Difference in service model",e.getMessage().substring(0,27));
//		
//	}
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
