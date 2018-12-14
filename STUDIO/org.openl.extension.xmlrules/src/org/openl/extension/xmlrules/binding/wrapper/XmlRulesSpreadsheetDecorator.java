package org.openl.extension.xmlrules.binding.wrapper;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.XmlRulesPath;
import org.openl.extension.xmlrules.utils.LazyCellExecutor;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetHeaderDefinition;
import org.openl.rules.calc.SpreadsheetInvoker;
import org.openl.rules.calc.SpreadsheetOpenClass;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

// This decorator must not implement IOpenWrapper
public class XmlRulesSpreadsheetDecorator extends Spreadsheet {
    private final Spreadsheet delegate;
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ProjectData projectData;
    private final XmlRulesPath functionPath;

    public XmlRulesSpreadsheetDecorator(XlsModuleOpenClass xlsModuleOpenClass,
            Spreadsheet delegate,
            ProjectData projectData) {
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.projectData = projectData;

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

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public IOpenClass getType() {
        return delegate.getType();
    }

    public boolean isCustomSpreadsheetType() {
        return delegate.isCustomSpreadsheetType();
    }

    public SpreadsheetCell[][] getCells() {
        return delegate.getCells();
    }

    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    public IResultBuilder getResultBuilder() {
        return delegate.getResultBuilder();
    }

    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    public SpreadsheetOpenClass getSpreadsheetType() {
        return delegate.getSpreadsheetType();
    }

    public int getHeight() {
        return delegate.getHeight();
    }

    public void setCells(SpreadsheetCell[][] cells) {
        delegate.setCells(cells);
    }

    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    public void setColumnNames(String[] colNames) {
        delegate.setColumnNames(colNames);
    }

    public ATableBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    public void setResultBuilder(IResultBuilder resultBuilder) {
        delegate.setResultBuilder(resultBuilder);
    }

    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    public void setRowNames(String[] rowNames) {
        delegate.setRowNames(rowNames);
    }

    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        delegate.setSpreadsheetType(spreadsheetType);
    }

    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    public int getWidth() {
        return delegate.getWidth();
    }

    public String[] getRowNames() {
        return delegate.getRowNames();
    }

    public String[] getColumnNames() {
        return delegate.getColumnNames();
    }

    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    public List<SpreadsheetCell> listNonEmptyCells(SpreadsheetHeaderDefinition definition) {
        return delegate.listNonEmptyCells(definition);
    }

    @Deprecated
    public int height() {
        return delegate.height();
    }

    @Deprecated
    public int width() {
        return delegate.width();
    }

    public void setInvoker(SpreadsheetInvoker invoker) {
        delegate.setInvoker(invoker);
    }
}
