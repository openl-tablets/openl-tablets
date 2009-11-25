/**
 * 
 */
package com.exigen.proba;

import java.util.List;

import com.exigen.liveexcel.test.TaskPrescription;
import com.exigen.liveexcel.test.TasksPortion;

/**
 * @author vabramovs
 *
 */
public class ProbaTasks {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		List<TasksPortion> tasks = TasksPortion.getTasks4Book(args[0]);
		for(TasksPortion taskpor: tasks){
			System.out.println("New task portion -"+taskpor.getPortionName());
			List<TaskPrescription> taskset = taskpor.getTaskList();
			for(TaskPrescription task: taskset){
				System.out.println("\t New task:");
				System.out.println("\t\t Function:"+task.getFunctionName());
				Object[] par = task.getArgs();
				for (int i=0;i<par.length;i++){
					System.out.println("\t\t Argument "+i+"="+par[i]);
				}
				System.out.println("\t\t Result:"+task.getResult());
				System.out.println("\t\t iteration:"+task.getIterationCount());
				
			}
			
		}

	}

}
