package org.openl.studio.projects.service.tables.read;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.SmartRulesHeaderView;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;

/**
 * Reads {@code SmartRules} table to {@link SmartRulesView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class SmartRulesTableReader extends ExecutableTableReader<SmartRulesView, SmartRulesView.Builder> {

    public static final String RANGE_RULE_MIN = "min";
    public static final String RANGE_RULE_MAX = "max";

    public SmartRulesTableReader() {
        super(SmartRulesView::builder);
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isSmartRules(table);
    }

    @Override
    protected void initialize(SmartRulesView.Builder builder, IOpenLTable openLTable) {
        super.initialize(builder, openLTable);

        var tsn = openLTable.getSyntaxNode();
        var tableBody = tsn.getTableBody();

        var headers = getConditionHeaders(tableBody.getRow(0));
        builder.headers(headers);
        processRules(builder,
                headers,
                tableBody.getSubtable(0, 1, tableBody.getWidth(), tableBody.getHeight() - 1),
                tsn.getMetaInfoReader());
    }

    private void processRules(SmartRulesView.Builder builder,
                              List<SmartRulesHeaderView> headers,
                              ILogicalTable rulesBody,
                              MetaInfoReader metaInfoReader) {
        var list = new ArrayList<LinkedHashMap<String, Object>>();
        var sourceTable = rulesBody.getSource();
        var height = OpenLTableUtils.getHeightWithoutEmptyRows(sourceTable);
        for (int rowId = 0; rowId < height; rowId++) {
            var rule = new LinkedHashMap<String, Object>();
            int colId = 0;
            for (var header : headers) {
                Object value;
                if (header.width == 1) {
                    value = getCellValue(sourceTable.getCell(colId, rowId), metaInfoReader);
                    colId++;
                } else if (header.width == 2) {
                    var rangeRule = new HashMap<String, Object>();
                    rangeRule.put(RANGE_RULE_MIN, getCellValue(sourceTable.getCell(colId, rowId), metaInfoReader));
                    rangeRule.put(RANGE_RULE_MAX, getCellValue(sourceTable.getCell(colId + 1, rowId), metaInfoReader));
                    colId += 2;
                    value = rangeRule;
                } else {
                    throw new IllegalStateException("Unexpected header width: " + header.width);
                }
                rule.put(header.title, value);
            }
            list.add(rule);
        }
        builder.rules(list);
    }

    public static List<SmartRulesHeaderView> getConditionHeaders(ILogicalTable headerBody) {
        List<SmartRulesHeaderView> headers = new ArrayList<>();
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(headerBody);
        for (int col = 0; col < width; col++) {
            var cell = headerBody.getCell(col, 0);
            headers.add(SmartRulesHeaderView.builder().title(cell.getStringValue()).width(cell.getWidth()).build());
        }
        return headers;
    }
}
