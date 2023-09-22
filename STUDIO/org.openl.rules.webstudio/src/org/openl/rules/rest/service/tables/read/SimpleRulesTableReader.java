package org.openl.rules.rest.service.tables.read;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.openl.rules.rest.model.tables.SimpleRulesView;
import org.openl.rules.rest.service.tables.OpenLTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.springframework.stereotype.Component;

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
        var tableBody = tsn.getTableBody();
        var headerSource = tsn.getHeader().getSourceString();

        var args = getArgs(headerSource, 0);
        var list = new ArrayList<LinkedHashMap<String, Object>>();
        var height = OpenLTableUtils.getHeightWithoutEmptyRows(tableBody);
        var width = OpenLTableUtils.getWidthWithoutEmptyColumns(tableBody);
        // start from 1 because 0 is header
        for (int row = 1; row < height; row++) {
            var rule = new LinkedHashMap<String, Object>();
            for (int col = 0; col < width; col++) {
                String argName = null;
                if (col == width - 1) {
                    argName = "return";
                } else if (col < args.size()) {
                    argName = args.get(col).name;
                }
                if (argName == null) {
                    argName = "arg" + col;
                }
                rule.put(argName, tableBody.getCell(col, row).getObjectValue());
            }
            list.add(rule);
        }
        builder.rules(list);
    }
}
