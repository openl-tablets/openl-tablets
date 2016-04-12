package org.openl.extension.xmlrules.binding.wrapper;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.table.MethodTableBoundNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.*;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

class XmlRulesTableMethodDelegator extends TableMethod implements XmlRulesMethodDelegator {
    private TableMethod delegate;

    public XmlRulesTableMethodDelegator(TableMethod delegate) {
        this.delegate = delegate;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegate.invoke(target, params, env);
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

    public String getTableUri() {
        return delegate.getTableUri();
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
