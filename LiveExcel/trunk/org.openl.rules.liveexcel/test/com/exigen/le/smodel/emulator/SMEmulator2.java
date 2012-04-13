/**
 * 
 */
package com.exigen.le.smodel.emulator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
public class SMEmulator2 implements ServiceModelProvider {
    
    private final File projectLocation;
    
    public SMEmulator2(File projectLocation) {
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
			if(type.getName().equals("Departament")){
				arg.setType(type);
			}
		}
		arg.setDescription("Common context");
		arg.setInput(new Cell().init("B2"));
		List<FunctionArgument> arguments = new ArrayList<FunctionArgument>();
//		arguments.add(arg);
		
		
		Function service_chiefSalary = new Function();
		service_chiefSalary.setName("service_JavaUDF");
		service_chiefSalary.setExcel("Collections");
		service_chiefSalary.setSheet("JavaUDF");
		service_chiefSalary.setFunctionDescription("Invoke Java method");
		service_chiefSalary.setReturnType(null);
//		service_chiefSalary.setReturnSpace(new Function.Cell().init("A1"));
		service_chiefSalary.setReturnSpace(new Range().init("A1:A4"));
		service_chiefSalary.setArguments(arguments);
		service_chiefSalary.setService(true);
		result.add(service_chiefSalary);
		
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
		
		
		TableDesc table = new TableDesc("EmulatedTable", cdl, new ColumnDesc(20));
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
