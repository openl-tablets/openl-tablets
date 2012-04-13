package com.exigen.le.proba;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Proba {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Workbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet("Proba");
		Row row = sheet.createRow(0);
		for(int i= 0;i<124;i++){
			Cell c = row.createCell(i);
			c.setCellValue("I="+i);
			
		}
		try {
			FileOutputStream fileOut = new FileOutputStream("workbook.xlsx");
			wb.write(fileOut);
			fileOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
