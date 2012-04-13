/**
 * 
 */
package com.exigen.liveexcel.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;



/**
 * @author vabramovs
 *
 */
public class TasksPortion {
	String portionName;
	List<TaskPrescription> taskList;
	
	public TasksPortion(String portionName,
			List<TaskPrescription> taskList) {
	this.portionName = portionName;
	this.taskList = taskList;
	}
	/**
	 * @return the portionName
	 */
	public String getPortionName() {	
		return portionName;
	}
	/**
	 * @return the taskList
	 */
	public List<TaskPrescription> getTaskList() {
		return taskList;
	}
	static public List<TasksPortion> getTasks4Book(String excelPath ){
		List<TasksPortion> answer = new ArrayList<TasksPortion>(); 
		
	   	 try {
	   		FileInputStream in = new FileInputStream(excelPath);
			POIFSFileSystem fs = new POIFSFileSystem(in);
			  Workbook wb =  new HSSFWorkbook(fs);
			  for(int i=0;i<wb.getNumberOfSheets();i++){
				  Sheet sheet = wb.getSheetAt(i);
				  List<TaskPrescription> tsks = TaskPrescription.getTasks4Sheet(sheet);
				  if(tsks.size()>0)
					  answer.add(new TasksPortion(sheet.getSheetName(),tsks));
			  }
		  
		   in.close();  
		   return answer;	  
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return answer;
	}

}
