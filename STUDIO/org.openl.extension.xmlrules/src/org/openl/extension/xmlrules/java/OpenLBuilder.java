package org.openl.extension.xmlrules.java;

import org.openl.OpenL;
import org.openl.conf.*;
import org.openl.extension.xmlrules.binding.XmlRulesMethodNodeBinder;
import org.openl.extension.xmlrules.utils.ArrayOperators;
import org.openl.extension.xmlrules.utils.InternalFunctions;
import org.openl.extension.xmlrules.utils.XmlRules;
import org.openl.extension.xmlrules.utils.XmlRulesOperators;
import org.openl.syntax.impl.ISyntaxConstants;

public class OpenLBuilder extends AOpenLBuilder {
    public static final String OPENL_XMLRULES_JAVA_NAME = OpenLBuilder.class.getPackage().getName();

    private static final String[] JAVA_LIBRARY_NAMES = new String[] {
            XmlRules.class.getName(),
            InternalFunctions.class.getName()
    };

    private static final String[] OPERATOR_LIBRARY_NAMES = new String[] {
            ArrayOperators.class.getName(),
            XmlRulesOperators.class.getName()
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

        addLibraries(libraries, ISyntaxConstants.OPERATORS_NAMESPACE, OPERATOR_LIBRARY_NAMES);
        addLibraries(libraries, ISyntaxConstants.THIS_NAMESPACE, JAVA_LIBRARY_NAMES);

        NodeBinderFactoryConfiguration nbc = op.createBindings();

        String[] binders = {
                "function", XmlRulesMethodNodeBinder.class.getName(),
        };

        for (int i = 0; i < binders.length / 2; i++) {
            NodeBinderFactoryConfiguration.SingleBinderFactory sbf = new NodeBinderFactoryConfiguration.SingleBinderFactory();
            sbf.setNode(binders[2 * i]);
            sbf.setClassName(binders[2 * i + 1]);
            nbc.addConfiguredBinder(sbf);
        }

        return op;
    }

    private void addLibraries(LibraryFactoryConfiguration libraries, String namespace, String[] libraryNames) {
        NameSpacedLibraryConfiguration library = new NameSpacedLibraryConfiguration();
        library.setNamespace(namespace);

        for (String libraryName : libraryNames) {
            JavaLibraryConfiguration configuration = new JavaLibraryConfiguration();
            configuration.setClassName(libraryName);
            library.addJavalib(configuration);
        }

        libraries.addConfiguredLibrary(library);
    }
}
