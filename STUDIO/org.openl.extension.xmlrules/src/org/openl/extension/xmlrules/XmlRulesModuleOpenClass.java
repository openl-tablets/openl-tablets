package org.openl.extension.xmlrules;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.OpenLSystemProperties;
import org.openl.extension.xmlrules.binding.wrapper.XmlRulesDecisionTableDecorator;
import org.openl.extension.xmlrules.binding.wrapper.XmlRulesSpreadsheetDecorator;
import org.openl.extension.xmlrules.binding.wrapper.XmlRulesTableMethodDecorator;
import org.openl.extension.xmlrules.project.XmlRulesModuleSyntaxNode;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.data.IDataBase;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.types.IOpenMethod;

public class XmlRulesModuleOpenClass extends XlsModuleOpenClass {
    private final ProjectData projectData;
    public XmlRulesModuleOpenClass(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> moduleDependencies, IBindingContext bindingContext) {
        super(XlsHelper.getModuleName(moduleNode),
                new XlsMetaInfo(moduleNode),
                openl,
                dbase,
                moduleDependencies,
                Thread.currentThread().getContextClassLoader(),
                OpenLSystemProperties.isDTDispatchingMode(bindingContext.getExternalParams()),
                OpenLSystemProperties.isDispatchingValidationEnabled(bindingContext.getExternalParams()));

        this.projectData = ((XmlRulesModuleSyntaxNode) moduleNode).getProjectData();
    }

    @Override
    protected IOpenMethod decorateForMultimoduleDispatching(IOpenMethod openMethod) {
        final XlsModuleOpenClass xlsModuleOpenClass = this;
        if (openMethod instanceof TableMethod) {
            openMethod = new XmlRulesTableMethodDecorator(xlsModuleOpenClass, (TableMethod) openMethod, projectData);
        }

        if (openMethod instanceof DecisionTable) {
            openMethod = new XmlRulesDecisionTableDecorator(xlsModuleOpenClass, (DecisionTable) openMethod, projectData);
        }

        if (openMethod instanceof Spreadsheet) {
            openMethod = new XmlRulesSpreadsheetDecorator(xlsModuleOpenClass, (Spreadsheet) openMethod, projectData);
        }

        return super.decorateForMultimoduleDispatching(openMethod);
    }

}
