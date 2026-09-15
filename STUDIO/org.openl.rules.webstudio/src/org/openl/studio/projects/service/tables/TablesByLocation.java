package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;

/**
 * Which table a place in a workbook belongs to.
 *
 * <p>Built once for the tables of a project and asked about many places — a compilation message, a cell. Two
 * tables can only overlap where they are written on the same worksheet, so the tables are bucketed by workbook
 * and sheet and a place is tested only against the tables of its own sheet, rather than against every table of
 * the workspace.
 *
 * @author Vladyslav Pikus
 */
public final class TablesByLocation {

    private record Entry(TableSyntaxNode node, XlsUrlParser at) {
    }

    private final Map<String, List<Entry>> bySheet;

    private TablesByLocation(Map<String, List<Entry>> bySheet) {
        this.bySheet = bySheet;
    }

    /** Indexes every table the project holds, the tables of the modules it depends on included. */
    public static TablesByLocation of(ProjectModel model) {
        var bySheet = new HashMap<String, List<Entry>>();
        for (TableSyntaxNode node : model.getAllTableSyntaxNodes()) {
            var at = node.getUriParser();
            if (at != null) {
                bySheet.computeIfAbsent(sheetOf(at), sheet -> new ArrayList<>()).add(new Entry(node, at));
            }
        }
        return new TablesByLocation(bySheet);
    }

    /** The table the given place is written in, or {@code null} where no table covers it. */
    public @Nullable TableSyntaxNode find(XlsUrlParser at) {
        var candidates = bySheet.get(sheetOf(at));
        if (candidates == null) {
            return null;
        }
        for (Entry candidate : candidates) {
            if (at.intersects(candidate.at())) {
                return candidate.node();
            }
        }
        return null;
    }

    private static String sheetOf(XlsUrlParser at) {
        return at.getWbPath() + '\n' + at.getWbName() + '\n' + at.getWsName();
    }
}
