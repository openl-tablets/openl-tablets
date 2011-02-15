package com.exigen.le.smodel.emulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Cell;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Range;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.Function.FunctionArgument;
import com.exigen.le.smodel.provider.ServiceModelProvider;

public class SMModelEmulator implements ServiceModelProvider {

	public ServiceModel create(String projectName, VersionDesc versionDesc) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Function> findFunctions(String projectName,
			VersionDesc versionDesc, List<Type> types) {
		List<Function> result = new ArrayList<Function>();
		
		List<FunctionArgument> arguments = new ArrayList<FunctionArgument>();
//		arguments.add(arg);
		
		
		Function service_func = new Function();
		service_func.setName("service_func1");
		service_func.setExcel("SM");
		service_func.setSheet("Sheet2");
		service_func.setFunctionDescription("Test function");
		service_func.setReturnType(null);
		service_func.setReturnSpace(new Cell().init("B1"));
		service_func.setArguments(arguments);
		service_func.setService(true);
		result.add(service_func);
		
		return result;
	}

	public List<TableDesc> findTables(String projectName,
			VersionDesc versionDesc) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Type> findTypes(String projectName, VersionDesc versionDesc) {
		
		
		Type person = new Type("Person",true);
		Type person_Course_Work=new Type("Person.Course_Work",true);
		Type person_Summer_School=new Type("Person.Summer_School",true);
		Type person_Current_Job = new Type("Person.Current_Job",true);
		Type person_Friend = new Type("Person.Friend",true);
		
		Type school = new Type("School",true);
		Type school_Class_Offered = new Type("School.Class_Offered",true);
		
		Type job = new Type("Job",true);
		
		Type company = new Type("Company",true);
		
		
		
		
		// --- Person
		person.setName("Person");
		person.setComplex(true);
		person.setPath("Person");
		List<MappedProperty> childsPerson = new LinkedList<MappedProperty>();
		childsPerson.add(new MappedProperty("SSN", Type.STRING)); //SSN
		childsPerson.add(new MappedProperty("Name", Type.STRING)); // Name
		childsPerson.add(new MappedProperty("DOB", Type.DATE)); // DOB
		childsPerson.add(new MappedProperty("Current_Income", Type.DOUBLE)); // Current Income
		childsPerson.add(new MappedProperty("Mother", person));
		childsPerson.add(new MappedProperty("Father", person));
		childsPerson.add(new MappedProperty("Course_Work", person_Course_Work, true, true));
		childsPerson.add(new MappedProperty("Summer_School", person_Summer_School, true, true));
		childsPerson.add(new MappedProperty("Current_Job", person_Current_Job, true, false));
		childsPerson.add(new MappedProperty("Friend", person_Friend, true, true));
		person.setChilds(childsPerson);
		
		// -- Person.Course_Work
		person_Course_Work.setName("Course_Work");
		person_Course_Work.setComplex(true);
		person_Course_Work.setPath("Person.Course_Work");
		List<MappedProperty> childsPCW = new LinkedList<MappedProperty>();
		childsPCW.add(new MappedProperty("School_Class_Offered",school_Class_Offered));
		childsPCW.add(new MappedProperty("Year_Taken", Type.DOUBLE));
		childsPCW.add(new MappedProperty("Grade_Received", Type.STRING));
		person_Course_Work.setChilds(childsPCW);
		
		// Person. Summer_School
		person_Summer_School.setName("Summer_School");
		person_Summer_School.setComplex(true);
		person_Summer_School.setPath("Person.Summer_School");
		List<MappedProperty> childsSS = new LinkedList<MappedProperty>();
		childsSS.add(new MappedProperty("School", school, false, true));
		childsSS.add(new MappedProperty("Year_Went", Type.DOUBLE));
		person_Summer_School.setChilds(childsSS);
		
		//Person. Current_Job
		person_Current_Job.setName("Current_Job");
		person_Summer_School.setComplex(true);
		person_Summer_School.setPath("Person.Current_Job");
		List<MappedProperty> childsCJ = new LinkedList<MappedProperty>();
		childsCJ.add(new MappedProperty("Start_Year",Type.DATE));
		childsCJ.add(new MappedProperty("Pay",Type.DOUBLE));
		childsCJ.add(new MappedProperty("Employer",company));
		childsCJ.add(new MappedProperty("Job",job));
		person_Current_Job.setChilds(childsCJ);
		
		// Person. Friend
		person_Friend.setName("Friend");
		person_Friend.setComplex(true);
		person_Friend.setPath("Person.Friend");
		List<MappedProperty> childsF = new LinkedList<MappedProperty>();
		childsF.add(new MappedProperty("Person",person));
		childsF.add(new MappedProperty("Rating", Type.DOUBLE));
		person_Friend.setChilds(childsF);
		
		// School
		school.setName("School");
		school.setComplex(true);
		school.setPath("School");
		List<MappedProperty> childsS = new LinkedList<MappedProperty>();
		childsS.add(new MappedProperty("Name", Type.STRING));
		childsS.add(new MappedProperty("Class_Offered", school_Class_Offered, true, true));
		school.setChilds(childsS);
		
		// School.Class Offered
		school_Class_Offered.setName("Class_Offered");
		school_Class_Offered.setComplex(true);
		school_Class_Offered.setPath("School.Class_Offered");
		List<MappedProperty> childsSCO = new LinkedList<MappedProperty>();
		childsSCO.add(new MappedProperty("Code", Type.STRING));
		childsSCO.add(new MappedProperty("Name", Type.STRING));
		childsSCO.add(new MappedProperty("Prof", person));
		school_Class_Offered.setChilds(childsSCO);
		
		//Job
		job.setName("Job");
		job.setComplex(true);
		job.setPath("Job");
		List<MappedProperty> childsJ = new LinkedList<MappedProperty>();
		childsJ.add(new MappedProperty("Title", Type.STRING));
		childsJ.add(new MappedProperty("Average_Pay_grade", Type.DOUBLE));
		job.setChilds(childsJ);
		
		//Company
		company.setName("Company");
		company.setComplex(true);
		company.setPath("Company");
		List<MappedProperty>childsC = new LinkedList<MappedProperty>();
		childsC.add(new MappedProperty("Name", Type.STRING));
		childsC.add(new MappedProperty("Country_HQ", Type.STRING));
		company.setChilds(childsC);
		
		List<Type> types = new LinkedList<Type>();
		types.add(person);
		types.add(school);
		types.add(job);
		types.add(company);
		
		return types;
	}
	
}
