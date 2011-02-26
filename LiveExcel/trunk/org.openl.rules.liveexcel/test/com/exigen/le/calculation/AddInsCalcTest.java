/**
 * 
 */
package com.exigen.le.calculation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.evaluator.selector.FunctionByDateSelector;
import com.exigen.le.evaluator.table.TableFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class AddInsCalcTest {
	
	@Test
	public void testIfErrorFunction(){
		ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());
        ServiceModelJAXB serviceModelProvider = new ServiceModelJAXB(new File("./test-resources/LERepository/AddIns/"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), serviceModelProvider);

//		prop.put("Collections.version","a");
		le.printoutServiceModel(System.out);
		
		List<Object> args = new ArrayList<Object>();
		
        String function = "service_IfErrorYes".toUpperCase();
		LE_Value[][] answer = le.calculate(function,args).getArray();
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
				answer = le.calculate(function,args).getArray();
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
	}
	
	@Test
	public void testNormsDistFunction(){
		ServiceModelJAXB serviceModelProvider = new ServiceModelJAXB(new File("./test-resources/LERepository/AddIns/"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), serviceModelProvider);
		     
//		prop.put("Collections.version","a");
		le.printoutServiceModel(System.out);
		
		List<Object> args = new ArrayList<Object>();
		args.add(new Double(0.5));
		
        String function = "service_NormsDist".toUpperCase();
		LE_Value[][] answer = le.calculate(function,args).getArray();
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
