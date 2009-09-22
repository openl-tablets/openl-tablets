package org.openl.codegen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Generates file in outFileLocation by inserting code into predefined places in
 * input file inFileLocation. Insertion places are defined by INSERT_TAG. The
 * insertion logic is handled by ICodeGenAdaptor, there could be multiple
 * INSERT_TAGS in the code, calling class can redefine INSERT_TAG value
 * 
 * @author snshor Created Jul 27, 2009
 * 
 */

public class FileCodeGen {
	
	public static final String DEFAULT_INSERT_TAG = "<<< INSERT";
	
	private String inFileLocation;
	private String outFileLocation;
	private String insertTag;
	
	public FileCodeGen(String inFileLocation, String outFileLocation, String insertTag) {
		
		this.inFileLocation = inFileLocation;
		this.outFileLocation = outFileLocation == null ? inFileLocation : outFileLocation;
		this.insertTag = insertTag == null ? DEFAULT_INSERT_TAG : insertTag;
	}
	
	public void processFile(ICodeGenAdaptor cga) throws IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(inFileLocation));
		StringBuilder sb = new StringBuilder(10000);
		
		String line = null;
		
		while ((line = br.readLine()) != null) {
			sb.append(line).append('\n');
			
			if (line.contains(insertTag)) {
				cga.processInsertTag(line, sb);
			}
		}
		
		br.close();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(outFileLocation));
		bw.write(sb.toString());
		bw.close();
	}
}
