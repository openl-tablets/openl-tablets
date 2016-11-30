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
import org.openl.rules.table.Point;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetWrapper extends Spreadsheet implements IOpenMethodWrapper{
    Spreadsheet delegate;
    XlsModuleOpenClass xlsModuleOpenClass;
    
    public SpreadsheetWrapper(XlsModuleOpenClass xlsModuleOpenClass, Spreadsheet delegate) {
        super();
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }
    
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(xlsModuleOpenClass, this, target, params, env);
    }
    
    @Override
    public IOpenMethod getDelegate() {
        return delegate;
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

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public Constructor<?> getResultConstructor() throws SecurityException, NoSuchMethodException {
        return delegate.getResultConstructor();
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

    public int height() {
        return delegate.height();
    }

    public int width() {
        return delegate.width();
    }

    public void setInvoker(SpreadsheetInvoker invoker) {
        delegate.setInvoker(invoker);
    }

    public Map<String, Point> getFieldsCoordinates() {
        return delegate.getFieldsCoordinates();
    }
    
    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }
    
    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

}
