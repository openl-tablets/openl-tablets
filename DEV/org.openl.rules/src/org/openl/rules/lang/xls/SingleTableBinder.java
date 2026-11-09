package org.openl.rules.lang.xls;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.binding.IMemberBoundNode;
import org.openl.rules.data.DataTableBoundNode;
import org.openl.rules.fuzzy.OpenLFuzzyUtils;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.PropertiesLoader;

/**
 * Binds one table against a module compiled before, to read what the compiler makes of the table as it stands now.
 *
 * <p>Only the table is bound. The tables it calls are found as the module compiled them, and no other table is
 * compiled again: neither those it uses nor those using it. The table is not added to the module, and the module is
 * left as it was, so what runs on it sees no change.
 *
 * <p>The table takes the properties it declares and inherits, its header, and its body: the conditions and actions of
 * a decision table, the steps of a Spreadsheet. A Data, a Test or a Run table takes its columns and their types; its
 * rows are not converted.
 *
 * <p>A table whose binder writes what it defines straight into the module is not bound
 * ({@link org.openl.rules.lang.xls.binding.AXlsTableBinder#bindsAlone()}): a Datatype, whose fields are read along
 * with the class generated for it, and the Conditions, Actions, Returns and Constants tables, which add their
 * definitions and constants to the module as they are bound.
 *
 * <p>A table the compiler cannot read is bound as far as it gets, as in a compile of the module: an error leaves the
 * parts already read.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SingleTableBinder {

    /**
     * Binds a table against a compiled module.
     *
     * @param table  the table, parsed from its sheet and not bound yet
     * @param module the module the table belongs to, compiled with its binding context kept
     * @return the table bound as far as the compiler reads it, or {@code null} when it cannot be bound alone: a module
     *         compiled without its binding context, a table whose binder writes into the module, or a table of no type
     *         a binder binds
     */
    public static @Nullable TableSyntaxNode bind(TableSyntaxNode table, XlsModuleOpenClass module) {
        var compiled = module.getRulesModuleBindingContext();
        var binder = XlsBinder.binderOf(table.getType());
        if (compiled == null || binder == null || !binder.bindsAlone()) {
            return null;
        }
        var context = new SandboxBindingContext(compiled, module);
        var holder = XlsModuleOpenClassHolder.getInstance();
        var previous = holder.getXlsModuleOpenClass();
        holder.setXlsModuleOpenClass(module);
        var combinedTypesBack = XlsModuleOpenClass.keepCombinedTypesApart();
        try {
            loadProperties(table, context, module);
            var node = binder.preBind(table, context.getOpenL(), context, module);
            if (node != null) {
                finalizeBind(node, context);
            }
        } catch (Exception | LinkageError e) {
            log.debug("The table {} is bound in part.", table.getUri(), e);
        } finally {
            combinedTypesBack.run();
            holder.setXlsModuleOpenClass(previous);
            OpenLFuzzyUtils.clearCaches();
        }
        return table;
    }

    /** The properties of the table: an error leaves it the properties it inherits, as a compile of the module does. */
    private static void loadProperties(TableSyntaxNode table, SandboxBindingContext context,
                                       XlsModuleOpenClass module) {
        try {
            new PropertiesLoader(context.getOpenL(), context, module).loadProperties(table);
        } catch (Exception | LinkageError e) {
            log.debug("The properties of the table {} are read in part.", table.getUri(), e);
        }
    }

    /**
     * Binds the body of a table. A table of a method takes its method first, which the module never receives. The rows
     * of a Data, a Test or a Run table are left unread.
     */
    private static void finalizeBind(IMemberBoundNode node, SandboxBindingContext context) throws Exception {
        if (node instanceof DataTableBoundNode) {
            return;
        }
        if (node instanceof AMethodBasedNode method) {
            method.createMethod();
        }
        node.finalizeBind(context);
    }
}
