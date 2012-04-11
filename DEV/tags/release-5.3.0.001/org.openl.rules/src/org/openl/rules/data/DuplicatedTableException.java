/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 *
 */
public class DuplicatedTableException extends RuntimeException
// TODO - make it syntax error
{

    /**
     *
     */
    private static final long serialVersionUID = -6269440215951548170L;

    TableSyntaxNode existingTable;
    TableSyntaxNode duplicatedTable;
    String tableName;

    public DuplicatedTableException(String tableName, TableSyntaxNode existingTable, TableSyntaxNode duplicatedTable) {
        this.tableName = tableName;
        this.existingTable = existingTable;
        this.duplicatedTable = duplicatedTable;
    }

    /**
     * @return
     */
    public TableSyntaxNode getDuplicatedTable() {
        return duplicatedTable;
    }

    /**
     * @return
     */
    public TableSyntaxNode getExistingTable() {
        return existingTable;
    }

    /**
     *
     */

    @Override
    public String getMessage() {
        return "The table already exists: " + tableName;
    }

}
