package com.exigen.le.calculation;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.democase.Coverage;
import com.exigen.le.democase.Driver;
import com.exigen.le.democase.Policy;
import com.exigen.le.democase.Vehicle;
import com.exigen.le.servicedescr.evaluator.MapWrapper;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

public class DataLoggingTest {
	@Test
	public void dataLoggingTest() throws Exception {
		Driver driver = fillDriver();
		Coverage coverage = fillCoverage();
		Policy policy = fillPolicy();
		Vehicle vehicle = fillVehicle();

		// Restore regular SM provider(from xml), which can be overriden by other tests
		ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/DemoCase2"));
        LiveExcel le = new LiveExcel(provider);
	
		List args = new LinkedList();
		args.add(coverage);
		args.add(vehicle);
		args.add(driver);
		args.add(policy);
		
		
		// initiate logging
		le.setLogFile("mylog1");
		le.setDoLog(true);
		
// test bean log		
		LE_Value result = null;
		for (int i=0;i<10; i++){
			 result = le.calculate("rateAutoLE".toUpperCase(), args);
		}	
		// after that file would be in temp directory for this project/version
		// log should be checked visually!
		String s = SMHelper.valueToString(result);
		assertEquals("343.07",s);
		
		
		
// test holder log
		le.setLogFile("mylog2"); 
		ServiceModel sm = le.getServiceModel();
		List args2 = new ArrayList();
		args2.add(fillCoverageHolder(sm.getType("Coverage")));
		args2.add(fillVehicleHolder(sm.getType("Vehicle")));
		args2.add(fillDriverHolder(sm.getType("Driver")));
		args2.add(fillPolicyHolder(sm.getType("Policy")));
		
		result = null;
		for (int i=0;i<10; i++){
			 result = le.calculate("rateAutoLE".toUpperCase(), args2);
		}	
		// after that file would be in temp directory for this project/version
		// log should be checked visually!
		s = SMHelper.valueToString(result);
		assertEquals("343.07",s);	
	}
	
	// BeanWrapper has property name according JavaBean conversation - start with lower case letter
	
	// MapWrapper need to have property name in Upper case, that corresponding ServiceModel conversation
	Driver fillDriver() throws Exception{
		Driver bean = new Driver();
		Map<String, Object> dMap = new HashMap<String, Object>();
		dMap.put("driverTypeCode", "Primary");
		dMap.put("maxPoints",0 );
		dMap.put("farmBureauMemberIndicator", true);
		dMap.put("gender","Female" );
		dMap.put("goodEliteDriverCode", "Good Driver");
		dMap.put("goodStudentIndicator",false );
		dMap.put("maritalStatusCode","Single" );
		dMap.put("occupationCode","FIRE FIGHTER" );
		dMap.put("relationToInsuredCode","IN" );
		dMap.put("yearsOfCoverage",3);
		dMap.put("yearsLisensed",0);
		
	    BeanUtils.copyProperties(bean, dMap);
		return bean;
	}
	
	// MapWrapper need to have property name in Upper case, that corresponding ServiceModel conversation
	MapWrapper fillDriverHolder(Type driver) {
		MapWrapper dMap = new MapWrapper(driver);
		dMap.put("driverTypeCode".toUpperCase(), "Primary");
		dMap.put("maxPoints".toUpperCase(),0 );
		dMap.put("farmBureauMemberIndicator".toUpperCase(), true);
		dMap.put("gender".toUpperCase(),"Female" );
		dMap.put("goodEliteDriverCode".toUpperCase(), "Good Driver");
		dMap.put("goodStudentIndicator".toUpperCase(),false );
		dMap.put("maritalStatusCode".toUpperCase(),"Single" );
		dMap.put("occupationCode".toUpperCase(),"FIRE FIGHTER" );
		dMap.put("relationToInsuredCode".toUpperCase(),"IN" );
		dMap.put("yearsOfCoverage".toUpperCase(),3);
		dMap.put("yearsLisensed".toUpperCase(),0);
		return dMap;
	}
	
	
	
	Coverage fillCoverage() throws Exception{
		Coverage bean = new Coverage();
		Map<String, Object> cMap = new HashMap<String, Object>();
	    cMap.put("coverageCode","BASE" );
	    cMap.put("dedactibleAmount",200 );
	    BeanUtils.copyProperties(bean, cMap);
	    return bean;
	}
	
	MapWrapper fillCoverageHolder(Type coverage) {
		MapWrapper cMap = new MapWrapper(coverage);
	    cMap.put("coverageCode".toUpperCase(),"BASE" );
	    cMap.put("dedactibleAmount".toUpperCase(),200 );
	    return cMap;
	}
	
	Vehicle fillVehicle() throws Exception{
		Vehicle bean = new Vehicle();
		Map<String, Object> vMap = new HashMap<String, Object>();
		vMap.put("zipCode", "90004");
		vMap.put("antiTheftDeviceCode", "PASSIVE");
		vMap.put("estimatedAnnualDistance", 14999);
		vMap.put("modelYear", 2000);
		vMap.put("rateEffectiveDate", new GregorianCalendar(2002, 7, 1));
		vMap.put("vehicleUsageCode", "LONG COMMUTE");
		vMap.put("collisionSymbol","32" );
		vMap.put("comprehensiveSymbol", "31");
		vMap.put("multiCar", true);
		vMap.put("nonOwnedPolicy",false );
		
		BeanUtils.copyProperties(bean, vMap);
		return bean;
		
	}
	
	MapWrapper fillVehicleHolder(Type vehicle){
		MapWrapper vMap = new MapWrapper(vehicle);
		vMap.put("zipCode".toUpperCase(), "90004");
		vMap.put("antiTheftDeviceCode".toUpperCase(), "PASSIVE");
		vMap.put("estimatedAnnualDistance".toUpperCase(), 14999);
		vMap.put("modelYear".toUpperCase(), 2000);
		vMap.put("rateEffectiveDate".toUpperCase(), new GregorianCalendar(2002, 7, 1));
		vMap.put("vehicleUsageCode".toUpperCase(), "LONG COMMUTE");
		vMap.put("collisionSymbol".toUpperCase(),"32" );
		vMap.put("comprehensiveSymbol".toUpperCase(), "31");
		vMap.put("multiCar".toUpperCase(), true);
		vMap.put("nonOwnedPolicy".toUpperCase(),false );
		return vMap;
	}
	
	Policy fillPolicy() throws Exception {
		Policy bean = new Policy();
		
		Map<String, Object> pMap = new HashMap<String, Object>();
		pMap.put("effectiveDate", new GregorianCalendar(2009, 6, 20));
		pMap.put("expirationDate", new GregorianCalendar(2009, 6, 20));
		pMap.put("multiplePolicyDiscount",false );
		
	    BeanUtils.copyProperties(bean, pMap);
	    return bean;
	    
	}
	
	MapWrapper fillPolicyHolder(Type policy){
		MapWrapper pMap = new MapWrapper(policy);
		pMap.put("effectiveDate".toUpperCase(), new GregorianCalendar(2009, 6, 20));
		pMap.put("expirationDate".toUpperCase(), new GregorianCalendar(2009, 6, 20));
		pMap.put("multiplePolicyDiscount".toUpperCase(),false );
		return pMap;
	}
	
}
