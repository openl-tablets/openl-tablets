package org.openl.rules.lang.xls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.GridRegionUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.syntax.ISyntaxNode;

/**
 * Names the table each message of a module is about.
 *
 * <p>A message is about the table that holds the cell it points at. A message that points at no cell, or at a cell
 * out of every table, stays without a table.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageTables {

    /**
     * Sets the table of every message that has none yet and points at a cell of a table of the module.
     *
     * @param messages the messages of the module
     * @param module   the syntax of the module, its tables included
     */
    public static void name(Collection<OpenLMessage> messages, @Nullable ISyntaxNode module) {
        if (!(module instanceof XlsModuleSyntaxNode xlsModule)) {
            return;
        }
        Map<String, List<TableSyntaxNode>> bySheet = null;
        // The tables of a sheet are located once, when a message first points at the sheet
        var located = new HashMap<String, List<Located>>();
        for (var message : messages) {
            var location = message.getSourceLocation();
            if (message.getTable() == null && location != null && location.contains("sheet=")) {
                if (bySheet == null) {
                    bySheet = tablesBySheet(xlsModule);
                }
                var at = new XlsUrlParser(location);
                var tables = located.get(at.getWsName());
                if (tables == null) {
                    tables = Located.of(bySheet.getOrDefault(at.getWsName(), List.of()));
                    located.put(at.getWsName(), tables);
                }
                var table = tableAt(at, tables);
                if (table != null) {
                    message.setTable(TableSyntaxNodeUtils.getTableName(table));
                }
            }
        }
    }

    private static Map<String, List<TableSyntaxNode>> tablesBySheet(XlsModuleSyntaxNode module) {
        var bySheet = new HashMap<String, List<TableSyntaxNode>>();
        for (var table : module.getXlsTableSyntaxNodes()) {
            if (table.getModule() instanceof XlsSheetSourceCodeModule sheet) {
                bySheet.computeIfAbsent(sheet.getSheetName(), name -> new ArrayList<>()).add(table);
            }
        }
        return bySheet;
    }

    private static @Nullable TableSyntaxNode tableAt(XlsUrlParser at, List<Located> tables) {
        if (at.getRange() == null) {
            return null;
        }
        var region = GridRegionUtils.makeRegion(at.getRange());
        for (var table : tables) {
            if (table.holds(at, region)) {
                return table.table();
            }
        }
        return null;
    }

    /**
     * A table with the file and the cells it takes, read once from its address.
     */
    private record Located(TableSyntaxNode table, String wbPath, String wbName, @Nullable IGridRegion region) {

        static List<Located> of(List<TableSyntaxNode> tables) {
            return tables.stream().map(Located::of).toList();
        }

        private static Located of(TableSyntaxNode table) {
            var address = new XlsUrlParser(table.getUri());
            var range = address.getRange();
            return new Located(table, address.getWbPath(), address.getWbName(),
                    range == null ? null : GridRegionUtils.makeRegion(range));
        }

        /** Whether the cells a message points at are in the same file as the table and among its cells. */
        boolean holds(XlsUrlParser at, IGridRegion cells) {
            return region != null && wbName.equals(at.getWbName()) && wbPath.equals(at.getWbPath())
                    && GridRegionUtils.intersects(region, cells);
        }
    }
}
