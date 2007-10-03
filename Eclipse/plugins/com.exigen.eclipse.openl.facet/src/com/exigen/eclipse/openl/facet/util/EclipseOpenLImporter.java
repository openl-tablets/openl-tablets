package com.exigen.eclipse.openl.facet.util;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import com.exigen.eclipse.common.core.CommonCore;
import com.exigen.eclipse.common.core.artefact.project.CommonProject;
import com.exigen.eclipse.common.core.artefact.project.facet.JavaFacet;
import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.common.core.internal.ExceptionHandler;
import com.exigen.eclipse.common.util.Assert;
import com.exigen.eclipse.openl.facet.Activator;
import com.exigen.openl.component.ErrorListener;
import com.exigen.openl.model.openl.RuleSetFile;
import com.sun.org.apache.bcel.internal.util.ClassPath;

public class EclipseOpenLImporter {
	private static EclipseOpenLImporter instance = null;

	protected ClassPath classPath;

	protected Map<CommonProject, ClassLoader> cashedClassLoaders = new HashMap<CommonProject, ClassLoader>();

	protected EclipseOpenLImporter() {
	}

	public static EclipseOpenLImporter getInstance() {
		if (instance == null) {
			instance = new EclipseOpenLImporter();
		}
		return instance;
	}

	protected ClassLoader getClassLoader(CommonProject commonProject)
			throws CommonException {
		if (cashedClassLoaders.containsKey(commonProject)) {
			return cashedClassLoaders.get(commonProject);
		}

		List<URL> urls = new ArrayList<URL>();
		JavaFacet javaFacet = commonProject.getFacet(JavaFacet.class);
		try {
			IPackageFragmentRoot[] allPackageFragmentRoots = javaFacet
					.getCorrespondingJavaProject().getAllPackageFragmentRoots();
			for (IPackageFragmentRoot fragmentRoot : allPackageFragmentRoots) {
				urls.add(fragmentRoot.getPath().toFile().toURL());
			}
		} catch (JavaModelException e) {
			ExceptionHandler
					.handleCoreExceptionThatCannotBeIgnored(e,
							"Cannot determine Java classpath to parse Excel file with OpenL Tablets rules");
		} catch (MalformedURLException e) {
			ExceptionHandler
					.handleSystemExceptionThatCannotBeIgnored(e,
							"Cannot determine Java classpath to parse Excel file with OpenL Tablets rules");
		}

		String jars[] = { "com.exigen.openl.importer.jar", "commons-4.1.1.jar",
				"commons-lang-2.1.jar", "commons-logging-1.0.4.jar",
				"core-4.1.1.jar", "j-4.1.1.jar",
				"lib-apache-poi-modified-4.1.1.jar", "rules-4.1.1.jar",
				"rules-helpers-4.1.1.jar" };
		for (String jar : jars) {
			URL importerJarURL = FileLocator
					.find(Activator.getDefault().getBundle(), new Path("lib/"
							+ jar), Collections.EMPTY_MAP);
			if (importerJarURL == null) {
				throw new CommonException("Cannot load JAR \"" + jar + "\"");
			}
			urls.add(importerJarURL);
		}

		URLClassLoader classLoader = new URLClassLoader(urls
				.toArray(new URL[urls.size()]), Thread.currentThread()
				.getContextClassLoader());

		cashedClassLoaders.put(commonProject, classLoader);

		return classLoader;
	}

	public RuleSetFile importExcelFile(IFile excelFile,
			ErrorListener errorListener) throws CommonException {
		try {
			CommonProject commonProject = CommonCore.getWorkspace().getProject(
					excelFile.getProject().getName());
			ClassLoader classLoader = getClassLoader(commonProject);
			Class<?> importerClass = classLoader
					.loadClass("com.exigen.openl.importer.OpenLImporter");
			Assert.isNotNull(importerClass);

			Object importer = importerClass.newInstance();
			Assert.isNotNull(importer);

			Method importMethod = importerClass.getMethod("importExcelFile",
					new Class[] { String.class, ErrorListener.class });

			ClassLoader currentClassLoader = Thread.currentThread()
					.getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(classLoader);

				String excelFileName = excelFile.getLocation().toString();
				return (RuleSetFile) importMethod.invoke(importer,
						new Object[] { excelFileName, errorListener });
			} finally {
				Thread.currentThread()
						.setContextClassLoader(currentClassLoader);
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			ExceptionHandler.handleSystemExceptionThatCannotBeIgnored(e,
					"Cannot import Excel file with OpenL Tablets \""
							+ excelFile.getLocation().toString() + "\"");
		}
		return null;
	}

	public String getCellRegionFor(IFile excelFile, String decisionTableName)
			throws CommonException {
		try {
			CommonProject commonProject = CommonCore.getWorkspace().getProject(
					excelFile.getProject().getName());
			ClassLoader classLoader = getClassLoader(commonProject);
			Class<?> importerClass = classLoader
					.loadClass("com.exigen.openl.importer.OpenLImporter");
			Assert.isNotNull(importerClass);

			Object importer = importerClass.newInstance();
			Assert.isNotNull(importer);

			Method importMethod = importerClass.getMethod("getCellRegionFor",
					new Class[] { String.class, String.class });

			ClassLoader currentClassLoader = Thread.currentThread()
					.getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(classLoader);

				String excelFileName = excelFile.getLocation().toString();
				return (String) importMethod.invoke(importer, new Object[] {
						excelFileName, decisionTableName });
			} finally {
				Thread.currentThread()
						.setContextClassLoader(currentClassLoader);
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			ExceptionHandler.handleSystemExceptionThatCannotBeIgnored(e,
					"Cannot find region in Excel file with OpenL Tablets \""
							+ excelFile.getLocation().toString()
							+ "\" for decision table \"" + decisionTableName
							+ "\"");
		}
		return null;
	}
}
