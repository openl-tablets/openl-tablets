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
import com.exigen.le.smodel.emulator.SMEmulator;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelJAXB;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class AddInsCalcTest {
	
	@Test
	public void testIfErrorFunction(){
		String projectName = "AddIns";


		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsx");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		
		ProjectManager.getInstance().registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

	     VersionDesc version = new VersionDesc(""); 		
		     
//		prop.put("Collections.version","a");
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
		List<Object> args = new ArrayList<Object>();
		
        String function = "service_IfErrorYes".toUpperCase();
		LE_Value[][] answer = le.calculate(projectName,version,function,args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
					System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
					if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
						SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
					}
					else{  // 
						assertEquals("2.0", answer[j][jj].getValue());
					}
					}
				}
				
				function = "service_IfErrorNO".toUpperCase();
				answer = le.calculate(projectName,version,function,args).getArray();
						for(int j=0;j<answer.length;j++){
							for(int jj=0;jj<answer[j].length;jj++){
							System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
							if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
								SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
							}
							else{  // 
								assertEquals("1.0", answer[j][jj].getValue());
							}
							}
						}
		le.clean();

	}
	
	@Test
	public void testNormsDistFunction(){
		String projectName = "AddIns";


		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsx");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		
		ProjectManager.getInstance().registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

	     VersionDesc version = new VersionDesc(""); 		
		     
//		prop.put("Collections.version","a");
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
		List<Object> args = new ArrayList<Object>();
		args.add(new Double(0.5));
		
        String function = "service_NormsDist".toUpperCase();
		LE_Value[][] answer = le.calculate(projectName,version,function,args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
					System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
					if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
						SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
					}
					else{  // 
						assertEquals("0.69146246", answer[j][jj].getValue().substring(0,10));
					}
					}
				}
				
//				function = "service_IfErrorNO".toUpperCase();
//				answer = le.calculate(projectName,version,function,args).getArray();
//						for(int j=0;j<answer.length;j++){
//							for(int jj=0;jj<answer[j].length;jj++){
//							System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
//							if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
//								SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
//							}
//							else{  // 
//								assertEquals("1.0", answer[j][jj].getValue());
//							}
//							}
//						}
		le.clean();

	}

}
