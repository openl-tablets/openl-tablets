package com.exigen.le.PAYD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.evaluator.selector.FunctionByDateSelector;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

import static junit.framework.Assert.*;

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


		ProjectLoader.reset();
		// Restore regular SM provider(from xml), which can be overriden by other tests
        LiveExcel le = new LiveExcel(new FunctionByDateSelector() ,new ServiceModelJAXB(new File("./test-resources/LERepository/PAYD/4")));
	
//		VersionDesc versionDesc = new VersionDesc("0");
		ServiceModel sm =le.getServiceModel();
		le.printoutServiceModel(System.out);
		
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
		ValueHolder  vehicles = le.calculate("getTestVehicles".toUpperCase(),args).getValueHolder();
		SMHelper.printoutValues(System.out, "", vehicles);
//		System.exit(0);
		
		ValueHolder  policies = le.calculate("getTestPolicies".toUpperCase(),args).getValueHolder();
//		SMHelper.printoutValues(System.out, "", policies);
		ValueHolder  drivers = le.calculate("getTestDrivers".toUpperCase(),args).getValueHolder();
//		SMHelper.printoutValues(System.out, "", drivers);
		ValueHolder coverages = le.calculate("getTestCoverages".toUpperCase(),args).getValueHolder();
//		SMHelper.printoutValues(System.out, "", coverages);
		

		List<Function> allfuncs = le.getServiceFunctions(null);
		String function = allfuncs.get(0).getName();
		
		Map<String, String> envProps = new HashMap<String, String>();
		envProps.put(Function.EFFECTIVE_DATE, "2010/05/21-08:00");
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
		
		LE_Value result = le.calculate(function, args, envProps);
		
		System.out.println("Result="+SMHelper.valueToString(result));

	}
    
    // We should clear all created temp files manually because JUnit terminates
    // JVM incorrectly and finalization methods are not executed
    @After
    public void finalize() {
        try {
            ProjectLoader.reset();
            FileUtils.deleteDirectory(ProjectLoader.getTempDir());
        } catch (IOException e) {
            e.printStackTrace();
            assertFalse(true);
        }
    }
}
