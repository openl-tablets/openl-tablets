package org.openl.extension.xmlrules.java;

import org.openl.OpenL;
import org.openl.conf.*;
import org.openl.extension.xmlrules.utils.InternalFunctions;
import org.openl.extension.xmlrules.utils.XmlRules;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilder extends AOpenLBuilder {
    public static final String OPENL_XMLRULES_JAVA_NAME = OpenLBuilder.class.getPackage().getName();

    private static final String[] JAVA_LIBRARY_NAMES = new String[] {
            XmlRules.class.getName(),
            InternalFunctions.class.getName()
    };

    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        OpenL.getInstance(OpenL.OPENL_JAVA_NAME, getUserEnvironmentContext());
        return super.build(category);
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(OpenL.OPENL_JAVA_NAME);
        op.setCategory(OPENL_XMLRULES_JAVA_NAME);

        LibraryFactoryConfiguration libraries = op.createLibraries();

        NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
        library.setNamespace(ISyntaxConstants.THIS_NAMESPACE);

        for (String javaLibConfiguration : JAVA_LIBRARY_NAMES) {
            JavaLibraryConfiguration javalib = new JavaLibraryConfiguration();
            javalib.setClassName(javaLibConfiguration);
            library.addJavalib(javalib);
        }

        libraries.addConfiguredLibrary(library);

        return op;
    }
}
