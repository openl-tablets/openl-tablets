/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.openl.util.Log;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class ClasspathLoader {

	
	static final public String Openl_Properties_Fname = "openl.project.classpath.properties";
	static final public String Openl_Classpath_Property = "openl.project.classpath";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	
	static public String getOpenLPropertiesFolder(String projectHome)
	{
		return projectHome + "/build";
	}
	
	
	public String loadExistingClasspath(String projectHome) throws IOException
	{
		FileInputStream fis = null;
		try
		{
			Properties p = new Properties();
			fis = new FileInputStream(new File(getOpenLPropertiesFolder(projectHome), Openl_Properties_Fname));
			p.load(fis);
			
			return p.getProperty(Openl_Classpath_Property);
		}	
		catch(Exception ex)
		{
			return null;
		}
		finally
		{
			if (fis != null)
				fis.close();
		}
		
		
	}
	
	
	public void saveClasspath(String[] cp, String projectHome) throws IOException
	{
		String ecp = loadExistingClasspath(projectHome);
		if (ecp == null || !isTheSame(ecp, cp))
		{
			String folder = getOpenLPropertiesFolder(projectHome); 
			FileWriter fw = null;
			try
			{
				 fw = new FileWriter(new File(folder,Openl_Properties_Fname));
				 fw.write(Openl_Classpath_Property + "=");
				 for (int i = 0; i < cp.length; i++)
				{
					fw.write("\\\n" + cp[i] + File.pathSeparator);
				}
			}
			catch(Exception ex)
			{
				Log.error("Error writing " + folder + "/" + Openl_Properties_Fname, ex);
			}
			finally
			{
				if (fw != null)
					fw.close();
			}
				
		}
	}	
	
		private static boolean isTheSame(String ecp, String[] cp)
		{
			String[] ecps = StringTool.tokenize(ecp, File.pathSeparator);
			if (ecps.length != cp.length)
				return false;
			for (int i = 0; i < ecps.length; i++)
			{
				boolean found = false;
				for (int j = 0; j < cp.length; j++)
				{
					if (cp[j].equals(ecps[i]))
					{
						found = true;
						break;
					}	
				}
				if (!found)
					return false;
				
			}
			return true;
		}
	
}
