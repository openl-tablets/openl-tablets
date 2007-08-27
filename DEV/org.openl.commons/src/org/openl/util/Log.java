/*
 * Created on May 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import org.apache.commons.logging.LogFactory;

/**
 * @author snshor
 *
 */
public class Log
{

	static org.apache.commons.logging.Log logger = LogFactory.getLog(Log.class);
	

	public static boolean isTraceEnabled()
	{
		return logger.isTraceEnabled();
	}	

	public static boolean isDebugEnabled()
	{
		return logger.isDebugEnabled();
	}	

	public static boolean isErrorEnabled()
	{
		return logger.isErrorEnabled();
	}	

	public static boolean isInfoEnabled()
	{
		return logger.isInfoEnabled();
	}	

	public static boolean isWarnEnabled()
	{
		return logger.isWarnEnabled();
	}	


	
	public static void debug(Object message)
	{
		logger.debug(message);
	}
	
	
	public static void debug(Object message, Throwable t)
	{
		logger.debug(message, t);
	}

	public static void info(Object message)
	{
		logger.info(message);
	}

	public static void info(Object message, Throwable t)
	{
		logger.info(message, t);
	}
	
	public static void trace(Object message)
	{
		logger.trace(message);
	}

	public static void trace(Object message, Throwable t)
	{
		logger.trace(message, t);
	}


	public static void warn(Object message)
	{
		logger.warn(message);
	}

	public static void warn(Object message, Throwable t)
	{
		logger.warn(message, t);
	}

	public static void error(Object message)
	{
		logger.error(message);
	}

	public static void error(Object message, Throwable t)
	{
		logger.error(message, t);
	}





}
