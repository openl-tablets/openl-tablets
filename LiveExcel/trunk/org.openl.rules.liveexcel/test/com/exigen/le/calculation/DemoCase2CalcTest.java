/**
 * 
 */
package com.exigen.le.calculation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.collections.Collections;
import com.exigen.le.collections.Departament;
import com.exigen.le.collections.departament.Person;
import com.exigen.le.democase.BuildDemoCaseObjects;
import com.exigen.le.democase.Coverage;
import com.exigen.le.democase.Driver;
import com.exigen.le.democase.Policy;
import com.exigen.le.democase.Vehicle;
import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.evaluator.table.TableFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.smodel.emulator.JavaUDF;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelJAXB;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class DemoCase2CalcTest {
	
	@Test
	public void testDemoCase2(){
		String projectName = "DemoCase2";
		
		String[][][][] etalons = {
		   new String[][][]{	
			 new String[][] {  
				 new String[]	{
						 "282.19",
						 "343.07",
					 },
				 new String[]	{
						 "212.31",
						 "257.83",
					},
				new String[]	{
						"196.14",
						"238.11",
				},
			 },	
			 new String[][] {  
				new String[]	{
						"254.19",
						"308.91",
				},
				new String[]	{
						"191.35",
						"232.27",
				},
				new String[]	{
						"226.66",
						"275.32",
				},
			 },
		   },
	   new String[][][]{
			 new String[][] {  
						new String[]	{
								"264.1",
								"320.98",
						},
						new String[]	{
								"198.76",
								"241.3",
						},
						 new String[]	{
								"183.65",
								"222.86"},
						},
			 new String[][] {  
				 new String[]	{
						 "237.9",
						 "289.02",
				},
				 new String[]	{
						 "179.16",
						 "217.39",
				},
				 new String[]	{
						 "212.17",
						 "257.65",
				},
			 },	
		   },	 
	   new String[][][]{
			 new String[][] {  
				 new String[]	{
						 "794.94",
						 "966.22",
				},
				 new String[]	{
						 "514.52",
						 "624.22",
				},
				 new String[]	{
						 "482.38",
						 "585.04",
				},
			 },	
			 new String[][] {  
				 new String[]	{
						 "766.69",
						 "931.76",
				},
				 new String[]	{
						 "496.42",
						 "602.15",
				},
				 new String[]	{
						 "506.02",
				 		"613.86"},
				},
		   },	
		};


		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		// Restore regular SM provider(from xml), which can be overriden by other tests
		ServiceModelProviderFactory.getInstance().setProvider(new ServiceModelJAXB());
	
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		ServiceModel sm =le.getServiceModelMakeDefault(projectName,new VersionDesc("") );
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
		List<Vehicle> vehicles = BuildDemoCaseObjects.createVehicles();
		List<Policy> policies = BuildDemoCaseObjects.createPolicies();
		List<Driver> drivers = BuildDemoCaseObjects.createDrivers();
		List<Coverage> coverages = BuildDemoCaseObjects.createCoverages();
		
		String function = "rateAutoLE".toUpperCase();

		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("",date); 		
		System.out.println("*******Calculate function(service) "+function);
//		for(Coverage coverage:coverages){
		for(int i=0;i<3;i++){
			Coverage coverage = coverages.get(i);
			BeanWrapper bw1 = new BeanWrapper(coverage, sm.getType("Coverage"));
//			for(Vehicle vehicle:vehicles){
			for(int ii = 0;ii<2;ii++){
				Vehicle vehicle=vehicles.get(ii);
				BeanWrapper bw2 = new BeanWrapper(vehicle, sm.getType("Vehicle"));
//				for(Driver driver:drivers){
				for(int iii = 0;iii<3;iii++){
					Driver driver=drivers.get(iii);
					BeanWrapper bw3 = new BeanWrapper(driver, sm.getType("Driver"));
//					for(Policy policy:policies){
					for(int iiii=0;iiii<2;iiii++){
						Policy policy=policies.get(iiii);
						BeanWrapper bw4 = new BeanWrapper(policy, sm.getType("Policy"));
						List<Object> args = new ArrayList<Object>();
						args.add(bw1);
						args.add(bw2);
						args.add(bw3);
						args.add(bw4);
						LE_Value[][] answer=le.calculate(projectName,version,function,args).getArray();
						for(int j=0;j<answer.length;j++){
							for(int jj=0;jj<answer[j].length;jj++){
								System.out.printf("[v=%d][c=%d][d=%d][p=%d]",
										vehicles.indexOf(vehicle)+1,coverages.indexOf(coverage)+1,
										drivers.indexOf(driver)+1,policies.indexOf(policy)+1);
								System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
								if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
									SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
								}
								else{  // 
									assertEquals(etalons[i][ii][iii][iiii], answer[j][jj].getValue());
								}
							}
						}
					}
				}
			}
		}
		le.clean();

	}

}
