package org.openl.rules.webstudio.web;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

import java.util.function.Predicate;

/**
 * Handles array of table types (e.g. rules, spreadsheet, etc. see {@code XlsNodeTypes} constant for supported types).
 * Checks if given table type exists in current array.
 *
 * @author snshor
 *
 */
class TableTypeSelector implements Predicate<TableSyntaxNode> {

    private final String[] types;

    TableTypeSelector(String[] types) {
        this.types = types;
    }

    @Override
    public boolean test(TableSyntaxNode node) {
        String type = node.getType();
        return ArrayUtils.contains(types, type);
    }

}
