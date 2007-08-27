/**
 * Created Oct 26, 2006
 */
package org.openl.main;

import java.util.Properties;

import org.openl.util.Log;



/**
 * @author snshor
 *
 */
public class OpenLVersion
{
	static final public String prop_file_name ="openl.version.properties";
	static final public String prop_version_name ="openl.version";
	static final public String prop_build_name ="openl.build";

	public static String getVersion()
	{
		return getProperties().getProperty(prop_version_name, "???");
	}
	
	public static String getBuild()
	{
		return getProperties().getProperty(prop_build_name, "??");
		
	}
	
	static synchronized Properties getProperties()
	{
		if (props == null)
		{
			props = new Properties();
			
			try
			{
				props.load(OpenLVersion.class.getResourceAsStream(prop_file_name));
			} catch (Throwable t)
			{
				Log.warn(prop_file_name + " not found", t);
			}
		}
		
		return props;
	}

	static Properties props = null;
	
}
