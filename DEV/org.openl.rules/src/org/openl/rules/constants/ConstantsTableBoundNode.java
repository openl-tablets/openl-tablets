package org.openl.rules.constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.gen.writers.DefaultValue;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.ConstantsTableMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.util.ParserUtils;
import org.openl.util.TableNameChecker;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.FieldMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.util.StringUtils;

public class ConstantsTableBoundNode implements IMemberBoundNode {

    private final TableSyntaxNode tableSyntaxNode;
    private final ModuleOpenClass moduleOpenClass;
    private final ILogicalTable table;
    private ILogicalTable normalizedData;
    private final OpenL openl;
    private Collection<ConstantOpenField> constantOpenFields = new ArrayList<>();

    ConstantsTableBoundNode(TableSyntaxNode syntaxNode,
            XlsModuleOpenClass moduleOpenClass,
            ILogicalTable table,
            OpenL openl) {
        this.tableSyntaxNode = syntaxNode;
        this.moduleOpenClass = moduleOpenClass;
        this.table = table;
        this.openl = openl;
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    public ModuleOpenClass getModuleOpenClass() {
        return moduleOpenClass;
    }

    private void processRow(ILogicalTable row, IBindingContext cxt) {

        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);

        if (ParserUtils.isBlankOrCommented(rowSrc.getCode())) {
            return;
        }
        if (row.getWidth() < 2) {
            String errorMessage = "Bad table structure: expected {header} / {type | name}.";
            BindHelper.processError(errorMessage, rowSrc, cxt);
            return;
        }

        GridCellSourceCodeModule typeCellSource = DatatypeTableBoundNode.getCellSource(row, cxt, 0);
        String typeName = typeCellSource.getCode();
        RuleRowHelper.getType(typeName, typeCellSource, cxt);
        IOpenClass constantType = RuleRowHelper.getType(typeName, typeCellSource, cxt);

        GridCellSourceCodeModule nameCellSource = DatatypeTableBoundNode.getCellSource(row, cxt, 1);
        String constantName = nameCellSource.getCode();
        if (TableNameChecker.isInvalidJavaIdentifier(constantName)) {
            String errorMessage = String.format("Bad constant name: %s", constantName);
            BindHelper.processError(errorMessage, nameCellSource, cxt);
        }

        String value = null;
        Object objectValue = null;
        if (row.getWidth() > 2) {
            GridCellSourceCodeModule defaultValueSrc = DatatypeTableBoundNode.getCellSource(row, cxt, 2);
            value = defaultValueSrc.getCode();
            if (ParserUtils.isCommented(value) || StringUtils.isBlank(value)) {
                value = null;
            }

            if (DefaultValue.DEFAULT.equals(value)) {
                objectValue = constantType.newInstance(openl.getVm().getRuntimeEnv());
            } else if (RuleRowHelper.isFormula(value)) {
                SubTextSourceCodeModule source = new SubTextSourceCodeModule(defaultValueSrc, 1);
                OpenMethodHeader methodHeader = new OpenMethodHeader(constantName,
                        constantType,
                        new MethodSignature(),
                        null);
                try {
                    boolean noErrors;
                    CompositeMethod compositeMethod;
                    cxt.pushErrors();
                    // cxt.pushMessages();
                    try {
                        compositeMethod = OpenLManager.makeMethod(openl, source, methodHeader, cxt);
                    } finally {

                        // cxt.popMessages();
                        List<SyntaxNodeException> syntaxNodeExceptions = cxt.popErrors();
                        noErrors = syntaxNodeExceptions.isEmpty();
                        syntaxNodeExceptions.forEach(cxt::addError);
                    }
                    if (noErrors) {
                        objectValue = compositeMethod.invoke(null, IBoundNode.EMPTY_RESULT, openl.getVm().getRuntimeEnv());
                    } else {
                        objectValue = null;
                    }
                } catch (Exception ex) {
                    BindHelper.processError(ex, defaultValueSrc, cxt);
                    objectValue = null;
                }
            } else if (String.class == constantType.getInstanceClass()) {
                objectValue = value;
            } else if (value == null) {
                objectValue = null;
            } else if (constantType.getName().startsWith("[[")) {
                BindHelper.processError("Multi-dimensional arrays are not supported.", defaultValueSrc, cxt);
                objectValue = null;
            } else {
                try {
                    objectValue = RuleRowHelper.loadNativeValue(row.getColumn(2).getCell(0, 0), constantType);
                    if (objectValue == null) {
                        objectValue = String2DataConvertorFactory.parse(constantType.getInstanceClass(), value, cxt);
                    }
                } catch (RuntimeException e) {
                String message = String.format("Cannot parse cell value '%s'.", value);
                    BindHelper.processError(message, e, defaultValueSrc, cxt);
                    objectValue = null;
                }
            }
        }

        try {
            FieldMetaInfo fieldMetaInfo = new FieldMetaInfo(constantType.getName(),
                constantName,
                tableSyntaxNode,
                tableSyntaxNode.getUri());

            ConstantOpenField constantField = new ConstantOpenField(constantName,
                objectValue,
                value,
                constantType,
                moduleOpenClass,
                fieldMetaInfo);

            moduleOpenClass.addField(constantField);

            constantOpenFields.add(constantField);
        } catch (Exception t) {
            BindHelper.processError(t, rowSrc, cxt);
        }
    }

    private void addConstants(final IBindingContext bindingContext) {
        final ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, bindingContext);
        normalizedData = dataTable;

        int tableHeight = 0;
        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }

        for (int i = 0; i < tableHeight; i++) {
            processRow(dataTable.getRow(i), bindingContext);
        }
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) {
        if (!bindingContext.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new ConstantsTableMetaInfoReader(this));
        }

        addConstants(bindingContext);

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();
        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
    }

    @Override
    public void removeDebugInformation(IBindingContext bindingContext) {
        if (bindingContext.isExecutionMode()) {
            for (ConstantOpenField constantOpenField : constantOpenFields) {
                constantOpenField.setMemberMetaInfo(null);
            }
            constantOpenFields = null;
        }
    }

    public ILogicalTable getNormalizedData() {
        return normalizedData;
    }

    public Collection<ConstantOpenField> getConstantOpenFields() {
        return constantOpenFields;
    }
}
