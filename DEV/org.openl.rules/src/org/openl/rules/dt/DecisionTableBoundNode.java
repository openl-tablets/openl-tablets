package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.binding.RulesModuleBindingContextHelper;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.types.IOpenMethodHeader;

/**
 * @author snshor
 */
public class DecisionTableBoundNode extends AMethodBasedNode {

    private final IBindingContext bindingContext;
    private boolean initialized;

    public DecisionTableBoundNode(TableSyntaxNode tableSyntaxNode,
                                  OpenL openl,
                                  IOpenMethodHeader header,
                                  ModuleOpenClass module,
                                  IBindingContext bindingContext) {

        super(tableSyntaxNode, openl, header, module);
        this.bindingContext = bindingContext;
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        var type = getType();
        var dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        var isTypeCustomSpreadsheetResult = type instanceof SpreadsheetResultOpenClass;
        var decisionTable = new DecisionTable(getHeader(), this, isTypeCustomSpreadsheetResult);
        if (decisionTable.isTypeCustomSpreadsheetResult()) {
            decisionTable.setDim(dim);
            decisionTable.setCustomSpreadsheetResultType(
                    (CustomSpreadsheetResultOpenClass) bindingContext.findType(
                            Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + decisionTable.getName()));
        }
        return decisionTable;
    }

    /**
     * Binds the body of a table that returns a spreadsheet result built from the results of its rules, before the
     * tables calling it are bound. The module has the table and every other table of its type by then, so its rules
     * can call them.
     *
     * <p>The type takes the steps of the results later, from {@link #completeCustomSpreadsheetResultType()}, so
     * that every table of the type is bound against the same steps. An error is reported on the table. The body of
     * any other table is bound with the module.
     */
    public void bindCustomSpreadsheetResultBody() {
        reportingErrors(() -> bindBody(bindingContext));
    }

    /**
     * Adds the steps of the results of the table to the spreadsheet result type it returns. An error is reported on
     * the table.
     */
    public void completeCustomSpreadsheetResultType() {
        reportingErrors(this::applyDeferredChanges);
    }

    /** Runs a step of binding, reporting its error on the table. */
    private void reportingErrors(DecisionTableLoader.Supplier step) {
        try {
            step.get();
        } catch (Exception | LinkageError e) {
            BindHelper.processError(e, getTableSyntaxNode(), bindingContext);
        }
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        super.finalizeBind(bindingContext);
        try {
            bindBody(bindingContext);
        } finally {
            applyDeferredChanges();
        }
    }

    private void applyDeferredChanges() {
        var decisionTable = getDecisionTable();
        if (decisionTable != null) {
            decisionTable.applyDeferredChanges();
        }
    }

    /** Binds the body of the table once, leaving the spreadsheet result type it returns as it is. */
    private void bindBody(IBindingContext context) throws Exception {
        var decisionTable = getDecisionTable();
        if (!initialized && decisionTable != null) {
            initialized = true;
            RulesModuleBindingContextHelper.compileAllTypesInSignature(getHeader().getSignature(), context);
            new DecisionTableLoader()
                    .loadAndBind(getTableSyntaxNode(), decisionTable, getOpenl(), getModule(), context);
        }
    }

    public final DecisionTable getDecisionTable() {
        return (DecisionTable) getMethod();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        getDecisionTable().updateDependency(dependencies);
    }
}
