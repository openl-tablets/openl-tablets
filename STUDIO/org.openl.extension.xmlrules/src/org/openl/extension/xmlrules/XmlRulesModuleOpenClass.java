package org.openl.extension.xmlrules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.OpenLSystemProperties;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.data.IDataBase;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.DecisionTable2Wrapper;
import org.openl.rules.lang.xls.binding.wrapper.MatchingOpenMethodDispatcherWrapper;
import org.openl.rules.lang.xls.binding.wrapper.SpreadsheetWrapper;
import org.openl.rules.lang.xls.binding.wrapper.TableMethodWrapper;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

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

        this.projectData = ProjectData.getCurrentInstance();
    }

    @Override
    protected IOpenMethod decorateForMultimoduleDispatching(final IOpenMethod openMethod) {
        final XlsModuleOpenClass xlsModuleOpenClass = this;
        if (openMethod instanceof TableMethod) {
            return new TableMethodWrapper(this, (TableMethod) openMethod) {
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
                        return super.invoke(target, params, env);
                    } finally {
                        if (topLevel) {
                            LazyCellExecutor.reset();
                            ProjectData.removeCurrentInstance();
                        }
                    }
                }
            };
        }

        if (openMethod instanceof DecisionTable) {
            IMethodSignature signature = openMethod.getMethod().getSignature();
            int numberOfParameters = signature.getNumberOfParameters();
            final List<Integer> dimensionNums = new ArrayList<Integer>();
            for (int i = 0; i < numberOfParameters; i++) {
                if (signature.getParameterName(i).startsWith("dim")) {
                    dimensionNums.add(i);
                }
            }
            return new DecisionTable2Wrapper(xlsModuleOpenClass, (DecisionTable) openMethod) {
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
            };
        }

        if (openMethod instanceof Spreadsheet) {
            return new SpreadsheetWrapper(xlsModuleOpenClass, (Spreadsheet) openMethod) {
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
                        return super.invoke(target, params, env);
                    } finally {
                        if (topLevel) {
                            LazyCellExecutor.reset();
                            ProjectData.removeCurrentInstance();
                        }
                    }
                }
            };
        }

        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return new MatchingOpenMethodDispatcherWrapper(xlsModuleOpenClass, (MatchingOpenMethodDispatcher) openMethod) {
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
                        return super.invoke(target, params, env);
                    } finally {
                        if (topLevel) {
                            LazyCellExecutor.reset();
                            ProjectData.removeCurrentInstance();
                        }
                    }
                }
            };
        }

        return super.decorateForMultimoduleDispatching(openMethod);
    }

}
