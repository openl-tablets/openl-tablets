package org.openl.rules.maven.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.XlsUrlParser;

/**
 * Shared compile-and-rewrite helper used by every {@code rules.*} migrator, such as
 * {@link RulesRemovedFunctionsMigrator}. It rewrites the rules code in the workbooks of the project that fails to
 * compile with an error one of the fixes is for.
 * <p>
 * Only the code the compiler rejects is changed, so a valid call that looks like a removed one, such as a call of a
 * rule of the project named {@code dateToString}, stays as it is. The compiler names the cell of each error and the
 * piece of its text,
 * a row of a Method table body included. An error in the code a table generates from its cells is left for a manual
 * fix. Only the workbooks of the modules of the project are changed, not those of a dependency.
 * <p>
 * The project is compiled again after the rewrite, until no fix applies, so a removed call inside the arguments of
 * another one is rewritten too.
 *
 * @author Yury Molchan
 */
@Slf4j
final class RulesCodeIO {

    /**
     * Bounds the compilations. Each one after the first rewrites a removed call that was nested in another one.
     */
    private static final int MAX_PASSES = 10;

    private RulesCodeIO() {
    }

    /**
     * Rewrites the code that fails to compile with an error one of the fixes is for, and emits a
     * {@code Migrate: …} info line for each rewritten workbook.
     *
     * @return the rewritten workbooks; empty when no fix applied or the folder holds no project
     */
    static List<Path> rewrite(Migrator migrator, Path sourceFolder, MigrationContext context, List<CodeFix> fixes)
            throws IOException {
        var folder = sourceFolder.toRealPath();
        var modules = modules(folder);
        var changed = new LinkedHashSet<Path>();
        var passes = 0;
        var rewritten = !modules.isEmpty();
        while (rewritten && passes++ < MAX_PASSES) {
            rewritten = false;
            for (var entry : edits(modules, context.compilation().messages(), fixes).entrySet()) {
                if (apply(entry.getKey(), entry.getValue())) {
                    changed.add(entry.getKey());
                    rewritten = true;
                }
            }
            if (rewritten) {
                context.compilation().changed();
            }
        }
        changed.forEach(file -> log.info("Migrate: {} ({})", folder.relativize(file), migrator.getCommitMessage()));
        return List.copyOf(changed);
    }

    private static List<Module> modules(Path folder) {
        try {
            var project = ProjectResolver.getInstance().resolve(folder);
            return project == null ? List.of() : project.getModules();
        } catch (ProjectResolvingException e) {
            throw new IllegalStateException("Failed to read the project", e);
        }
    }

    /**
     * Collects the edits of the errors the fixes apply to, by workbook. An error reported twice, as by a module and
     * by the project that holds it, gives one edit.
     */
    private static Map<Path, Set<Edit>> edits(List<Module> modules,
                                              Collection<OpenLMessage> messages,
                                              List<CodeFix> fixes) {
        var edits = new LinkedHashMap<Path, Set<Edit>>();
        for (var message : messages) {
            var edit = message instanceof OpenLErrorMessage error ? editOf(modules, error, fixes) : null;
            if (edit != null) {
                edits.computeIfAbsent(edit.file(), x -> new LinkedHashSet<>()).add(edit);
            }
        }
        return edits;
    }

    private static @Nullable Edit editOf(List<Module> modules, OpenLErrorMessage error, List<CodeFix> fixes) {
        var range = error.getSourceRange();
        if (range == null) {
            return null;
        }
        var url = new XlsUrlParser(range.uri());
        var module = modules.stream().filter(m -> m.containsTable(range.uri())).findFirst().orElse(null);
        if (module == null || url.getWsName() == null || url.getCell() == null) {
            return null;
        }
        var code = range.code();
        var piece = code.substring(range.start(), range.end());
        return fixes.stream()
                .map(fix -> fix.fix(piece, error))
                .filter(Objects::nonNull)
                .findFirst()
                .map(replacement -> new Edit(module.getRulesPath(),
                        url.getWsName(),
                        url.getCell(),
                        code,
                        range.start(),
                        piece,
                        CodeText.operand(replacement, code, range.start(), range.end())))
                .orElse(null);
    }

    /**
     * Applies the edits to the workbook and saves it when a cell changed.
     *
     * @return {@code true} when the workbook is rewritten
     */
    private static boolean apply(Path file, Set<Edit> edits) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var in = Files.newInputStream(file); var workbook = WorkbookFactory.create(in)) {
            var cells = new LinkedHashMap<Cell, List<CellEdit>>();
            for (var edit : edits) {
                var sheet = workbook.getSheet(edit.sheet());
                var located = sheet == null ? null : locate(sheet, edit);
                if (located == null) {
                    log.warn("Cannot find '{}' in {}!{} of {}. Rewrite it with '{}'.",
                            edit.piece(),
                            edit.sheet(),
                            edit.cell(),
                            file.getFileName(),
                            edit.replacement());
                } else {
                    cells.computeIfAbsent(located.cell(), x -> new ArrayList<>()).add(located);
                }
            }
            var changed = false;
            for (var entry : cells.entrySet()) {
                changed |= replace(entry.getKey(), entry.getValue());
            }
            if (!changed) {
                return false;
            }
            workbook.write(bytes);
        }
        Files.write(file, bytes.toByteArray());
        return true;
    }

    /**
     * Finds the piece in the cell. The cell holds the text the compiler read the code from, or, for a text read from
     * somewhere else, holds it once.
     */
    private static @Nullable CellEdit locate(Sheet sheet, Edit edit) {
        var reference = new CellReference(edit.cell());
        var cell = PoiExcelHelper.getCell(reference.getCol(), reference.getRow(), sheet);
        var text = cell == null || cell.getCellType() != CellType.STRING ? null : cell.getStringCellValue();
        var at = text == null ? -1 : text.indexOf(edit.code());
        return at < 0 || text.indexOf(edit.code(), at + 1) >= 0
                ? null
                : new CellEdit(cell, at + edit.start(), edit.piece(), edit.replacement());
    }

    /**
     * Replaces the pieces in the cell, from the last one to the first, so that the positions of the others stay
     * valid. A piece that is no longer in its place, or that overlaps another one, as the same piece reported twice,
     * is left as it is.
     * <p>
     * The new text keeps the style of the cell and is stored as a shared string, also in a cell that held an inline
     * string.
     */
    private static boolean replace(Cell cell, List<CellEdit> edits) {
        var text = cell.getStringCellValue();
        var result = new StringBuilder(text);
        var limit = text.length();
        var replaced = false;
        var sorted = edits.stream().sorted(Comparator.comparingInt(CellEdit::offset).reversed()).toList();
        for (var edit : sorted) {
            var end = edit.offset() + edit.piece().length();
            if (end <= limit && text.startsWith(edit.piece(), edit.offset())) {
                result.replace(edit.offset(), end, edit.replacement());
                limit = edit.offset();
                replaced = true;
                log.info("{}!{}: {} -> {}",
                        cell.getSheet().getSheetName(),
                        cell.getAddress().formatAsString(),
                        edit.piece(),
                        edit.replacement());
            }
        }
        if (replaced) {
            // POI changes only the cached value of an inline string, which readers ignore
            cell.setBlank();
            cell.setCellValue(result.toString());
        }
        return replaced;
    }

    /**
     * The replacement of the piece the compiler reported at the position in the text it read from the named cell.
     */
    private record Edit(Path file, String sheet, String cell, String code, int start, String piece,
                        String replacement) {
    }

    /**
     * The replacement of the piece that starts at the position in the text of a cell.
     */
    private record CellEdit(Cell cell, int offset, String piece, String replacement) {
    }
}
