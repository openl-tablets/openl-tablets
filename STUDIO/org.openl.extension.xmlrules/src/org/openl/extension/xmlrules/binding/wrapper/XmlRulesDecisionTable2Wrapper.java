package org.openl.extension.xmlrules.binding.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.DecisionTable2Wrapper;
import org.openl.types.IMethodSignature;
import org.openl.vm.IRuntimeEnv;

public class XmlRulesDecisionTable2Wrapper extends DecisionTable2Wrapper {
    private final ProjectData projectData;

    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final List<Integer> dimensionNums;
    private final Class<?>[] paramTypes;

    public XmlRulesDecisionTable2Wrapper(XlsModuleOpenClass xlsModuleOpenClass,
            DecisionTable delegate, ProjectData projectData) {
        super(xlsModuleOpenClass, delegate);
        this.projectData = projectData;
        this.xlsModuleOpenClass = xlsModuleOpenClass;

        final IMethodSignature signature = delegate.getMethod().getSignature();
        int numberOfParameters = signature.getNumberOfParameters();

        dimensionNums = new ArrayList<Integer>();
        for (int i = 0; i < numberOfParameters; i++) {
            if (signature.getParameterName(i).startsWith("dim")) {
                dimensionNums.add(i);
            }
        }

        paramTypes = new Class[signature.getNumberOfParameters()];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = signature.getParameterType(i).getInstanceClass();
        }
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
            for (int i = 0; i < params.length; i++) {
                params[i] = HelperFunctions.convertArgument(paramTypes[i], params[i]);
            }

            for (Integer dimensionNum : dimensionNums) {
                Object param = params[dimensionNum];
                if (param instanceof String) {
                    params[dimensionNum] = ((String) param).toLowerCase();
                }
            }
            return super.invoke(target, params, env);
        } finally {
            if (topLevel) {
                LazyCellExecutor.reset();
                ProjectData.removeCurrentInstance();
            }
        }
    }
}
