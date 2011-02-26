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
public class MosaicTest {
	
	@Test

	public void testMosaicFunction(){
        ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/Mosaic"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);

        ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

		le.printoutServiceModel(System.out);
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
		
		answer = le.calculate(function,args).getArray();
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
		
		 answer = le.calculate(function,args).getArray();
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
		
		 answer = le.calculate(function,args).getArray();
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
		
		 answer = le.calculate(function,args).getArray();
		assertEquals("34.0", answer[0][0].getValue());
		assertEquals("64.0", answer[0][1].getValue());
		
	}
	
	@Test(expected=RuntimeException.class )
	public void testMosaicFunctionNotDivisible(){
        ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/Mosaic"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);

        ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

		le.printoutServiceModel(System.out);
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
			answer = le.calculate(function,args).getArray();
		} catch (RuntimeException e) {
			assertEquals("Argument with index  0 need to have divisible dimensions", e.getCause().getMessage());
			throw e;
		}
	}

	@Test(expected=RuntimeException.class )
	public void testMosaicFunctionNotSameFactor(){
        ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/Mosaic"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);

		ProjectLoader.registerElementFactory(ProjectElement.ElementType.TABLE, TableFactory.getInstance());

		le.printoutServiceModel(System.out);
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
			answer = le.calculate(function,args).getArray();
		} catch (RuntimeException e) {
			assertEquals("All arrays arguments need to have same dimension factor, but argument 2 has 2,while previous 1", e.getCause().getMessage());
			throw e;
			
		}
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
