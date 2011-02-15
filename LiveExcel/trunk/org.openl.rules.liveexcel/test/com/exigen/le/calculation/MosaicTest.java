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
public class MosaicTest {
	
	@Test

	public void testMosaicFunction(){
		String projectName = "Mosaic";


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
		     
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
        String function = "mosaic".toUpperCase();
		
		List<Object> args = new ArrayList<Object>();
		LE_Value[][] answer;
		Double[][] x;
		Double y;
		Double[][] z;
		
		
//  Success - one iteration
		x = new Double[][]{  {new Double(1),new Double(2)},
										{new Double(1),new Double(2)},
										{new Double(1),new Double(2)},
						};
		y = new Double(2);
		
		z = new Double[][]{  {new Double(1),new Double(2),new Double(3)},
										{new Double(4),new Double(5),new Double(6)},
						};
		
		args.add(x);
		args.add(y);
		args.add(z);
		
		answer = le.calculate(projectName,version,function,args).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
					System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
					if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
						SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
					}
					else{  // 
						assertEquals("32.0", answer[j][jj].getValue());
					}
					}
				}
		//  Success - 2x1 iterations
				
		x = new Double[][]{  
						{new Double(1),new Double(2)},
						{new Double(1),new Double(2)},
						{new Double(1),new Double(2)},
						
						{new Double(2),new Double(4)},
						{new Double(2),new Double(4)},
						{new Double(2),new Double(4)},
				
		};
		y = new Double(4);
		
		z = new Double[][]{ 
				{new Double(1),new Double(2),new Double(3)},
				{new Double(4),new Double(5),new Double(6)},
				
				{new Double(2),new Double(4),new Double(6)},
				{new Double(8),new Double(10),new Double(12)},


		};
		args.clear();
		args.add(x);
		args.add(y);
		args.add(z);
		
		 answer = le.calculate(projectName,version,function,args).getArray();
		assertEquals("34.0", answer[0][0].getValue());
		assertEquals("64.0", answer[1][0].getValue());
		
		//  Success - 2x2 iterations
		
		x = new Double[][]{  
						{new Double(1),new Double(2), new Double(2),new Double(4)},
						{new Double(1),new Double(2), new Double(2),new Double(4)},
						{new Double(1),new Double(2), new Double(2),new Double(4)},
						
						{new Double(2),new Double(4), new Double(1),new Double(2)},
						{new Double(2),new Double(4), new Double(1),new Double(2)},
						{new Double(2),new Double(4), new Double(1),new Double(2)},
				
		};
		y = new Double(4);
		
		z = new Double[][]{ 
				{new Double(1),new Double(2),new Double(3),  new Double(2),new Double(4),new Double(6)},
				{new Double(4),new Double(5),new Double(6),  new Double(8),new Double(10),new Double(12)},
				
				{new Double(2),new Double(4),new Double(6),   new Double(1),new Double(2),new Double(3)},
				{new Double(8),new Double(10),new Double(12), new Double(4),new Double(5),new Double(6)},


		};
		args.clear();
		args.add(x);
		args.add(y);
		args.add(z);
		
		 answer = le.calculate(projectName,version,function,args).getArray();
		assertEquals("34.0", answer[0][0].getValue());
		assertEquals("64.0", answer[1][0].getValue());
		assertEquals("34.0", answer[1][1].getValue());
		assertEquals("64.0", answer[0][1].getValue());
		
		//  Success - 1x2 iterations
		
		x = new Double[][]{  
						{new Double(1),new Double(2), new Double(2),new Double(4)},
						{new Double(1),new Double(2), new Double(2),new Double(4)},
						{new Double(1),new Double(2), new Double(2),new Double(4)},
		};
		y = new Double(4);
		
		z = new Double[][]{ 
				{new Double(1),new Double(2),new Double(3),  new Double(2),new Double(4),new Double(6)},
				{new Double(4),new Double(5),new Double(6),  new Double(8),new Double(10),new Double(12)},
		};
		args.clear();
		args.add(x);
		args.add(y);
		args.add(z);
		
		 answer = le.calculate(projectName,version,function,args).getArray();
		assertEquals("34.0", answer[0][0].getValue());
		assertEquals("64.0", answer[0][1].getValue());
		
		le.clean();

	}
	
	@Test(expected=RuntimeException.class )
	public void testMosaicFunctionNotDivisible(){
		String projectName = "Mosaic";


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
		     
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
        String function = "mosaic".toUpperCase();
		
		List<Object> args = new ArrayList<Object>();
		LE_Value[][] answer;
		Double[][] x;
		Double y;
		Double[][] z;
		
		// Wrong - not divisible
		x = new Double[][]{  
				{new Double(1),new Double(2), new Double(2),new Double(4), new Double(-9) },
				{new Double(1),new Double(2), new Double(2),new Double(4), new Double(-9)},
				{new Double(1),new Double(2), new Double(2),new Double(4), new Double(-9)},
		};
		y = new Double(4);
		
		z = new Double[][]{ 
				{new Double(1),new Double(2),new Double(3),  new Double(2),new Double(4),new Double(6)},
				{new Double(4),new Double(5),new Double(6),  new Double(8),new Double(10),new Double(12)},
		};
		args.clear();
		args.add(x);
		args.add(y);
		args.add(z);
		
		 try {
			answer = le.calculate(projectName,version,function,args).getArray();
		} catch (RuntimeException e) {
			assertEquals("Argument with index  0 need to have divisible dimensions", e.getCause().getMessage());
			throw e;
		}
		
		le.clean();

	}

	@Test(expected=RuntimeException.class )
	public void testMosaicFunctionNotSameFactor(){
		String projectName = "Mosaic";


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
		     
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
        String function = "mosaic".toUpperCase();
		
		List<Object> args = new ArrayList<Object>();
		LE_Value[][] answer;
		Double[][] x;
		Double y;
		Double[][] z;
		
		// Wrong - not same factor
		x = new Double[][]{  
				{new Double(1),new Double(2),  new Double(2),new Double(4)},
				{new Double(1),new Double(2),  new Double(2),new Double(4)},
				{new Double(1),new Double(2),  new Double(2),new Double(4)},
				
		
			};
			y = new Double(4);
			
			z = new Double[][]{ 
					{new Double(1),new Double(2),new Double(3),  new Double(2),new Double(4),new Double(6)},
					{new Double(4),new Double(5),new Double(6),  new Double(8),new Double(10),new Double(12)},
					
					{new Double(2),new Double(4),new Double(6),   new Double(1),new Double(2),new Double(3)},
					{new Double(8),new Double(10),new Double(12), new Double(4),new Double(5),new Double(6)},
			
			};
			args.clear();
			args.add(x);
			args.add(y);
			args.add(z);

		 try {
			answer = le.calculate(projectName,version,function,args).getArray();
		} catch (RuntimeException e) {
			assertEquals("All arrays arguments need to have same dimension factor, but argument 2 has 2,while previous 1", e.getCause().getMessage());
			throw e;
			
		}
		le.clean();

	}
}
