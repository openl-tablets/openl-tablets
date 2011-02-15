package com.exigen.le.PAYD;

import static junit.framework.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.democase.BuildDemoCaseObjects;
import com.exigen.le.democase.Coverage;
import com.exigen.le.democase.Driver;
import com.exigen.le.democase.Policy;
import com.exigen.le.democase.Vehicle;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.smodel.provider.ServiceModelJAXB;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;

public class PAYDCalcTest {
	@Test
	public void testPAYDTestData(){
		
		String[][][][] etalons = {
		   new String[][][]{	
			 new String[][] {  
				 new String[]	{
						 ""
					 },
				},
		   },	
		};


		String projectName = "PAYD";
		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsx");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		// Restore regular SM provider(from xml), which can be overriden by other tests
		ServiceModelProviderFactory.getInstance().setProvider(new ServiceModelJAXB());
	
		le.setUnInitialized();
		le.init(prop);
		le.clean();
//		VersionDesc versionDesc = new VersionDesc("0");
		VersionDesc versionDesc = new VersionDesc("4");
		ServiceModel sm =le.getServiceModelMakeDefault(projectName,versionDesc );
		le.printoutServiceModel(System.out, projectName,versionDesc);
		
		List<Function> funcs = sm.getFunctions();
		
		Function getTestVehicles = new Function("PAYD Rater GJD","Data Sample", "getTestVehicles", "G1020:Y1527" );
//		Function getTestVehicles = new Function("PAYD Rater GJD","Data Sample", "getTestVehicles", "G1515:Y1527" );
		getTestVehicles.setReturnType(sm.getType("Vehicle"));
		getTestVehicles.setReturnCollection(true);
		funcs.add(getTestVehicles);
		
		Function getTestDrivers = new Function("PAYD Rater GJD","Data Sample", "getTestDrivers", "G516:T1015" );
		getTestDrivers.setReturnType(sm.getType("Driver"));
		getTestDrivers.setReturnCollection(true);
		funcs.add(getTestDrivers);
		
		Function getTestPolicies = new Function("PAYD Rater GJD","Data Sample", "getTestPolicies", "G11:L510" );
		getTestPolicies.setReturnType(sm.getType("Policy"));
		getTestPolicies.setReturnCollection(true);
		funcs.add(getTestPolicies);
		
		Function getTestCoverages = new Function("PAYD Rater GJD","Data Sample", "getTestCoverages", "G7:G8" );
		getTestCoverages.setReturnType(sm.getType("Coverage"));
		getTestCoverages.setReturnCollection(true);
		funcs.add(getTestCoverages);
		List<Object> args = new ArrayList<Object>();
		ValueHolder  vehicles = le.calculate(projectName, versionDesc, "getTestVehicles".toUpperCase(),args).getValueHolder();
		SMHelper.printoutValues(System.out, "", vehicles);
//		System.exit(0);
		
		ValueHolder  policies = le.calculate(projectName, versionDesc, "getTestPolicies".toUpperCase(),args).getValueHolder();
//		SMHelper.printoutValues(System.out, "", policies);
		ValueHolder  drivers = le.calculate(projectName, versionDesc, "getTestDrivers".toUpperCase(),args).getValueHolder();
//		SMHelper.printoutValues(System.out, "", drivers);
		ValueHolder coverages = le.calculate(projectName, versionDesc, "getTestCoverages".toUpperCase(),args).getValueHolder();
//		SMHelper.printoutValues(System.out, "", coverages);
		

		List<Function> allfuncs = le.getServiceFunctions(projectName, versionDesc);
		String function = allfuncs.get(0).getName();
		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("4",date); 		
		System.out.println("*******Calculate function(service) "+function);
		SMHelper.printoutValues(System.out, "DRIVER,4", drivers.getValue("DRIVER",4));
		SMHelper.printoutValues(System.out, "VEHICLE,494", vehicles.getValue("VEHICLE",494));
		SMHelper.printoutValues(System.out, "POLICY,0", policies.getValue("POLICY",0));
		SMHelper.printoutValues(System.out, "COVERAGE,0", coverages.getValue("COVERAGE",0));
		args.add(drivers.getValue("DRIVER",4));
		args.add(vehicles.getValue("VEHICLE",494));
		args.add(policies.getValue("POLICY",0));
		args.add(coverages.getValue("COVERAGE",0));
		args.add("Gross Total Premium");
		
		LE_Value result = le.calculate(projectName, versionDesc, function, args);
		
		System.out.println("Result="+SMHelper.valueToString(result));
		le.clean();

	}

}
