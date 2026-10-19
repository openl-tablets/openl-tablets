package org.openl.studio.projects.service.tables;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.LRUMap;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.rules.lang.xls.syntax.TableIds;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.source.IProjectSourceCodeModule;
import org.openl.studio.common.exception.GoneException;

/**
 * Remembers the ids the tables of a project had before they were renamed or deleted, so that a client holding such
 * an id is served the table it names now, or told that the table was deleted.
 *
 * <p>Each project folder of a user's workspace has a ledger of its own, which keeps its most recent changes in
 * memory. They are lost when the server restarts: an id from before is unknown then. The ledger learns of a rename
 * from a write of OpenL Studio that renames a table or changes its {@code id} property, from a move of a workbook
 * within the project, and from a compilation that finds a table under a new id at the cell where a table stood whose
 * id no table has any more. It learns of a deletion from OpenL Studio deleting the table.
 *
 * <p>An id a table has now always names that table: the ledger is asked only for an id no table has. A chain of
 * renames leads to the id at its end, and an id of a deleted table stays deleted until a table takes it again.
 *
 * @author Yury Molchan
 */
public final class StaleTableIds {

    /** How many changes each project remembers. */
    private static final int CHANGES = 1000;
    /** How many modules whose compilation was dropped are remembered. */
    private static final int MODULES = 1000;
    private static final String TABLE_ID = "tableId";

    private static final Map<Path, Ledger> LEDGERS = new ConcurrentHashMap<>();

    private StaleTableIds() {
    }

    /**
     * Learns that a table of a project is known by another id.
     *
     * <p>The ids that led to the old one lead to the new one from now on. The table had the old id until now, so the
     * rename is learned also for an id the ledger knew as deleted: a table took that id again.
     *
     * @param project the project folder in the workspace
     * @param from    the id the table had
     * @param to      the id the table has now
     */
    public static void renamed(Path project, String from, String to) {
        if (!from.equals(to)) {
            ledgerOf(project).renamed(from, to);
        }
    }

    /**
     * Learns that a table of the project a model compiled is known by another id.
     *
     * @param model the compiled project, which a model of no project leaves nothing to learn
     * @param from  the id the table had
     * @param to    the id the table has now
     * @see #renamed(Path, String, String)
     */
    public static void renamed(ProjectModel model, String from, String to) {
        var project = projectFolderOf(model);
        if (project != null) {
            renamed(project, from, to);
        }
    }

    /**
     * Learns that a table of a project was deleted: its id and the ids that led to it answer that it is gone.
     *
     * @param project the project folder in the workspace
     * @param id      the id the table had
     */
    public static void deleted(Path project, String id) {
        ledgerOf(project).deleted(id);
    }

    /**
     * Learns that a table of the project a model compiled was deleted.
     *
     * @param model the compiled project, which a model of no project leaves nothing to learn
     * @param id    the id the table had
     * @see #deleted(Path, String)
     */
    public static void deleted(ProjectModel model, String id) {
        var project = projectFolderOf(model);
        if (project != null) {
            deleted(project, id);
        }
    }

    /**
     * The table an id names in a compiled project.
     *
     * <p>A table that has the id now is the one it names. Otherwise the ledger of the project tells whether the table
     * was renamed: the request is served for the table it names now, whose id the {@code Content-Location} header of
     * the answer carries.
     *
     * @param model   the compiled project
     * @param tableId the id a request names a table by
     * @return the table, or {@code null} when no table has the id and the ledger knows nothing of it
     * @throws GoneException when the table was deleted
     */
    public static @Nullable IOpenLTable tableOf(ProjectModel model, String tableId) {
        var table = model.getTableById(tableId);
        var project = projectFolderOf(model);
        if (table != null || project == null) {
            return table;
        }
        var current = ledgerOf(project).currentOf(tableId);
        table = current == null ? null : model.getTableById(current);
        if (table != null) {
            servedAs(tableId, current);
        }
        return table;
    }

    /**
     * The id a table of a project is known by now, for an id no table of what the request reads has.
     *
     * <p>A renamed table is known by the id it has now, which the {@code Content-Location} header of the answer
     * carries. An id the ledger knows nothing of is answered as it is.
     *
     * @param project the project folder in the workspace
     * @param tableId the id a request names a table by
     * @return the id the table has now
     * @throws GoneException when the table was deleted
     */
    public static String currentIdOf(Path project, String tableId) {
        var current = ledgerOf(project).currentOf(tableId);
        if (current == null) {
            return tableId;
        }
        servedAs(tableId, current);
        return current;
    }

    /** The folder of the project a model compiled, or {@code null} for a model of no project. */
    private static @Nullable Path projectFolderOf(ProjectModel model) {
        var module = model.getModuleInfo();
        var project = module == null ? null : module.getProject();
        return project == null ? null : project.getProjectFolder();
    }

    private static Ledger ledgerOf(Path project) {
        return LEDGERS.computeIfAbsent(project.toAbsolutePath().normalize(), folder -> new Ledger());
    }

    /**
     * Names the address of what the answer serves: the address of the request, with the id it named the table by
     * replaced by the id the table has now.
     */
    private static void servedAs(String stale, String current) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || attributes.getResponse() == null) {
            return;
        }
        var request = attributes.getRequest();
        var path = Arrays.stream(request.getRequestURI().split("/", -1))
                .map(segment -> segment.equals(stale) ? current : segment)
                .collect(Collectors.joining("/"));
        var query = request.getQueryString();
        if (query != null) {
            path += Arrays.stream(query.split("&", -1))
                    .map(parameter -> parameter.equals(TABLE_ID + "=" + stale) ? TABLE_ID + "=" + current : parameter)
                    .collect(Collectors.joining("&", "?", ""));
        }
        attributes.getResponse().setHeader(HttpHeaders.CONTENT_LOCATION, path);
    }

    /** The changes of the tables of one project: where each stale id leads, {@code null} for a deleted table. */
    private static final class Ledger extends LinkedHashMap<String, @Nullable String> {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, @Nullable String> eldest) {
            return size() > CHANGES;
        }

        /** A rename of a table that had the old id until now. */
        synchronized void renamed(String from, String to) {
            replaceAll((stale, current) -> from.equals(current) ? to : current);
            // The new id names a table now, which no change of before leads elsewhere.
            remove(to);
            remove(from);
            put(from, to);
        }

        /**
         * A rename a compilation found. A deleted table takes no part in it, though a table later stood where it
         * stood.
         */
        synchronized void found(String from, String to) {
            if (!containsKey(from) || get(from) != null) {
                renamed(from, to);
            }
        }

        synchronized void deleted(String id) {
            replaceAll((stale, current) -> id.equals(current) ? null : current);
            remove(id);
            put(id, null);
        }

        /**
         * Where a stale id leads.
         *
         * @return the id the table has now, or {@code null} when the ledger knows nothing of the id
         * @throws GoneException when the table was deleted
         */
        synchronized @Nullable String currentOf(String id) {
            if (!containsKey(id)) {
                return null;
            }
            var current = get(id);
            if (current == null) {
                throw new GoneException("table.deleted.message");
            }
            return current;
        }
    }

    /**
     * Learns the tables a compilation of a module finds renamed.
     *
     * <p>A table whose id no table of the module has any more, and a table under an id the module had no table with,
     * that start at one cell are one table renamed. The ids the module had are taken when its compilation is dropped,
     * and only when they were asked for: ids nobody was given need no ledger. They are kept until the module is
     * compiled again, for the most recent modules.
     */
    public static final class Compilations {

        /** The ids of the tables by the cell they start at, of each module whose compilation was dropped. */
        private final Map<String, Map<String, String>> dropped = Collections.synchronizedMap(new LRUMap<>(MODULES));

        /**
         * Takes the ids of the tables of a module whose compilation is dropped.
         *
         * @param module the module as it was compiled
         */
        public void dropped(XlsModuleSyntaxNode module) {
            if (module.getModule() instanceof IProjectSourceCodeModule source && source.getFileUri() != null
                    && TableIds.isNumbered(module)) {
                dropped.put(source.getFileUri(), TableIds.idsByCell(module));
            }
        }

        /**
         * Learns the renames of a module compiled again since its compilation was dropped.
         *
         * @param module the module as it is compiled now
         */
        public void compiled(XlsModuleSyntaxNode module) {
            if (module.getModule() instanceof IProjectSourceCodeModule source && source.getFileUri() != null) {
                var before = dropped.remove(source.getFileUri());
                if (before != null) {
                    renamedBetween(source.getProjectFolder(), before, TableIds.idsByCell(module));
                }
            }
        }

        /** Learns the tables found under a new id at the cell where a table with an id no table has stood. */
        private static void renamedBetween(Path project, Map<String, String> before, Map<String, String> after) {
            var kept = new HashSet<>(after.values());
            var known = new HashSet<>(before.values());
            before.forEach((cell, id) -> {
                var now = after.get(cell);
                if (now != null && !kept.contains(id) && !known.contains(now)) {
                    ledgerOf(project).found(id, now);
                }
            });
        }
    }
}
