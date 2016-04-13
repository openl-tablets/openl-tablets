package org.openl.extension.xmlrules.binding.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.MatchingOpenMethodDispatcherWrapper;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class XmlRulesMatchingOpenMethodDispatcherWrapper extends MatchingOpenMethodDispatcherWrapper {
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ProjectData projectData;
    private final ArgumentsConverter argumentsConverter;

    public XmlRulesMatchingOpenMethodDispatcherWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            MatchingOpenMethodDispatcher delegate,
            ProjectData projectData) {
        super(xlsModuleOpenClass, delegate);
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.projectData = projectData;

        argumentsConverter = new ArgumentsConverter(delegate.getMethod());
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        boolean topLevel = cache == null;
        if (topLevel) {
            cache = new LazyCellExecutor(xlsModuleOpenClass, target, env);
            LazyCellExecutor.setInstance(cache);
            ProjectData.setCurrentInstance(projectData);
        }
        try {
            params = argumentsConverter.convert(params);
            return super.invoke(target, params, env);
        } finally {
            if (topLevel) {
                LazyCellExecutor.reset();
                ProjectData.removeCurrentInstance();
            }
        }
    }

    @Override
    public List<IOpenMethod> getCandidates() {
        List<IOpenMethod> candidates = super.getCandidates();
        List<IOpenMethod> newCandidates = new ArrayList<IOpenMethod>();

        for (IOpenMethod candidate : candidates) {
            // Prohibit unwrapping of several XmlRules methods inside of dispatcher, because they contain essential logic needed during execution
            if (!(candidate instanceof XmlRulesMethodDelegator)) {
                if (candidate instanceof TableMethod) {
                    candidate = new XmlRulesTableMethodDelegator(new XmlRulesTableMethodDecorator(xlsModuleOpenClass,
                            (TableMethod) candidate,
                            projectData));
                } else if (candidate instanceof DecisionTable) {
                    candidate = new XmlRulesDecisionTable2Delegator(new XmlRulesDecisionTableDecorator(xlsModuleOpenClass,
                            (DecisionTable) candidate,
                            projectData));
                }
            }

            newCandidates.add(candidate);
        }

        return newCandidates;
    }
}
