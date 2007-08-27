/*
 * Created on Jul 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.OpenL;
import org.openl.util.ASelector;
import org.openl.util.FileTreeIterator;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class ClassLoaderFactory
{

	//	public ClassLoaderFactory()
	//	{
	////		userClassLoaders.put("org.openl.core", getOpenlCoreLoader());
	//	}

	public static ClassLoader getOpenlCoreLoader(ClassLoader ucl)
	{
		try 
		{
			Class c = ucl.loadClass(OpenL.class.getName());
			if (c != null)
				return ucl;
			
		} catch (Exception e) {
		}
		
		return OpenL.class.getClassLoader();
	}

	public static synchronized HashMap reset()
	{
		HashMap oldLoaders = userClassLoaders;

		userClassLoaders = new HashMap();

		return oldLoaders;
	}

	static final class Key
	{
		String name;
		String classpath;
		ClassLoader parent;
		IUserContext cxt;
		Key(
			String name,
			String classpath,
			ClassLoader parent,
			IUserContext cxt)
		{
			this.name = name;
			this.classpath = classpath;
			this.parent = parent;
			this.cxt = cxt;
		}

		public boolean equals(Object obj)
		{
			if (!(obj instanceof Key))
				return false;
			Key k = (Key) obj;

			return new EqualsBuilder()
			//				.append(name, k.name)
			.append(classpath, k.classpath)
			.append(cxt, k.cxt)
			.append(parent, k.parent)
			.isEquals();
		}

		public int hashCode()
		{
			return new HashCodeBuilder()
			//				.append(name)
			.append(parent).append(cxt).append(classpath).toHashCode();
		}

	}

	public static synchronized ClassLoader createUserClassloader(
		String name,
		String classpath,
		ClassLoader parent,
		IUserContext ucxt)
		throws Exception
	{

		Log.debug(
			"name=" + name + " cp=" + classpath + " " + ucxt + " cl=" + parent);

		Key key = new Key(name, classpath, parent, ucxt);
		ClassLoader loader = (ClassLoader) userClassLoaders.get(key);

		Log.debug(loader == null ? "New" : "Old");

		if (loader == null)
		{
			loader = createClassLoader(classpath, parent, ucxt);
			//TODO fix it			
			userClassLoaders.put(key, loader);
		}

		return loader;
	}

	static HashMap userClassLoaders = new HashMap();

	static public ClassLoader createClassLoader(
		String classpath,
		ClassLoader parent,
		IUserContext ucxt)
		throws Exception
	{

		return createClassLoader(splitClassPath(classpath), parent, ucxt);
	}

	static public ClassLoader createClassLoader(
		String[] classpath,
		ClassLoader parent,
		IUserContext ucxt)
		throws Exception
	{
		Vector v = new Vector();
		for (int i = 0; i < classpath.length; i++)
		{

			if (classpath[i].endsWith("*"))
				makeWildcardPath(makeFile(ucxt.getUserHome(), classpath[i].substring(0, classpath[i].length()-1)), v);
			else
			{

				File f = makeFile(ucxt.getUserHome(), classpath[i]);

				if (!f.exists())
				{
					throw new IOException(
						"File " + f.getPath() + " does not exist");
				}

				v.add(makeFile(ucxt.getUserHome(), classpath[i]).toURL());
			}

			//			System.out.println(urls[i].toExternalForm());
		}

		URL[] urls = (URL[]) v.toArray(new URL[v.size()]);
		return new URLClassLoader(urls, parent);
	}

	/**
	 * @param string
	 * @param string2
	 * @param v
	 */
	public static void makeWildcardPath(File root, Vector v)
	{
  	
		ISelector sel = new ASelector()
		{
			public boolean select(Object obj)
			{
				File f = (File) obj;
				String apath = f.getAbsolutePath(); 
				boolean res = apath.endsWith(".jar") || apath.endsWith(".zip");
//					Log.info(f.getAbsolutePath());
//				if (res)
//				  Log.info(f.getAbsolutePath());
				return res;  
			} 
  		
		};

		Iterator iter = new FileTreeIterator(root, 0).select(sel);
		
		
		for (; iter.hasNext();)
		{
				File f = (File) iter.next();
				try
				{
					v.add(f.toURL());
				}
				catch (MalformedURLException e)
				{
					throw RuntimeExceptionWrapper.wrap(e);
				}
		}
		
	}

	static File makeFile(String root, String name) throws Exception
	{
		File f = new File(name);

		if (f.isAbsolute() || name.startsWith("/"))
			return f.getCanonicalFile();

		return new File(root, name).getCanonicalFile();

	}

	static protected String[] splitClassPath(String classpath)
	{
		StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);

		String[] res = new String[st.countTokens()];
		for (int i = 0; i < res.length; i++)
		{
			res[i] = st.nextToken();
		}
		return res;
	}

	//	static class CurrentFactory extends ThreadLocal
	//	{
	//	}
	//
	//	static CurrentFactory _currentFactory = new CurrentFactory();
	//
	//	public static ClassLoaderFactory getCurrentFactory()
	//	{
	//		return (ClassLoaderFactory) _currentFactory.get();
	//	}
	//
	//	public static void setCurrentFactory(ClassLoaderFactory m)
	//	{
	//		_currentFactory.set(m);
	//	}

}
