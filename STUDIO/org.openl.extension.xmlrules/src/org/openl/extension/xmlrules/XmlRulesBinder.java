package org.openl.extension.xmlrules;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.IMemberBoundNode;
import org.openl.conf.IUserContext;
import org.openl.dependency.CompiledDependency;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.project.XmlRulesModuleSyntaxNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
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
    public IBoundCode bind(IParsedCode parsedCode, IBindingContext bindingContext) {
        try {
            XmlRulesModuleSyntaxNode topNode = (XmlRulesModuleSyntaxNode) parsedCode.getTopNode();
            ProjectData.setCurrentInstance(topNode.getProjectData());
            return super.bind(parsedCode, bindingContext);
        } finally {
            ProjectData.removeCurrentInstance();
        }
    }

    @Override
    protected void finilizeBind(IMemberBoundNode memberBoundNode,
            TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext moduleContext) {
        if (memberBoundNode instanceof AMethodBasedNode) {
            try {
                String methodName = ((AMethodBasedNode) memberBoundNode).getMethod().getName();

                ExpressionContext expressionContext = new ExpressionContext();
                expressionContext.setCurrentPath(ProjectData.getCurrentInstance().getPath(methodName));
                ExpressionContext.setInstance(expressionContext);
                super.finilizeBind(memberBoundNode, tableSyntaxNode, moduleContext);
            } finally {
                ExpressionContext.removeInstance();
            }
        } else {
            super.finilizeBind(memberBoundNode, tableSyntaxNode, moduleContext);
        }
    }
}
