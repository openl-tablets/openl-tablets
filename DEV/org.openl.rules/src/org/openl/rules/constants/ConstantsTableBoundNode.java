package org.openl.rules.constants;

import java.util.ArrayList;
import java.util.Collection;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
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
import org.openl.rules.utils.ParserUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionCollector;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.FieldMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.util.MessageUtils;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

public class ConstantsTableBoundNode implements IMemberBoundNode {

    private TableSyntaxNode tableSyntaxNode;
    private ModuleOpenClass moduleOpenClass;
    private ILogicalTable table;
    private ILogicalTable normalizedData;
    private OpenL openl;
    private Collection<ConstantOpenField> constantOpenFields = new ArrayList<>();

    public ConstantsTableBoundNode(TableSyntaxNode syntaxNode,
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

    /**
     * Checks if the given row can be processed.
     *
     * @param rowSrc checked row
     * @return false if row content is empty, or was commented with special symbols.
     */
    public static boolean canProcessRow(GridCellSourceCodeModule rowSrc) {
        return !ParserUtils.isBlankOrCommented(rowSrc.getCode());
    }

    private String getName(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        GridCellSourceCodeModule nameCellSource = DatatypeTableBoundNode.getCellSource(row, cxt, 1);
        IdentifierNode[] idn = DatatypeTableBoundNode.getIdentifierNode(nameCellSource);
        if (idn.length != 1) {
            String errorMessage = String.format("Bad constant name: %s", nameCellSource.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, nameCellSource);
        } else {
            return idn[0].getIdentifier();
        }
    }

    private IOpenClass getConstantType(IBindingContext bindingContext,
            ILogicalTable row,
            GridCellSourceCodeModule tableSrc) throws SyntaxNodeException {

        IOpenClass fieldType = OpenLManager.makeType(openl, tableSrc, bindingContext);

        if (fieldType == null || fieldType instanceof NullOpenClass) {
            throw SyntaxNodeExceptionUtils
                .createError(MessageUtils.getTypeNotFoundMessage(tableSrc.getCode()), null, null, tableSrc);
        }

        if (row.getWidth() < 2) {
            String errorMessage = "Bad table structure: must be {header} / {type | name}";
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, tableSrc);
        }
        return fieldType;
    }

    private void processRow(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {

        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);

        if (canProcessRow(rowSrc)) {
            String constantName = getName(row, cxt);

            IOpenClass constantType = getConstantType(cxt, row, rowSrc);

            Object objectValue;
            String defaultValue = null;
            GridCellSourceCodeModule defaultValueSrc = DatatypeTableBoundNode.getCellSource(row, cxt, 2);
            if (!ParserUtils.isCommented(defaultValueSrc.getCode())) {
                IdentifierNode[] idn = DatatypeTableBoundNode.getIdentifierNode(defaultValueSrc);
                if (idn.length > 0) {
                    // if there is any valid identifier, consider it is a default
                    // value
                    //
                    defaultValue = defaultValueSrc.getCode();
                }
            }
            String value = defaultValue;
            if (DefaultValue.DEFAULT.equals(value)) {
                objectValue = constantType.newInstance(openl.getVm().getRuntimeEnv());
            } else if (RuleRowHelper.isFormula(value)) {
                objectValue = OpenLManager.run(openl, new SubTextSourceCodeModule(defaultValueSrc, 1));
            } else if (String.class == constantType.getInstanceClass()) {
                objectValue = value;
            } else if (value == null) {
                objectValue = null;
            } else if (constantType.getName().startsWith("[[")) {
                throw SyntaxNodeExceptionUtils.createError("Multi-dimensional arrays are not supported.",
                    defaultValueSrc);
            } else {
                try {
                    objectValue = RuleRowHelper.loadNativeValue(row.getColumn(2).getCell(0, 0), constantType);
                    if (objectValue == null) {
                        objectValue = String2DataConvertorFactory.parse(constantType.getInstanceClass(), value, cxt);
                    }
                } catch (RuntimeException e) {
                    String message = String.format("Cannot parse cell value '%s'", value);
                    IOpenSourceCodeModule cellSourceCodeModule = DatatypeTableBoundNode.getCellSource(row, cxt, 2);

                    if (e instanceof CompositeSyntaxNodeException) {
                        CompositeSyntaxNodeException exception = (CompositeSyntaxNodeException) e;
                        if (exception.getErrors() != null && exception.getErrors().length == 1) {
                            SyntaxNodeException syntaxNodeException = exception.getErrors()[0];
                            throw SyntaxNodeExceptionUtils
                                .createError(message, null, syntaxNodeException.getLocation(), cellSourceCodeModule);
                        }
                        throw SyntaxNodeExceptionUtils.createError(message, cellSourceCodeModule);
                    } else {
                        TextInterval location = value == null ? null : LocationUtils.createTextInterval(value);
                        throw SyntaxNodeExceptionUtils.createError(message, e, location, cellSourceCodeModule);
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
                throw SyntaxNodeExceptionUtils
                    .createError(t.getMessage(), t, null, DatatypeTableBoundNode.getCellSource(row, cxt, 1));
            }
        }
    }

    private void addConstants(final IBindingContext cxt) throws Exception {
        final ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, cxt);
        normalizedData = dataTable;

        int tableHeight = 0;
        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }

        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        for (int i = 0; i < tableHeight; i++) {
            final int index = i;
            syntaxNodeExceptionCollector.run(() -> processRow(dataTable.getRow(index), cxt));
        }
        syntaxNodeExceptionCollector.throwIfAny();
    }

    @Override
    public void finalizeBind(IBindingContext cxt) throws Exception {
        if (!cxt.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new ConstantsTableMetaInfoReader(this));
        }

        addConstants(cxt);

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();
        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) {
        if (cxt.isExecutionMode()) {
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
