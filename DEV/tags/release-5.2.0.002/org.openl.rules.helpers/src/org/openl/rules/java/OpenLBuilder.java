package org.openl.rules.java;

import org.openl.OpenConfigurationException;
import org.openl.OpenL;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.JavaImportTypeConfiguration;
import org.openl.conf.JavaLibraryConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NameSpacedLibraryConfiguration;
import org.openl.conf.NameSpacedTypeConfiguration;
import org.openl.conf.NoAntOpenLTask;
import org.openl.conf.TypeFactoryConfiguration;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilder extends AOpenLBuilder
{
	public OpenL build(String category) throws OpenConfigurationException
	{
		OpenL.getInstance("org.openl.j", getUserContext());
		return super.build(category);
	}

	public NoAntOpenLTask getNoAntOpenLTask()
	{
		NoAntOpenLTask op = new NoAntOpenLTask();
		
		
		op.setExtendsCategory("org.openl.j");
		op.setCategory("org.openl.rules.java");


		LibraryFactoryConfiguration libraries = op.createLibraries();
		NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
		library.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
		
		JavaLibraryConfiguration javalib1 = new JavaLibraryConfiguration();
		javalib1.setClassName("org.openl.rules.helpers.Util");
		library.addJavalib(javalib1);

		JavaLibraryConfiguration javalib2 = new JavaLibraryConfiguration();
		javalib2.setClassName("org.openl.meta.DoubleValue");
		library.addJavalib(javalib2);
		
		JavaLibraryConfiguration javalib3 = new JavaLibraryConfiguration();
		javalib3.setClassName("java.lang.Math");
		library.addJavalib(javalib3);
		
		libraries.addConfiguredLibrary(library);
		
		/**
   <libraries>

		<library namespace="org.openl.this">
			<javalib classname="org.openl.rules.helpers.Util"/>
		</library>
  </libraries>
*/
		
		TypeFactoryConfiguration types = op.createTypes();
		NameSpacedTypeConfiguration typelibrary = new NameSpacedTypeConfiguration();
		typelibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
		
		JavaImportTypeConfiguration javaimport1 = new JavaImportTypeConfiguration();
		javaimport1.setAll("org.openl.rules.helpers");
		typelibrary.addJavaImport(javaimport1);

		JavaImportTypeConfiguration javaimport2 = new JavaImportTypeConfiguration();
		javaimport2.setAll("org.openl.meta");
		typelibrary.addJavaImport(javaimport2);

		JavaImportTypeConfiguration javaimport3 = new JavaImportTypeConfiguration();
		javaimport3.setAll("org.openl.rules.helpers.scope");
		typelibrary.addJavaImport(javaimport3);
		
		JavaImportTypeConfiguration javaimport4 = new JavaImportTypeConfiguration();
		javaimport4.setAll("org.openl.rules.calc");
		typelibrary.addJavaImport(javaimport4);
		

		types.addConfiguredTypeLibrary(typelibrary);
		
		
		
		/*
 
  <types>
    <typelibrary namespace="org.openl.this">
      <javaimport all="${org.openl.rules.java.project.imports}"/>
      <javaimport all="org.openl.rules.helpers"/>
    </typelibrary>
  </types>

 */		
		
		
		return op;
	}


}
