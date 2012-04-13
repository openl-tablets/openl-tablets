/**
 * 
 */
package com.exigen.le.smodel.emulator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exigen.le.LE_Value;
import com.exigen.le.smodel.Cell;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Range;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.Function.FunctionArgument;
import com.exigen.le.smodel.TableDesc.ColumnDesc;
import com.exigen.le.smodel.TableDesc.DataType;
import com.exigen.le.smodel.provider.ServiceModelProvider;

/**
 * @author vabramovs
 *
 */
public class SMEmulator implements ServiceModelProvider {
    
    private final File projectLocation;
    
    public SMEmulator(File projectLocation) {
        this.projectLocation = projectLocation;
    }

    
	public List<Type> findTypes() {
		List<Type> result = new ArrayList<Type>();
		
		Type numeric = new Type();
		numeric.setComplex(false);
		numeric.setName(LE_Value.TypeString.NUMERIC);
		
		Type str = new Type();
		str.setComplex(false);
		str.setName(LE_Value.TypeString.STRING);
		
		Type person = new Type();
		person.setComplex(true);
		person.setName("Person");
		
		List<MappedProperty> personChilds = new ArrayList<MappedProperty>();
		MappedProperty salary = new MappedProperty();
		salary.setCollection(true);
		salary.setName("salary");
		salary.setType(numeric);
		salary.setTypeName(LE_Value.TypeString.NUMERIC);
		personChilds.add(salary);
		MappedProperty name = new MappedProperty();
		name.setCollection(false);
		name.setName("name");
		name.setType(str);
		name.setTypeName(LE_Value.TypeString.STRING);
		name.setKey(true);
		personChilds.add(name);
		
		person.setChilds(personChilds);
		
		

		Type departament = new Type();
		departament.setComplex(true);
		departament.setName("Departament");
		
		List<MappedProperty> departamentChilds = new ArrayList<MappedProperty>();
		MappedProperty employee = new MappedProperty();
		employee.setCollection(true);
		employee.setName("person");
		employee.setType(person);
		employee.setTypeName("Person");
		
		employee.setEmbedded(false);
		departamentChilds.add(employee);

		MappedProperty project = new MappedProperty();
		project.setCollection(true);
		project.setName("project");
		project.setType(str);
		project.setTypeName(LE_Value.TypeString.STRING);
		departamentChilds.add(project);
		departament.setChilds(departamentChilds);
		
		result.add(departament);
		result.add(person);
		
		return result;
	}
	public List<Function> findFunctions(List<Type> types) {
		List<Function> result = new ArrayList<Function>();
		
		FunctionArgument arg = new FunctionArgument();
		for(Type type:types){
			if(type.getName().equals("Departament".toUpperCase())){
				arg.setType(type);
				arg.setTypeName(type.getName());
				
			}
		}
		arg.setDescription("Departament");
		arg.setInput(new Cell().init("B2"));
		List<FunctionArgument> arguments = new ArrayList<FunctionArgument>();
		arguments.add(arg);
		
		
		Function service_chiefSalary = new Function();
		service_chiefSalary.setName("service_chiefSalary");
		service_chiefSalary.setExcel("Collections");
		service_chiefSalary.setSheet("Salary");
		service_chiefSalary.setFunctionDescription("Get chief salary");
		service_chiefSalary.setReturnType(null);
		service_chiefSalary.setReturnSpace(new Cell().init("B1"));
		service_chiefSalary.setArguments(arguments);
		service_chiefSalary.setService(true);
		result.add(service_chiefSalary);
		
		Function service_SAverage = new Function();
		service_SAverage.setName("service_SAverage");
		service_SAverage.setExcel("Collections");
		service_SAverage.setSheet("Salary");
		service_SAverage.setFunctionDescription("Get average salary");
		service_SAverage.setReturnType(null);
		service_SAverage.setReturnSpace(new Cell().init("B3"));
		service_SAverage.setArguments(arguments);
		service_SAverage.setService(true);
		result.add(service_SAverage);
		
		Function service_JoSalary = new Function();
		service_JoSalary.setName("service_JoSalary");
		service_JoSalary.setExcel("Collections");
		service_JoSalary.setSheet("Salary");
		service_JoSalary.setFunctionDescription("Get Jo salary");
		service_JoSalary.setReturnType(null);
		service_JoSalary.setReturnSpace(new Cell().init("B5"));
		service_JoSalary.setArguments(arguments);
		service_JoSalary.setService(true);
		result.add(service_JoSalary);

		Function service_JoAid = new Function();
		service_JoAid.setName("service_JoAid");
		service_JoAid.setExcel("Collections");
		service_JoAid.setSheet("Salary");
		service_JoAid.setFunctionDescription("Get Joa aid");
		service_JoAid.setReturnType(null);
		service_JoAid.setReturnSpace(new Cell().init("B7"));
		service_JoAid.setArguments(arguments);
		service_JoAid.setService(true);
		result.add(service_JoAid);
		
		Function service_mainProject = new Function();
		service_mainProject.setName("service_mainProject");
		service_mainProject.setExcel("Collections");
		service_mainProject.setSheet("Project");
		service_mainProject.setFunctionDescription("Get main project");
		service_mainProject.setReturnType(null);
		service_mainProject.setReturnSpace(new Cell().init("B1"));
		service_mainProject.setArguments(arguments);
		service_mainProject.setService(true);
		result.add(service_mainProject);

		Function service_projects = new Function();
		service_projects.setName("service_projects");
		service_projects.setExcel("Collections");
		service_projects.setSheet("Project");
		service_projects.setFunctionDescription("Get project list");
		service_projects.setReturnType(null);
		service_projects.setReturnSpace(new Cell().init("B3"));
		service_projects.setArguments(arguments);
		service_projects.setService(true);
		result.add(service_projects);
		
		
		Function service_JoDebt = new Function();
		service_JoDebt.setName("service_JoDebt");
		service_JoDebt.setExcel("Collections");
		service_JoDebt.setSheet("Salary");
		service_JoDebt.setFunctionDescription("Get Jo Debt");
		service_JoDebt.setReturnType(null);
		service_JoDebt.setReturnSpace(new Cell().init("B9"));
		service_JoDebt.setArguments(arguments);
		service_JoDebt.setService(true);
		result.add(service_JoDebt);
		
		Function bank_Claim = new Function();
		bank_Claim.setName("bank_Claim");
		bank_Claim.setExcel("Aid");
		bank_Claim.setSheet("Sheet2");
		bank_Claim.setFunctionDescription("Get Bank Claim");
		bank_Claim.setReturnType(null);
		bank_Claim.setReturnSpace(new Cell().init("B1"));
		bank_Claim.setArguments(arguments);
		bank_Claim.setService(false);
		result.add(bank_Claim);
		
		List<FunctionArgument> empty = new ArrayList<FunctionArgument>();

		Function service_Date1 = new Function();
		service_Date1.setName("service_Date");
		service_Date1.setExcel("Collections");
		service_Date1.setSheet("Date");
		service_Date1.setFunctionDescription("Get Activation Month - Junuary");
		service_Date1.setReturnType(null);
		service_Date1.setReturnSpace(new Cell().init("B1"));
		service_Date1.setArguments(empty);
		service_Date1.setService(true);
		Map<String,String> attrib1 = new HashMap<String,String>();
		attrib1.put(Function.EFFECTIVE_DATE, "2010/01/01-00:00");
		service_Date1.setAttributes(attrib1);
		result.add(service_Date1);
		
		Function service_Date2 = new Function();
		service_Date2.setName("service_Date");
		service_Date2.setExcel("Collections");
		service_Date2.setSheet("Date");
		service_Date2.setFunctionDescription("Get Activation Month - April");
		service_Date2.setReturnType(null);
		service_Date2.setReturnSpace(new Cell().init("B2"));
		service_Date2.setArguments(empty);
		service_Date2.setService(true);
		Map<String,String> attrib2 = new HashMap<String,String>();
		attrib2.put(Function.EFFECTIVE_DATE, "2010/04/01-00:00");
		service_Date2.setAttributes(attrib2);
		result.add(service_Date2);
		
		Function service_Date32 = new Function();
		service_Date32.setName("service_Date");
		service_Date32.setExcel("Collections");
		service_Date32.setSheet("Date");
		service_Date32.setFunctionDescription("Get Activation Month-December");
		service_Date32.setReturnType(null);
		service_Date32.setReturnSpace(new Cell().init("B3"));
		service_Date32.setArguments(empty);
		service_Date32.setService(true);
		Map<String,String> attrib3 = new HashMap<String,String>();
		attrib3.put(Function.EFFECTIVE_DATE, "2010/12/01-00:00");
		service_Date32.setAttributes(attrib3);
		result.add(service_Date32);
		
		Function service_Aggregation = new Function();
		service_Aggregation.setName("service_Aggregation");
		service_Aggregation.setExcel("Collections");
		service_Aggregation.setSheet("Aggregations");
		service_Aggregation.setFunctionDescription("Calculate all functions");
		service_Aggregation.setReturnType(null);
		service_Aggregation.setReturnSpace(new Cell().init("B10"));
		service_Aggregation.setArguments(arguments);
		service_Aggregation.setService(true);
		result.add(service_Aggregation);
		
		Function real_Aggregation = new Function();
		real_Aggregation.setName("real_Aggregation");
		real_Aggregation.setExcel("Collections");
		real_Aggregation.setSheet("Aggregations");
		real_Aggregation.setFunctionDescription("Calculate all functions");
		real_Aggregation.setReturnType(null);
		real_Aggregation.setReturnSpace(new Range().init("B4:C7"));
		real_Aggregation.setArguments(arguments);
		real_Aggregation.setService(false);
		result.add(real_Aggregation);
		
		Function service_TableProject = new Function();
		service_TableProject.setName("service_TableProject");
		service_TableProject.setExcel("Collections");
		service_TableProject.setSheet("Project");
		service_TableProject.setFunctionDescription("Get project list");
		service_TableProject.setReturnType(null);
		service_TableProject.setReturnSpace(new Cell().init("B5"));
		service_TableProject.setArguments(arguments);
		service_TableProject.setService(true);
		result.add(service_TableProject);
		
		Function service_GetObject = new Function();
		service_GetObject.setName("service_GetObject");
		service_GetObject.setExcel("Collections");
		service_GetObject.setSheet("TestData");
		service_GetObject.setFunctionDescription("Collect object from horizontal range");
		service_GetObject.setReturnType(arg.getType());  // Output - the same as input
		service_GetObject.setReturnSpace(new Range().init("TestData!C3:TestData!E21"));
		service_GetObject.setArguments(arguments);
		service_GetObject.setService(true);
		result.add(service_GetObject);
		
		Function service_GetVObject = new Function();
		service_GetVObject.setName("service_GetVObject");
		service_GetVObject.setExcel("Collections");
		service_GetVObject.setSheet("TestData");
		service_GetVObject.setFunctionDescription("Collect object from vertical range");
		service_GetVObject.setReturnType(arg.getType());  // Output - the same as input
		service_GetVObject.setReturnSpace(new Range().init("TestData!C26:TestData!I28"));
		service_GetVObject.setArguments(arguments);
		service_GetVObject.setService(true);
		result.add(service_GetVObject);
		
		Function service_GetRefObject = new Function();
		service_GetRefObject.setName("service_GetRefObject");
		service_GetRefObject.setExcel("Collections");
		service_GetRefObject.setSheet("TestData");
		service_GetRefObject.setFunctionDescription("Collect object with referencies");
		service_GetRefObject.setReturnType(arg.getType());  // Output - the same as input
		service_GetRefObject.setReturnSpace(new Range().init("TestData!C31:TestData!E49"));
		service_GetRefObject.setArguments(arguments);
		service_GetRefObject.setService(true);
		result.add(service_GetRefObject);
		
		Function service_chiefSalRefObject = new Function();
		service_chiefSalRefObject.setName("service_chiefSalRefObject");
		service_chiefSalRefObject.setExcel("Collections");
		service_chiefSalRefObject.setSheet("Aggregations");
		service_chiefSalRefObject.setFunctionDescription("Get chief salary from compound object with referencies");
		service_chiefSalRefObject.setReturnType(null); 
		service_chiefSalRefObject.setReturnSpace(new Cell().init("Aggregations!B14"));
		service_chiefSalRefObject.setArguments(arguments);
		service_chiefSalRefObject.setService(true);
		result.add(service_chiefSalRefObject);
		return result;
	}
	public List<TableDesc> findTables() {
		List<TableDesc> result = new ArrayList<TableDesc>();
		
		
		List<ColumnDesc> cdl = new LinkedList<ColumnDesc>();
		ColumnDesc cd1 = new ColumnDesc(10);
		ColumnDesc cd2 = new ColumnDesc(DataType.DATE, true);
		cd2.setAdditionalName(Function.EFFECTIVE_DATE);
		cdl.add(cd1);
		cdl.add(cd2);
		
		
		TableDesc table = new TableDesc("EmulatedTable", cdl, new ColumnDesc(30));
		table.setFileName("table.bin");
		
		
		result.add(table);
		
		return result;
	}
	public ServiceModel create() {
		List<Type>   types = findTypes();
		List<Function>    functions = findFunctions(types);
		List<TableDesc>   tables  = findTables();
		ServiceModel serviceModel=new ServiceModel(types,functions,tables);
		return serviceModel;

	}
    public File getProjectLocation() {
        return projectLocation;
    }
}
