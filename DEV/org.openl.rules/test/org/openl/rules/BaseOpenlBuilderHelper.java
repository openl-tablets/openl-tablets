package org.openl.rules;

import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
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

    public BaseOpenlBuilderHelper(String src) {
        EngineFactory<Object> engineFactory = new RulesEngineFactory<>(src);
        engineFactory.setExecutionMode(false);
        compiledOpenClass = engineFactory.getCompiledOpenClass();
        XlsMetaInfo xmi = (XlsMetaInfo) compiledOpenClass.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();
    }

    public Object newInstance() {
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        return getCompiledOpenClass().getOpenClass().newInstance(env);
    }

    public CompiledOpenClass getCompiledOpenClass() {
        return compiledOpenClass;
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
        return xsn.getXlsTableSyntaxNodes();
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
        return clazz.getMethod(methodName, params);
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
