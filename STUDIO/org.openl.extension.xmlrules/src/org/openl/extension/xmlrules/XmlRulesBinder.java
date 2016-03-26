package org.openl.extension.xmlrules;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.conf.IUserContext;
import org.openl.dependency.CompiledDependency;
import org.openl.extension.xmlrules.project.XmlRulesModuleSyntaxNode;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.syntax.code.IParsedCode;

public class XmlRulesBinder extends XlsBinder {
    public XmlRulesBinder(IUserContext userContext) {
        super(userContext);
    }

    @Override
    protected String getDefaultOpenLName() {
        return org.openl.extension.xmlrules.java.OpenLBuilder.OPENL_XMLRULES_JAVA_NAME;
    }

    @Override
    protected XlsModuleOpenClass createModuleOpenClass(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> moduleDependencies,
            IBindingContext bindingContext) {
        return new XmlRulesModuleOpenClass(moduleNode, openl, dbase, moduleDependencies, bindingContext);
    }

    @Override
    public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator bindingContextDelegator) {
        try {
            XmlRulesModuleSyntaxNode topNode = (XmlRulesModuleSyntaxNode) parsedCode.getTopNode();
            ProjectData.setCurrentInstance(topNode.getProjectData());
            return super.bind(parsedCode, bindingContextDelegator);
        } finally {
            ProjectData.removeCurrentInstance();
        }
    }
}
