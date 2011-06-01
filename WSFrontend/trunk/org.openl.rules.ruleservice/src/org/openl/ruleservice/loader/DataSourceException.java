package org.openl.ruleservice.loader;
/**
 * Main data source exception for wrapping
 * 
 * @author MKamalov
 *
 */

public class DataSourceException extends Exception{

	private static final long serialVersionUID = 6818824565990021295L;

	public DataSourceException() {
		super();
	}

	public DataSourceException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataSourceException(String message) {
		super(message);
	}

	public DataSourceException(Throwable cause) {
		super(cause);
	}
}
