package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.rest.model.tables.SimpleRuleHeaderView;
import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;

/**
 * Reads {@code SimpleRules} table to {@link SimpleRulesView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class SimpleRulesTableReader extends ExecutableTableReader<SimpleRulesView, SimpleRulesView.Builder> {

    public SimpleRulesTableReader() {
        super(SimpleRulesView::builder);
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isSimpleRules(table);
    }

    @Override
    protected void initialize(SimpleRulesView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var metaInfoReader = tsn.getMetaInfoReader();
        var tableBody = tsn.getTableBody();

        var headers = getConditionHeaders(tableBody.getRow(0));
        builder.headers(headers);
        var list = new ArrayList<LinkedHashMap<String, Object>>();
        var height = OpenLTableUtils.getHeightWithoutEmptyRows(tableBody);
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(tableBody);
        // start from 1 because 0 is header
        for (int row = 1; row < height; row++) {
            var rule = new LinkedHashMap<String, Object>();
            for (int col = 0; col < width; col++) {
                String ruleName = null;
                if (col < headers.size()) {
                    ruleName = headers.get(col).title;
                }
                if (ruleName == null) {
                    ruleName = SimpleRuleHeaderView.UNKNOWN_HEADER_NAME + col;
                }
                rule.put(ruleName, getCellValue(tableBody.getCell(col, row), metaInfoReader));
            }
            list.add(rule);
        }
        builder.rules(list);
    }

    public static List<SimpleRuleHeaderView> getConditionHeaders(ILogicalTable headerBody) {
        List<SimpleRuleHeaderView> headers = new ArrayList<>();
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(headerBody);
        for (int col = 0; col < width; col++) {
            headers.add(SimpleRuleHeaderView.builder().title(headerBody.getCell(col, 0).getStringValue()).build());
        }
        return headers;
    }
}
