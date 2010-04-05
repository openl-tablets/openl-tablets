/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 */
public class DuplicatedTableException extends OpenLCompilationException {

    private static final long serialVersionUID = -6269440215951548170L;

    private TableSyntaxNode existingTable;
    private TableSyntaxNode duplicatedTable;

    public DuplicatedTableException(String tableName, TableSyntaxNode existingTable, TableSyntaxNode duplicatedTable) {

        super("The table already exists: " + tableName, null, null, duplicatedTable.getModule());

        this.existingTable = existingTable;
        this.duplicatedTable = duplicatedTable;
    }

    public TableSyntaxNode getDuplicatedTable() {
        return duplicatedTable;
    }

    public TableSyntaxNode getExistingTable() {
        return existingTable;
    }

}
