package org.openl.rules.table.search.selectors;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.AStringBoolOperator;
import org.openl.util.AStringBoolOperator.ContainsIgnoreCaseOperator;

public class TableHeaderSelector extends TableDataSelector {

    private AStringBoolOperator matchOperator;
    private String value;

    public TableHeaderSelector() {
    }

    public TableHeaderSelector(String value) {
        this(value, new ContainsIgnoreCaseOperator(null));
    }

    public TableHeaderSelector(String value, AStringBoolOperator matchOperator) {
        this.value = value;
        this.matchOperator = matchOperator;
    }

    public AStringBoolOperator getMatchOperator() {
        return matchOperator;
    }

    public void setMatchOperator(AStringBoolOperator matchOperator) {
        this.matchOperator = matchOperator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean select(TableSyntaxNode node) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        String header = node.getHeaderLineValue().getValue();

        return matchOperator.isMatching(value, header);
    }

}
