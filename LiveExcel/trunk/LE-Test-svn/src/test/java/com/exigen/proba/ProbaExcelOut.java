package com.exigen.proba;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.exigen.poi.array.test.ArrayFormulaTest;


public class ProbaExcelOut {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
//	    	 POIFSFileSystem fs = new POIFSFileSystem(
//	                                    new FileInputStream(
//	                                    args[0]));

	    	 // create a workbook out of the input stream
//	         Workbook wb =  new XSSFWorkbook(args[0]);;
//	         Workbook wb =  new HSSFWorkbook(fs);;
			
			// create empty workbook
	         Workbook wb =  new HSSFWorkbook();
	         
	         FileOutputStream out = new FileOutputStream(args[1]);
	         

	         

	        	
		         // get a reference to the worksheet
//		         Sheet sheet = wb.getSheetAt(0);
		         Sheet sheet = wb.createSheet();
		         
		         Row rowd = sheet.createRow((short) (0));
	        	 Cell cd = rowd.createCell((short) 0);
	        	 
	        	 cd.setCellValue("ol_declare_table(\"BigLookup\", \"Test LiveExcel lookup\", A4:CW1004)");
	 		    	 
		         
		         Row row0 = sheet.createRow((short) (3));
		         
//		         for(int i=0;i<100;i++)
		         for(int i=0;i<10000;i++)   // By row
		         {
		        	 // create rows from 5 fill values in the 3rd column
		        	 Row row1 = sheet.createRow((short) (+i+4));
		        	 Cell c0 = row1.createCell((short) 0);
		        	 c0.setCellValue(i);
		        	 
	                  for(int j=1;j<51;j++){	      
//	                  for(int j=1;j<100;j++){	     // By column 
	                	if(i==0){
	                		c0 = row0.createCell((short) j);
	                		c0.setCellValue(j);
	                	}
	                		
		        	 // create a cell on the 3rd column
	                	  Cell c1_j = row1.createCell((short) j);
		        
	                	  c1_j.setCellValue("r"+i+"c"+j);
	                	  System.out.println("Created for "+i+","+j);
	
	                  }
		         }

		         wb.write(out);
		         out.close();
	        
	         System.out.println("Done");
	         System.out.println("Do not forget correct manualy output file:");
	         System.out.println("  - range in ol_declare_table");
	         System.out.println("  - add \"=\" before ol_declare_table");
	     }
	      catch (Throwable t){
	    	  t.printStackTrace();
	      }

	}

}
