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
        org.openl.rules.helpers.RulesUtils.class.getName(),
        org.openl.rules.dt2.algorithm.evaluator.CtrUtils.class.getName(),
        org.openl.meta.ByteValue.class.getName(), 
        org.openl.meta.ShortValue.class.getName(),
        org.openl.meta.IntValue.class.getName(),  
        org.openl.meta.LongValue.class.getName(), 
        org.openl.meta.FloatValue.class.getName(),
        org.openl.meta.DoubleValue.class.getName(),
        org.openl.meta.BigIntegerValue.class.getName(),
        org.openl.meta.BigDecimalValue.class.getName()};
    
    private static final String[] JAVA_OPERATORS_CLASSES = new String[]{
        org.openl.meta.ByteValue.class.getName(), 
        org.openl.meta.ShortValue.class.getName(),
        org.openl.meta.IntValue.class.getName(),  
        org.openl.meta.LongValue.class.getName(), 
        org.openl.meta.FloatValue.class.getName(),
        org.openl.meta.DoubleValue.class.getName(),
        org.openl.meta.BigIntegerValue.class.getName(),
        org.openl.meta.BigDecimalValue.class.getName()};
    
    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        OpenL.getInstance(OpenL.OPENL_J_NAME, getUserEnvironmentContext());
        return super.build(category);
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(OpenL.OPENL_J_NAME);
        op.setCategory(OpenL.OPENL_JAVA_NAME);

        LibraryFactoryConfiguration libraries = op.createLibraries();
        
        NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
        library.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
        
        for (String javaLibConfiguration : JAVA_LIBRARY_NAMES) {
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(javaLibConfiguration);
            library.addJavalib(javalib);
        }        

        libraries.addConfiguredLibrary(library);

        NameSpacedLibraryConfiguration nslc = new NameSpacedLibraryConfiguration();
        nslc.setNamespace(ISyntaxConstants.OPERATORS_NAMESPACE);
        JavaLibraryConfiguration javalib1 = new JavaLibraryConfiguration();
        javalib1.setClassName(org.openl.binding.impl.Operators.class.getName());
        nslc.addJavalib(javalib1);
        for (String className : JAVA_OPERATORS_CLASSES){
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(className);
            nslc.addJavalib(javalib);
        }
        libraries.addConfiguredLibrary(nslc);
        
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
