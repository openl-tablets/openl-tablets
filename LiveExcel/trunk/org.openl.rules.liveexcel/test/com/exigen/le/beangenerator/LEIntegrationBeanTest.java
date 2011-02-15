package com.exigen.le.beangenerator;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarOutputStream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.WrapDynaBean;
import org.junit.Test;

import static org.junit.Assert.*;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;

public class LEIntegrationBeanTest {
	@Test
	public  void modelToBeanTest() throws Exception{
		
		String project = "DemoCase2";
		String function = "rateAutoLE".toUpperCase();
		
		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");

		le.init(prop);
		VersionDesc version =le.getDefaultVersionDesc(project);
		ServiceModel sm = le.getServiceModel(project, version);
		
		Type universal = emulateUniversal(sm);
		
		GeneratorClassLoader cl = new GeneratorClassLoader();
		
		// defines and loads bean tree
		
		Class cBean1 = BeanTreeGenerator.loadBeanClasses(universal.getName(), universal, cl,new PrintWriter(System.out),null);
//		Class cBean1 = BeanTreeGenerator.loadBeanClasses(universal.getName(), universal, cl);
		Object bean1 = cBean1.newInstance(); // test instantiation
		
		fillDemoCaseBean(bean1);
		
		BeanWrapper bw = new BeanWrapper(bean1,universal);
		List<Object> args = new ArrayList<Object>();
		args.add(bw.getValue("Coverage".toUpperCase()));
		args.add(bw.getValue("Vehicle".toUpperCase()));
		args.add(bw.getValue("Driver".toUpperCase()));
		args.add(bw.getValue("Policy".toUpperCase()));

		
		//call LE
		
		VersionDesc vd = new VersionDesc();
		vd.setVersion("");
		LE_Value result = null;
		result = le.calculate(project, vd, function, args);
		String s = SMHelper.valueToString(result);
		System.out.println("Result = "+s);
		
		assertEquals("343.07",s);
		System.out.println("Done");
	}
	
	
	
		@Test
		public  void modelToBeanTest2() throws Exception{
			
			String project = "Collections";
			
			LiveExcel le = LiveExcel.getInstance();
			le.setUnInitialized();
			
			Properties prop = new Properties();
			prop.put("repositoryManager.excelExtension",".xlsm");
			prop.put("repositoryManager.headPath","");
			prop.put("repositoryManager.branchPath","");
			le.init(prop);
			le.clean();
			ServiceModel sm  =le.getServiceModel(project, new VersionDesc("d"));
			SMHelper.printoutStructure(System.out, "", sm.getTypes());
			
			GeneratorClassLoader cl = new GeneratorClassLoader();
			
//			Type universal = emulateUniversal(sm);
			
			// defines and loads bean tree
			Type root = sm.getType("Departament");
			Class cBean1 = BeanTreeGenerator.loadBeanClasses(root.getName(),root, cl);
			root = sm.getType("Person");
			
			Class cBean2 = BeanTreeGenerator.loadBeanClasses(root.getName(),root, cl);
			Object bean1 = cBean1.newInstance(); // test instantiation
			
			Class cBean = bean1.getClass();
			
			Class cPerson = cBean.getDeclaredMethod("getPERSON", new Class[]{}).getReturnType().getComponentType();
			Method[] ms = cPerson.getDeclaredMethods(); 
			
			
			Class cCourseWork = cPerson.getDeclaredMethod("getSALARY", new Class[]{}).getReturnType().getComponentType();
			
			
			System.out.println();
			
		}	
	
	public static void fillDemoCaseBean(Object bean) throws Exception{
		// generated DemoCase context bean
		Class cBean = bean.getClass();
//		Class cVehicle = cBean.getDeclaredMethod("getVehicle", new Class[]{}).getReturnType();
//		Class cCoverage = cBean.getDeclaredMethod("getCoverage", new Class[]{}).getReturnType();
//		Class cDriver = cBean.getDeclaredMethod("getDriver", new Class[]{}).getReturnType();
//		Class cPolicy = cBean.getDeclaredMethod("getPolicy", new Class[]{}).getReturnType();
		Class cVehicle = cBean.getDeclaredMethod("getVEHICLE", new Class[]{}).getReturnType();
		Class cCoverage = cBean.getDeclaredMethod("getCOVERAGE", new Class[]{}).getReturnType();
		Class cDriver = cBean.getDeclaredMethod("getDRIVER", new Class[]{}).getReturnType();
		Class cPolicy = cBean.getDeclaredMethod("getPOLICY", new Class[]{}).getReturnType();
		
		Map<String, Object> vMap = new HashMap<String, Object>();
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
		
		Object oVehicle = cVehicle.newInstance();
		BeanUtils.copyProperties(oVehicle, vMap);
		
	    
		Map<String, Object> cMap = new HashMap<String, Object>();
	    cMap.put("coverageCode".toUpperCase(),"BASE" );
	    cMap.put("dedactibleAmount".toUpperCase(),200 );
	    
	    Object oCoverage = cCoverage.newInstance();
	    BeanUtils.copyProperties(oCoverage, cMap);
	    
	    
		Map<String, Object> dMap = new HashMap<String, Object>();
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
		
	    Object oDriver = cDriver.newInstance();
	    BeanUtils.copyProperties(oDriver, dMap);
		
	    
		Map<String, Object> pMap = new HashMap<String, Object>();
		pMap.put("effectiveDate".toUpperCase(), new GregorianCalendar(2009, 6, 20));
		pMap.put("expirationDate".toUpperCase(), new GregorianCalendar(2009, 6, 20));
		pMap.put("multiplePolicyDiscount".toUpperCase(),false );
		
	    Object oPolicy = cPolicy.newInstance();
	    BeanUtils.copyProperties(oPolicy, pMap);
		
	    // set beans to root bean
	    DynaBean db = new WrapDynaBean(bean);
	    db.set("vehicle".toUpperCase(), oVehicle);
	    db.set("coverage".toUpperCase(), oCoverage);
	    db.set("driver".toUpperCase(), oDriver);
	    db.set("policy".toUpperCase(), oPolicy);
	    
	}
	private Type emulateUniversal(ServiceModel sm){
		// Emulare "universal" context for Democase
		Type universal = new Type("Universal",true);
		List<MappedProperty> uniChilds = new ArrayList<MappedProperty>();
		for(Type type:sm.getTypes()){
			MappedProperty root = new MappedProperty(type.getName(),type,true);
			
			uniChilds.add(root);
		}
		universal.setChilds(uniChilds);
		universal.setPaths("");
		return universal;
	}
}
