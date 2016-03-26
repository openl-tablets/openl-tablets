package org.openl.extension.xmlrules.binding.wrapper;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.TableMethodWrapper;
import org.openl.rules.method.table.TableMethod;
import org.openl.vm.IRuntimeEnv;

public class XmlRulesTableMethodWrapper extends TableMethodWrapper {
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ProjectData projectData;
    private final ArgumentsConverter argumentsConverter;

    public XmlRulesTableMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            TableMethod delegate,
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
}
