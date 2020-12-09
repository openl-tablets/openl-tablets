package org.openl.rules.webstudio.web;

import java.util.function.Predicate;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringUtils;

class TableHeaderSelector implements Predicate<TableSyntaxNode> {

    private final String value;

    TableHeaderSelector(String value) {
        this.value = value;
    }

    @Override
    public boolean test(TableSyntaxNode node) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        String header = node.getHeaderLineValue().getValue();

        return StringUtils.containsIgnoreCase(header, value);
    }

}
