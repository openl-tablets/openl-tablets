package org.openl.conf;

import java.util.Collection;
import java.util.HashSet;

import org.openl.OpenL;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilderImpl extends AOpenLBuilder {

    private String extendsCategory = OpenL.OPENL_J_NAME;

    private String category;

    private Collection<String> imports = new HashSet<String>();

    private String libName;

    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader userEnvitonmentContextClassLoader = getUserEnvironmentContext().getUserClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(userEnvitonmentContextClassLoader);
            OpenL.getInstance(extendsCategory, getUserEnvironmentContext());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return super.build(category);
    }

    public String getCategory() {
        return category;
    }

    public String getExtendsCategory() {
        return extendsCategory;
    }

    public Collection<String> getImports() {
        return imports;
    }

    public String getLibName() {
        return libName;
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(extendsCategory);
        op.setCategory(category);

        LibraryFactoryConfiguration libraries = op.createLibraries();
        if (libName != null) {
            NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
            library.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(libName);
            library.addJavalib(javalib);
            libraries.addConfiguredLibrary(library);
        }
        
        /**
         * <libraries>
         * 
         * <library namespace="org.openl.this"> <javalib
         * classname="org.openl.rules.helpers.RulesUtils"/> </library>
         * </libraries>
         */

        if (!getImports().isEmpty()) {
            TypeFactoryConfiguration types = op.createTypes();
            NameSpacedTypeConfiguration typelibrary = new NameSpacedTypeConfiguration();
            typelibrary.setNamespace(ISyntaxConstants.THIS_NAMESPACE);
            JavaImportTypeConfiguration javaimport = new JavaImportTypeConfiguration();
            // javaimport.setAll("com.exigen.ipb.rm.uk");
            //
            // typelibrary.addJavaImport(javaimport);

            javaimport = new JavaImportTypeConfiguration();
            javaimport.setAllImports(getImports());

            typelibrary.addConfiguration(javaimport);

            types.addConfiguredTypeLibrary(typelibrary);
        }

        /*
         * 
         * <types> <typelibrary namespace="org.openl.this"> <javaimport
         * all="${org.openl.rules.java.project.imports}"/> <javaimport
         * all="org.openl.rules.helpers"/> </typelibrary> </types>
         */

        return op;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setExtendsCategory(String extendsCategory) {
        this.extendsCategory = extendsCategory;
    }

    public void setImports(Collection<String> imports) {
        this.imports = imports;
    }

    public void setLibName(String libName) {
        this.libName = libName;
    }

}
