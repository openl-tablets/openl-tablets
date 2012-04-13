/**
 * 
 */
package com.exigen.le.smodel.emulator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.exigen.le.LE_Value;
import com.exigen.le.smodel.Cell;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.Function.FunctionArgument;
import com.exigen.le.smodel.provider.ServiceModelProvider;
import com.exigen.le.smodel.table.DBInitTest;

/**
 * @author vabramovs
 *
 */
public class SM_Tables_Emulator implements ServiceModelProvider {
	public List<Type> findTypes() {
		List<Type> result = new ArrayList<Type>();
		Type numeric = new Type();
		numeric.setComplex(false);
		numeric.setName(LE_Value.TypeString.NUMERIC);
		
		Type str = new Type();
		str.setComplex(false);
		str.setName(LE_Value.TypeString.STRING);
		result.add(numeric);
		result.add(str);
		
		return result;
	}
	public List<Function> findFunctions(List<Type> types) {
		List<Function> result = new ArrayList<Function>();
		
		List<FunctionArgument> arguments = new ArrayList<FunctionArgument>();
		
	
		FunctionArgument arg1 = new FunctionArgument();
		for(Type type:types){
			if(type.getName().equals(LE_Value.TypeString.NUMERIC)){
				arg1.setType(type);
			}
		}
		arg1.setDescription("Argument 1");
		arg1.setInput(new Cell().init("B3"));
		
		FunctionArgument arg2 = new FunctionArgument();
		for(Type type:types){
			if(type.getName().equals(LE_Value.TypeString.STRING)){
				arg2.setType(type);
			}
		}
		arg2.setDescription("Argument 2");
		arg2.setInput(new Cell().init("B4"));
		arguments.add(arg1);
		arguments.add(arg2);
		
		Function service_calcSimplestTable = new Function();
		service_calcSimplestTable.setName("service_calcSimplestTable");
//		service_calcSimplestTable.setExcel("Tables");
		service_calcSimplestTable.setExcel("Header");
		service_calcSimplestTable.setSheet("Simplest");
		service_calcSimplestTable.setFunctionDescription("Lookup simplest table");
		service_calcSimplestTable.setReturnType(null);
		service_calcSimplestTable.setReturnSpace(new Cell().init("B5"));
		service_calcSimplestTable.setArguments(arguments);
		service_calcSimplestTable.setService(true);
		result.add(service_calcSimplestTable);
		
		return result;
	}
	public List<TableDesc> findTables() {
		return DBInitTest.findTables();
	}
	public ServiceModel create() {
		List<Type>   types = findTypes();
		List<Function>    functions = findFunctions(types);
		List<TableDesc>   tables  = findTables();
		ServiceModel serviceModel=new ServiceModel(types,functions,tables);
		return serviceModel;

	}
    public File getProjectLocation() {
        // TODO Auto-generated method stub
        return null;
    }
}
