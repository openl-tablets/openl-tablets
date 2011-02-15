/**
 * 
 */
package com.exigen.le.proba;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import com.exigen.le.project.ProjectManager;


/**
 * @author vabramovs
 *
 */
public class ProfilerSample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {


		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		int iterationCount = 20;

//		CollectionsEvaluator ceval = new CollectionsEvaluator(prop);
		TableEvaluator teval = new TableEvaluator(prop);
		
		System.out.println("Start profiler and press any key..");
		try {
			System.in.read();
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        Date begin = new Date();

        int delta = iterationCount/4;
        
		for(int i=0;i<iterationCount;i++){
			System.out.println("Iteration "+i);
//			ceval.iterate();
			teval.iterate();
			
			if(i%delta == 0){
		        // Force gc
		        System.gc();
		        System.gc();
		        
				System.out.println("Iteration "+i+"Get shapshot and press any key..");
				try {
					System.in.read();
					System.in.read();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		
		Date end = new Date();
		
		
		ProjectManager pr = ProjectManager.getInstance();
        // Dispose resources 
//        ceval.dispose();
        teval.dispose();
        // Force gc
        System.gc();
        System.gc();
        
		System.out.println("Stop profiler and press any key..");
		try {
			System.in.read();
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       System.out.println("Start at "+begin);
       System.out.println("End  at  "+end);
       long duration = end.getTime()-begin.getTime();
       System.out.println("Duration in msec - " +duration);
       System.out.println("Average iteration in msec - " +duration/iterationCount);
	}

}
