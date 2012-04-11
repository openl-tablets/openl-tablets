package org.openl.lib.apache.tomcat;


import java.io.File;
import java.lang.reflect.Method;

/*
 * Created on Jul 11, 2004
 *
 * Developed by OpenRules Inc 2003-2004 	
 */

/**
 * @author snshor
 */
public class StartTomcat
{

	public static void main(String[] args) throws Exception
	{
		
//		System.out.println("OpenL Tomcat Starter,  Version " + OpenLVersion.getVersion() + 
//				" Build " + OpenLVersion.getBuild() + " http://openl-tablets.sourceforge.net  (c) 2006,2007\n");
		
//		System.out.println("OpenL Tomcat Starter,  Version " + OpenLVersion.getVersion() + 
//				" Build " + OpenLVersion.getBuild() + " " + 
//				OpenLVersion.getURL()  + " (c) " + OpenLVersion.getCopyrightYear() +"\n");
		
		
		String chome = System.getProperty("catalina.home");

		if (chome == null)
		{
			chome = getProperty(args, "catalina.home");
		}
		if (chome == null)
		{
			chome ="../org.openl.lib.apache.tomcat/apache-tomcat-5.5.17";
		}
		File catalinaHome = new File(chome);
		
		if (!catalinaHome.exists())
		{
			throw new Exception("\nYou did not set up correctly catalina.home variable.\n Please refer to OpenL Tablets document 'Web Programming and OpenL Tablets'. Chapter - Web Develoment Setup");
		}	
		
		System.setProperty("catalina.home", catalinaHome.getCanonicalPath());
		
		System.out.println(
			"Using tomcat home: " + System.getProperty("catalina.home"));

		String cbase = System.getProperty("catalina.base");
		

		if (cbase == null)
		{
			cbase = getProperty(args, "catalina.base");
		}
		if (cbase == null)
		{
			cbase=".";
		}
		
		File catalinaBase = new File(cbase);
		System.setProperty("catalina.base", catalinaBase.getCanonicalPath());
		
		System.out.println(
				"Using tomcat base: " + System.getProperty("catalina.base"));
		
		Class bootstrap = null;
		try
		{
			bootstrap = Class.forName("org.apache.catalina.startup.Bootstrap");
		}
		catch(ClassNotFoundException cnfe)
		{
			throw new Exception("\n Apache Tomcat bootstrap.jar must be in classpath.");
		}
		
		Method main = bootstrap.getMethod("main", new Class[]{String[].class});
		
		
		main.invoke(null, new Object[]{new String[] { "start" }});
		
		
//		org.apache.catalina.startup.Bootstrap.main(new String[] { "start" });
		
	}
	/*
		C:\3p\jakarta-tomcat-5.0.25\bin>start "Tomcat" "c:\j2sdk1.4.2_04\bin\java"    
		-Djava.endorsed.dirs="C:\3p\jakarta-tomcat-5.0.25\common\endorsed" 
		-classpath "c:\j2sdk1.4.2_04\lib\tools.jar;C:\3p\jakarta-tomcat-5.0.25\bin\bootstrap.jar" 
		-Dcatalina.base="C:\3p\jakarta-tomcat-5.0.25" 
		-Dcatalina.home="C:\3p\jakarta-tomcat-5.0.25" 
		-Djava.io.tmpdir="C:\3p\jakarta-tomcat-5.0.25\temp" 
		org.apache.catalina.startup.Bootstrap  start
	
	*/

	private static String getProperty(String[] args, String prefix) {
		if (args != null){
			for (String parameter : args){
				if (parameter.startsWith(prefix)){
					return parameter.substring(parameter.indexOf('=')+1).trim();					
				}
			}
		}
		return null;
	}

}

