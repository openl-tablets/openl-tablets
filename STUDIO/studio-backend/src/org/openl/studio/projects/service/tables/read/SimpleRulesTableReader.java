package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.ARuleHeaderView;
import org.openl.studio.projects.model.tables.SimpleRuleHeaderView;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

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
    protected boolean supportsShape(IOpenLTable table) {
        return OpenLTableUtils.isSimpleRules(table);
    }

    @Override
    protected void initialize(SimpleRulesView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        if (tsn.getHeader().isCollect()) {
            builder.collect(true);
        }
        var metaInfoReader = tsn.getMetaInfoReader();
        var cellValueReader = new CellValueReader(metaInfoReader);
        var tableBody = tsn.getTableBody();

        var titles = tableBody.getRow(0);
        var headers = getConditionHeaders(titles);
        builder.headers(headers);
        var list = new ArrayList<LinkedHashMap<String, Object>>();
        // A rule is a row of the sheet: a value merged over several rows, such as a condition the rules below share,
        // belongs to each of them.
        var source = tableBody.getSource();
        var height = OpenLTableUtils.getHeightWithoutEmptyRows(source);
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(tableBody);
        for (var row = titles.getSource().getHeight(); row < height; row++) {
            var rule = new LinkedHashMap<String, Object>();
            // a column titled over several cells of the sheet holds its value in the first of them
            var cell = 0;
            for (var col = 0; col < width; col++) {
                String ruleName = null;
                if (col < headers.size()) {
                    ruleName = headers.get(col).title;
                }
                if (ruleName == null) {
                    ruleName = ARuleHeaderView.UNKNOWN_HEADER_NAME + col;
                }
                rule.put(ruleName, cellValueReader.apply(source.getCell(cell, row)));
                cell += tableBody.getColumnWidth(col);
            }
            list.add(rule);
        }
        builder.rules(list);
    }

    public static List<SimpleRuleHeaderView> getConditionHeaders(ILogicalTable headerBody) {
        var headers = new ArrayList<SimpleRuleHeaderView>();
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(headerBody);
        for (var col = 0; col < width; col++) {
            headers.add(SimpleRuleHeaderView.builder().title(headerBody.getCell(col, 0).getStringValue()).build());
        }
        return headers;
    }
}
