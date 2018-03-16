package org.openl.extension.xmlrules.binding.wrapper;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.extension.xmlrules.XmlRulesPath;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.table.MethodTableBoundNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.*;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

// This decorator must not implement IOpenWrapper
public class XmlRulesTableMethodDecorator extends TableMethod {
    private final TableMethod delegate;
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ProjectData projectData;
    private final ArgumentsConverter argumentsConverter;

    private final XmlRulesPath functionPath;

    public XmlRulesTableMethodDecorator(XlsModuleOpenClass xlsModuleOpenClass,
            TableMethod delegate,
            ProjectData projectData) {
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.projectData = projectData;
        argumentsConverter = new ArgumentsConverter(delegate.getMethod());
        functionPath = projectData.getPath(delegate.getName());
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
        cache.pushCurrentPath(functionPath);
        try {
            params = argumentsConverter.convert(params);
            return delegate.invoke(target, params, env);
        } finally {
            cache.popCurrentPath();
            if (topLevel) {
                LazyCellExecutor.reset();
                ProjectData.removeCurrentInstance();
            }
        }
    }

    public String toString() {
        return delegate.toString();
    }

    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    public String getUri() {
        return delegate.getUri();
    }

    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    public String getName() {
        return delegate.getName();
    }

    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public MethodTableBoundNode getMethodTableBoundNode() {
        return delegate.getMethodTableBoundNode();
    }

    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    public CompositeMethod getCompositeMethod() {
        return delegate.getCompositeMethod();
    }

    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    public ATableBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }
}
