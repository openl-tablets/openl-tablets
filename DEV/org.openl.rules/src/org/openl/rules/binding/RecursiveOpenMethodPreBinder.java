package org.openl.rules.binding;

import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.OpenMethodHeader;

public interface RecursiveOpenMethodPreBinder extends IOpenMethod {

    OpenMethodHeader getHeader();

    TableSyntaxNode getTableSyntaxNode();

    /** The spreadsheet result type the table builds, none for a table that returns no spreadsheet result. */
    @Nullable
    CustomSpreadsheetResultOpenClass getCustomSpreadsheetResultOpenClass();

    boolean isSpreadsheetWithCustomSpreadsheetResult();

    void preBind();

    /**
     * Binds the body that builds the spreadsheet result type of the table. It runs once the table and every other
     * table of its type are in the module, so the body can call the table itself.
     *
     * <p>The type does not take the steps of the body yet, so every table of the type sees the same steps.
     */
    void bindBody();

    /** Adds the steps of the body to the spreadsheet result type of the table, once all bodies of the type are bound. */
    void completeType();

    void startPreBind();

    void finishPreBind();

    boolean isPreBindStarted();

    boolean isCompleted();

}
