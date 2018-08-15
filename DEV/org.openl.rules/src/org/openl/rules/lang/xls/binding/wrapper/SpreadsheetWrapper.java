package org.openl.rules.lang.xls.binding.wrapper;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
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

public class SpreadsheetWrapper extends Spreadsheet implements IOpenMethodWrapper {
    Spreadsheet delegate;
    XlsModuleOpenClass xlsModuleOpenClass;

    public SpreadsheetWrapper(XlsModuleOpenClass xlsModuleOpenClass, Spreadsheet delegate) {
        super();
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(this, target, params, env);
    }

    @Override
    public XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClass;
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public Constructor<?> getResultConstructor() throws SecurityException, NoSuchMethodException {
        return delegate.getResultConstructor();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public boolean isCustomSpreadsheetType() {
        return delegate.isCustomSpreadsheetType();
    }

    @Override
    public SpreadsheetCell[][] getCells() {
        return delegate.getCells();
    }

    @Override
    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public IResultBuilder getResultBuilder() {
        return delegate.getResultBuilder();
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    @Override
    public SpreadsheetOpenClass getSpreadsheetType() {
        return delegate.getSpreadsheetType();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public void setCells(SpreadsheetCell[][] cells) {
        delegate.setCells(cells);
    }

    @Override
    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    @Override
    public void setColumnNames(String[] colNames) {
        delegate.setColumnNames(colNames);
    }

    @Override
    public ATableBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    @Override
    public void setResultBuilder(IResultBuilder resultBuilder) {
        delegate.setResultBuilder(resultBuilder);
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public void setRowNames(String[] rowNames) {
        delegate.setRowNames(rowNames);
    }

    @Override
    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        delegate.setSpreadsheetType(spreadsheetType);
    }

    @Override
    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public String[] getRowNames() {
        return delegate.getRowNames();
    }

    @Override
    public String[] getColumnNames() {
        return delegate.getColumnNames();
    }

    @Override
    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    @Override
    public List<SpreadsheetCell> listNonEmptyCells(SpreadsheetHeaderDefinition definition) {
        return delegate.listNonEmptyCells(definition);
    }

    @Override
    public int height() {
        return delegate.height();
    }

    @Override
    public int width() {
        return delegate.width();
    }

    @Override
    public void setInvoker(SpreadsheetInvoker invoker) {
        delegate.setInvoker(invoker);
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

}
