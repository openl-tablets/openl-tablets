package com.exigen.openl.importer;

import com.exigen.openl.model.openl.RuleSetFile;

public class Test {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OpenLImporter importer = new OpenLImporter();
		String cellRange = importer
				.getCellRegionFor(
						"C:\\my\\data\\eclipse-workspaces\\openl-on-studio\\runtime\\OpenLWorkspace\\com.exigen.bpm.example.solution.processes\\src\\main\\models\\com\\exigen\\bpm\\example\\resources\\HelloCustomer.xls",
						"HelloCustomer");
	}
}
