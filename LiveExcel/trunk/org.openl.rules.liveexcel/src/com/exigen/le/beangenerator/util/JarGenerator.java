package com.exigen.le.beangenerator.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.exigen.le.LiveExcel;
import com.exigen.le.beangenerator.BeanGenerator;
import com.exigen.le.beangenerator.BeanTreeGenerator;
import com.exigen.le.beangenerator.GeneratorClassLoader;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;

/**
 * Utility class to create jar with generated Beans
 * @author zsulkins
 *
 */
public class JarGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length <= 1){
			System.out.println("Usage: JarGenerator <jarFileName> <project> [<revision>] [<LE propertyFile>]");
			System.exit(1);
		}
		
		String jarFileName = args[0];
		String project = args[1];
		
		String revision = "";
		if (args.length > 2){
			revision = args[2];
		}
		String propFileName = "le.properties";
		if (args.length > 3){
			propFileName = args[3];
		}
		
		URL propURL = JarGenerator.class.getClassLoader().getResource(propFileName);
		InputStream is = JarGenerator.class.getClassLoader().getResourceAsStream(propFileName);
		if (is == null){
			System.out.println("Property file not found: " + propFileName);
			System.exit(2);
		}
		
		Properties prop = new Properties();
		try {
			prop.load(is);
		} catch (IOException ioe){
			System.out.println("Property file can not be read: " + propURL);
			System.out.println("Reason: " + ioe.getCause());
			System.exit(3);
		}
		
		// print parameters
		System.out.println("Jar: " + jarFileName);
		System.out.println("Project: " + project);
		System.out.println("Revision: " + revision);
		System.out.println("Properties: " + propURL);
		prop.list(System.out);

		
		try {
			createJarWithBeans(project, revision, prop, jarFileName);
		} catch (IOException ioe){
			System.out.println("Failed to create jar");
			ioe.printStackTrace();
			System.exit(4);
		}

		System.out.println("Jar file created");
		System.exit(0);
		
	}
	
	public static void createJarWithBeans(String project, String revision, Properties leProperties, String jarFileName) throws IOException {

		File jarFile = new File(jarFileName);
		FileOutputStream stream = new FileOutputStream(jarFile);

		LiveExcel le = LiveExcel.getInstance();
		le.init(leProperties);
		VersionDesc vd = new VersionDesc(revision);
		ServiceModel  sm =le.getServiceModel(project, vd);
		VersionDesc def = le.getDefaultVersionDesc(project);
		List<Type> types = sm.getTypes();
		
		Manifest mf = createManifest(project, def.getVersion());
		JarOutputStream jout = new JarOutputStream(stream, mf);

		for(Type root:types){
			GeneratorClassLoader cl = new GeneratorClassLoader();
			// generate classes for beans
			BeanTreeGenerator.loadBeanClasses(root.getName(),root, cl, null, jout);
		}
		jout.close();
		stream.close();
		
	}
	
	static Manifest createManifest(String project, String revision){
		 final StringBuilder sbuf = new StringBuilder();
		 sbuf.append(Attributes.Name.MANIFEST_VERSION.toString());
		 sbuf.append(": 1.0" + "\n");
		 sbuf.append("Project: "+ project + "\n");
		 sbuf.append("Revision: "+ revision + "\n");
		 
		 ByteArrayInputStream bais = null;
		 try {  bais = new ByteArrayInputStream(sbuf.toString().getBytes("UTF-8")); }
		    catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
		 
		 Manifest mf = new Manifest();
		 try {
			 mf.read(bais);
		 } catch (IOException ioe){
			 throw new RuntimeException(ioe);
		 }
		 return mf;
	}

}
