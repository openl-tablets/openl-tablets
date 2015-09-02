package org.openl.extension.xmlrules;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.conf.IUserContext;
import org.openl.dependency.CompiledDependency;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

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
}
