package org.openl.rules.webstudio.web;

import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Handles array of table types (e.g. rules, spreadsheet, etc. see {@code XlsNodeTypes} constant for supported types).
 * Checks if given table type exists in current array.
 *
 * @author snshor
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class TableTypeSelector implements Predicate<TableSyntaxNode> {

    private final String[] types;

    @Override
    public boolean test(TableSyntaxNode node) {
        var type = node.getType();
        return ArrayUtils.contains(types, type);
    }

}
