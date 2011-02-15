/**
 * 
 */
package com.exigen.le.smodel;


public interface ExcelSpace {
	public boolean isArea();

	public Cell from();

	public Cell to();
	
	public int getHeight();
	
	public int getWidth();
	public static class Factory{
		public static ExcelSpace create(String excelArea){
			if(excelArea.contains(":")){
				return new Range().init(excelArea);
			}
			else {
				return new Cell().init(excelArea);
			}
				
		}
	}
}

