package org.openl.extension.xmlrules;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.conf.IUserContext;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.OpenLSystemProperties;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.data.IDataBase;
import org.openl.rules.dt2.DecisionTable;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.binding.wrapper.DecisionTable2Wrapper;
import org.openl.rules.lang.xls.binding.wrapper.TableMethodWrapper;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

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
        return new XlsModuleOpenClass(null,
                XlsHelper.getModuleName(moduleNode),
                new XlsMetaInfo(moduleNode),
                openl,
                dbase,
                moduleDependencies,
                OpenLSystemProperties.isDTDispatchingMode(bindingContext.getExternalParams()),
                OpenLSystemProperties.isDispatchingValidationEnabled(bindingContext.getExternalParams())) {
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
                            }
                            try {
                                return super.invoke(target, params, env);
                            } finally {
                                if (topLevel) {
                                    LazyCellExecutor.reset();
                                }
                            }
                        }
                    };
                }


                if (openMethod instanceof DecisionTable) {
                    return new DecisionTable2Wrapper(xlsModuleOpenClass, (DecisionTable) openMethod) {
                        @Override
                        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
                            LazyCellExecutor cache = LazyCellExecutor.getInstance();
                            boolean topLevel = cache == null;
                            if (topLevel) {
                                cache = new LazyCellExecutor(xlsModuleOpenClass, target, env);
                                LazyCellExecutor.setInstance(cache);
                            }
                            try {
                                return super.invoke(target, params, env);
                            } finally {
                                if (topLevel) {
                                    LazyCellExecutor.reset();
                                }
                            }
                        }
                    };
                }
                return super.decorateForMultimoduleDispatching(openMethod);
            }

        };
    }
}
