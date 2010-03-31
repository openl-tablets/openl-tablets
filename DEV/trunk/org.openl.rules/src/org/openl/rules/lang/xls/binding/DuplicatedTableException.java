/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;


/**
 * @author snshor
 * TODO - make it syntax error
 *
 */
public class DuplicatedTableException extends RuntimeException{

    private static final long serialVersionUID = -6269440215951548170L;

    private TableSyntaxNode existingTable;
    private TableSyntaxNode duplicatedTable;
    private String tableName;

    public DuplicatedTableException(String tableName, TableSyntaxNode existingTable, TableSyntaxNode duplicatedTable) {
        this.tableName = tableName;
        this.existingTable = existingTable;
        this.duplicatedTable = duplicatedTable;
    }

    public TableSyntaxNode getDuplicatedTable() {
        return duplicatedTable;
    }

    public TableSyntaxNode getExistingTable() {
        return existingTable;
    }

    @Override
    public String getMessage() {
        return "The table already exists: " + tableName;
    }

}
