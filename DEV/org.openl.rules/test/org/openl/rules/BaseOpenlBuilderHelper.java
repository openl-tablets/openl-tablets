package org.openl.rules;

import static org.junit.Assert.assertNotNull;

import java.util.Map.Entry;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.openl.CompiledOpenClass;
import org.openl.conf.UserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.EngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

/**
 * Helper class for building IOpenClass and getting XlsModuleSyntaxNode from it.
 * To get everything you need for your tests just extend this class.
 * 
 * 
 * @author DLiauchuk
 *
 */
public abstract class BaseOpenlBuilderHelper {

    private XlsModuleSyntaxNode xsn;
    private CompiledOpenClass compiledOpenClass;
    private EngineFactory<Object> engineFactory;
    private IDependencyManager dependencyManager;
    private boolean executionMode = false;

    public BaseOpenlBuilderHelper() {

    }

    public BaseOpenlBuilderHelper(String src) {
        build(src);
    }

    public BaseOpenlBuilderHelper(String src, IDependencyManager dependencyManager) {
        this(src, dependencyManager, false);
    }

    public BaseOpenlBuilderHelper(String src, boolean executionMode) {
        this(src, null, executionMode);
    }

    public BaseOpenlBuilderHelper(String src, IDependencyManager dependencyManager, boolean executionMode) {
        this.dependencyManager = dependencyManager;
        this.executionMode = executionMode;
        build(src);
    }

    public void build(String sourceFile) {
        buildEngineFactory(sourceFile);
        buildCompiledOpenClass();
        if (!executionMode) {
            XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
            xsn = xmi.getXlsModuleNode();
        }
    }

    protected EngineFactory<Object> buildEngineFactory(String sourceFile) {
        if (engineFactory == null) {
            engineFactory = new RulesEngineFactory<Object>(sourceFile);
            engineFactory.setDependencyManager(dependencyManager);
            engineFactory.setExecutionMode(executionMode);
        }
        return engineFactory;
    }

    public EngineFactory<Object> getEngineFactory() {
        return engineFactory;
    }

    public Object newInstance() {
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        return getCompiledOpenClass().getOpenClass().newInstance(env);
    }

    protected CompiledOpenClass buildCompiledOpenClass() {
        if (compiledOpenClass == null) {
            compiledOpenClass = getEngineFactory().getCompiledOpenClass();
        }
        return compiledOpenClass;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
    }

    public Class<?> getClass(String name) throws ClassNotFoundException {
        Class<?> clazz = getCompiledOpenClass().getClassLoader().loadClass(name);
        assertNotNull(clazz);
        return clazz;
    }

    @Deprecated
    protected TableSyntaxNode findTable(String tableName, TableSyntaxNode[] tsns) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : tsns) {
            if (tableName.equals(tsn.getDisplayName())) {
                result = tsn;
            }
        }
        return result;
    }

    protected TableSyntaxNode findTable(String tableName) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            if (tableName.equals(tsn.getDisplayName())) {
                result = tsn;
            }
        }
        return result;
    }

    protected TableSyntaxNode findTable(String tableName, ITableProperties properties) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            if (tableName.equals(tsn.getDisplayName())) {
                EqualsBuilder equalsBuilder = new EqualsBuilder();
                for (Entry<String, Object> property : properties.getAllProperties().entrySet()) {
                    equalsBuilder.append(property.getValue(),
                        tsn.getTableProperties().getPropertyValue(property.getKey()));
                }
                if (equalsBuilder.isEquals()) {
                    result = tsn;
                }
            }
        }
        return result;
    }

    protected TableSyntaxNode findDispatcherForMethod(String methodName) {
        IOpenClass moduleOpenClass = getCompiledOpenClass().getOpenClass();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method.getInfo() != null && method.getInfo().getSyntaxNode() instanceof TableSyntaxNode) {
                TableSyntaxNode tsn = (TableSyntaxNode) method.getInfo().getSyntaxNode();
                if (DispatcherTablesBuilder.isDispatcherTable(tsn) && method.getName().endsWith(methodName)) {
                    return tsn;
                }
            }
        }
        return null;
    }

    protected TableSyntaxNode[] getTableSyntaxNodes() {
        TableSyntaxNode[] tsns = xsn.getXlsTableSyntaxNodes();
        return tsns;
    }

    protected XlsModuleSyntaxNode getModuleSuntaxNode() {
        return xsn;
    }

    protected Object invokeMethod(IOpenMethod testMethod, Object[] paramValues) {
        org.openl.vm.IRuntimeEnv environment = new SimpleRulesVM().getRuntimeEnv();
        Object myInstance = getCompiledOpenClass().getOpenClassWithErrors().newInstance(environment);

        return testMethod.invoke(myInstance, paramValues, environment);
    }

    protected Object invokeMethod(String methodName) {
        return invokeMethod(methodName, new IOpenClass[] {}, new Object[0]);
    }

    protected Object invokeMethod(String methodName, IOpenClass[] params, Object[] paramValues) {
        IOpenMethod testMethod = getMethod(methodName, params);

        Assert.assertNotNull(String.format("Method with name \"%s\" does not exists", methodName), testMethod);

        return invokeMethod(testMethod, paramValues);
    }

    protected IOpenMethod getMethod(String methodName, IOpenClass[] params) {
        IOpenClass clazz = getCompiledOpenClass().getOpenClassWithErrors();
        return clazz.getMatchingMethod(methodName, params);
    }

    protected IOpenField getField(String fieldName) {
        return getCompiledOpenClass().getOpenClassWithErrors().getField(fieldName);
    }

    protected Object getFieldValue(String fieldName) {
        IOpenField field = getField(fieldName);
        org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
        Object myInstance = getCompiledOpenClass().getOpenClassWithErrors().newInstance(environment);
        return field.get(myInstance, environment);
    }
}
