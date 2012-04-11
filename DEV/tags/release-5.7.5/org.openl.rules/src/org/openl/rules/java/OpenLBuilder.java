package org.openl.rules.java;

import org.openl.OpenL;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.JavaImportTypeConfiguration;
import org.openl.conf.JavaLibraryConfiguration;
import org.openl.conf.LibraryFactoryConfiguration;
import org.openl.conf.NameSpacedLibraryConfiguration;
import org.openl.conf.NameSpacedTypeConfiguration;
import org.openl.conf.NoAntOpenLTask;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.TypeFactoryConfiguration;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilder extends AOpenLBuilder {
    
    private static final String[] JAVA_LIBRARY_NAMES = new String[]{
        "org.openl.rules.helpers.RulesUtils",
        "java.lang.Math",
        "org.openl.meta.ByteValue", // don`t change the order of elements!
        "org.openl.meta.ShortValue", // as lower types can be casted to upper ones.
        "org.openl.meta.IntValue",  // appropriate methods should be looking for from
        "org.openl.meta.LongValue", // lower to upper hierarchy level.
        "org.openl.meta.FloatValue",
        "org.openl.meta.DoubleValue",
        "org.openl.meta.BigIntegerValue",
        "org.openl.meta.BigDecimalValue"};
    
    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        OpenL.getInstance("org.openl.j", getUserEnvironmentContext());
        return super.build(category);
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory("org.openl.j");
        op.setCategory("org.openl.rules.java");

        LibraryFactoryConfiguration libraries = op.createLibraries();
        
        NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
        library.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
        
        for (String javaLibConfiguration : JAVA_LIBRARY_NAMES) {
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(javaLibConfiguration);
            library.addJavalib(javalib);
        }        

        libraries.addConfiguredLibrary(library);

        /**
         * <libraries>
         *
         * <library namespace="org.openl.this"> <javalib
         * classname="org.openl.rules.helpers.RulesUtils"/> </library> </libraries>
         */

        TypeFactoryConfiguration types = op.createTypes();
        
        NameSpacedTypeConfiguration typelibrary = new NameSpacedTypeConfiguration();
        typelibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        JavaImportTypeConfiguration javaimport1 = new JavaImportTypeConfiguration();
        javaimport1.setImport("org.openl.rules.helpers");
        typelibrary.addConfiguration(javaimport1);

        JavaImportTypeConfiguration javaimport2 = new JavaImportTypeConfiguration();        
        javaimport2.setImport("org.openl.meta");
        typelibrary.addConfiguration(javaimport2);

        JavaImportTypeConfiguration javaimport3 = new JavaImportTypeConfiguration();        
        javaimport3.setImport("org.openl.rules.helpers.scope");
        typelibrary.addConfiguration(javaimport3);

        JavaImportTypeConfiguration javaimport4 = new JavaImportTypeConfiguration();                
        javaimport4.setImport("org.openl.rules.calc");
        javaimport4.setImport("org.openl.rules.calc.result");
        typelibrary.addConfiguration(javaimport4);

        types.addConfiguredTypeLibrary(typelibrary);

        /*
         *
         * <types> <typelibrary namespace="org.openl.this"> <javaimport
         * all="${org.openl.rules.java.project.imports}"/> <javaimport
         * all="org.openl.rules.helpers"/> </typelibrary> </types>
         *
         */

        return op;
    }

}
