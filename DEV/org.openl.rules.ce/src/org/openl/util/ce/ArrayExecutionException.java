package org.openl.util.ce;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.SortedMap;

public class ArrayExecutionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1937529333326931200L;

	
	SortedMap<Integer, Throwable> errorMap;
	
	public SortedMap<Integer, Throwable> getErrorMap() {
		return errorMap;
	}

	public ArrayExecutionException(String message, SortedMap<Integer, Throwable> errorMap)
	{
		super(message);
		this.errorMap = errorMap;
	}

	@Override
	public String getMessage() {
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.append(super.getMessage()).append('\n');
		
		for (Map.Entry<Integer, Throwable> entry : errorMap.entrySet()) {
			pw.append(entry.getKey().toString()).append(": == ").append(entry.getValue().getMessage()).append('\n');
			entry.getValue().printStackTrace(pw);
		}
		
		pw.close();
		String res = sw.toString();
		
		try {
			sw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
	
	
	
	
	
}
