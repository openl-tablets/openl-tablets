package org.openl.rules.webstudio.web;

import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringUtils;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class TableHeaderSelector implements Predicate<TableSyntaxNode> {

    private final String value;

    @Override
    public boolean test(TableSyntaxNode node) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        var header = node.getHeaderLineValue().getValue();

        return StringUtils.containsIgnoreCase(header, value);
    }

}
