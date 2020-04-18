package org.openl.rules.lang.xls.binding.wrapper.base;

import java.util.Map;
import java.util.Objects;

import org.openl.binding.BindingDependencies;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.calc.SpreadsheetInvoker;
import org.openl.rules.calc.SpreadsheetOpenClass;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.Point;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

public class AbstractSpreadsheetWrapper extends Spreadsheet {
    static {
        WrapperValidation.validateWrapperClass(AbstractSpreadsheetWrapper.class,
            AbstractSpreadsheetWrapper.class.getSuperclass());
    }

    protected final Spreadsheet delegate;

    public AbstractSpreadsheetWrapper(Spreadsheet delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public void setCustomSpreadsheetResultType(CustomSpreadsheetResultOpenClass spreadsheetCustomResultType) {
        delegate.setCustomSpreadsheetResultType(spreadsheetCustomResultType);
    }

    @Override
    public boolean isCustomSpreadsheet() {
        return delegate.isCustomSpreadsheet();
    }

    @Override
    public SpreadsheetBoundNode getBoundNode() {
        return delegate.getBoundNode();
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
    public void setColumnNames(String[] colNames) {
        delegate.setColumnNames(colNames);
    }

    @Override
    public void setResultBuilder(IResultBuilder resultBuilder) {
        delegate.setResultBuilder(resultBuilder);
    }

    @Override
    public void setRowNames(String[] rowNames) {
        delegate.setRowNames(rowNames);
    }

    @Override
    public void setRowTitles(String[] rowTitles) {
        delegate.setRowTitles(rowTitles);
    }

    @Override
    public String[] getRowNamesForResultModel() {
        return delegate.getRowNamesForResultModel();
    }

    @Override
    public void setRowNamesForResultModel(String[] rowNamesForResultModel) {
        delegate.setRowNamesForResultModel(rowNamesForResultModel);
    }

    @Override
    public String[] getColumnNamesForResultModel() {
        return delegate.getColumnNamesForResultModel();
    }

    @Override
    public void setColumnNamesForResultModel(String[] columnNamesForResultModel) {
        delegate.setColumnNamesForResultModel(columnNamesForResultModel);
    }

    @Override
    public String[] getRowTitles() {
        return delegate.getRowTitles();
    }

    @Override
    public void setColumnTitles(String[] columnTitles) {
        delegate.setColumnTitles(columnTitles);
    }

    @Override
    public String[] getColumnTitles() {
        return delegate.getColumnTitles();
    }

    @Override
    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        delegate.setSpreadsheetType(spreadsheetType);
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
    public boolean isDetailedPlainModel() {
        return delegate.isDetailedPlainModel();
    }

    @Override
    public void setDetailedPlainModel(boolean detailedPlainModel) {
        delegate.setDetailedPlainModel(detailedPlainModel);
    }

    @Override
    public void setInvoker(SpreadsheetInvoker invoker) {
        delegate.setInvoker(invoker);
    }

    @Override
    public Map<String, Point> getFieldsCoordinates() {
        return delegate.getFieldsCoordinates();
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegate.invoke(target, params, env);
    }

    @Override
    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
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
    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    @Override
    public boolean isConstructor() {
        return delegate.isConstructor();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
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
        return this;
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractSpreadsheetWrapper that = (AbstractSpreadsheetWrapper) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}