package org.openl.rules.lang.xls.syntax;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.LRUMap;
import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.IProjectSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.util.HashingUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.FileNameFormatter;

/**
 * Names the tables of a module by what they are, not by where they stand.
 *
 * <p>An id is made of the place of the table's workbook in its project, the kind of the table and its key: the
 * {@code id} property when the table declares one, its name otherwise. Moving the table on its sheet or to another
 * sheet of its workbook keeps the id, and so do its parameters, its return type and its properties. Renaming the
 * table, or changing its {@code id} property, gives it a new one. Neither the project nor the place of the project
 * on a disk is part of an id, so a table has one id for every user and machine.
 *
 * <p>Tables of a module that share all three are numbered in the order the module holds them: by workbook, by sheet,
 * then from top to bottom. A table OpenL Studio moves keeps the number it had until the server restarts, and so do
 * the tables sharing what a table OpenL Studio deletes; see {@link #idAfterWrite} and {@link #deleting}.
 *
 * <p>An id is 16 upper-case characters of Crockford's Base32: the first 80 bits of the SHA-256 digest of what the
 * table is. Its digits and capitals stand apart from the lower-case words of a URL path.
 *
 * @author Yury Molchan
 */
public final class TableIds {

    /** Starts every identity, so that a change of what an identity is made of gives new ids. */
    private static final String VERSION = "v1";
    private static final char SEPARATOR = '\u001F';
    /** 80 bits are 16 symbols of Base32, with no padding. */
    private static final int ID_SYMBOLS = 16;
    /** How many tables keep their numbers. */
    private static final int PINS = 1000;
    private static final String ID_PROPERTY = "id";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** Where a table that keeps its number stands, and what it is and the number it keeps there. */
    private static final Map<String, Numbered> PINNED = Collections.synchronizedMap(new LRUMap<>(PINS));

    private TableIds() {
    }

    /** What a table declares into, which its key is unique in. */
    private enum Kind {
        METHOD("method"),
        TYPE("type"),
        DATA("data"),
        TEST("test"),
        RUN("run"),
        CONSTANTS("constants"),
        CONDITIONS("conditions"),
        ACTIONS("actions"),
        RETURNS("returns"),
        PROPERTIES("properties"),
        ENVIRONMENT("environment"),
        TABLE_PART("tablepart"),
        GENERATED("generated"),
        OTHER("other");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        static Kind of(@Nullable XlsNodeTypes type) {
            return switch (type) {
                case XLS_DT, XLS_SPREADSHEET, XLS_TBASIC, XLS_COLUMN_MATCH, XLS_METHOD -> METHOD;
                case XLS_DATATYPE -> TYPE;
                case XLS_DATA -> DATA;
                case XLS_TEST_METHOD -> TEST;
                case XLS_RUN_METHOD -> RUN;
                case XLS_CONSTANTS -> CONSTANTS;
                case XLS_CONDITIONS -> CONDITIONS;
                case XLS_ACTIONS -> ACTIONS;
                case XLS_RETURNS -> RETURNS;
                case XLS_PROPERTIES -> PROPERTIES;
                case XLS_ENVIRONMENT -> ENVIRONMENT;
                case XLS_TABLEPART -> TABLE_PART;
                case null, default -> OTHER;
            };
        }
    }

    /** What a table is: the place of its workbook in the project, its kind and its key. */
    record Identity(String module, String kind, String key) {

        /** The id of the table that is the given one of the tables sharing this identity, counted from 1. */
        String id(int number) {
            var text = VERSION + SEPARATOR + module + SEPARATOR + kind + SEPARATOR + key
                    + (number > 1 ? SEPARATOR + "~" + number : "");
            return HashingUtils.sha256Base32(text, ID_SYMBOLS);
        }
    }

    /** Where a table stands in the order its module holds its tables. */
    private record Place(int workbook, int part, int sheet, int top, int left) implements Comparable<Place> {

        private static final Comparator<Place> ORDER = Comparator.comparingInt(Place::workbook)
                .thenComparingInt(Place::part)
                .thenComparingInt(Place::sheet)
                .thenComparingInt(Place::top)
                .thenComparingInt(Place::left);

        /** A table of a sheet. */
        static final int ON_SHEET = 0;
        /** A table gathered from parts written on several sheets. */
        static final int GATHERED = 1;
        /** A table the compiler built, which stands on no sheet of a file. */
        static final int BUILT = 2;

        @Override
        public int compareTo(Place other) {
            return ORDER.compare(this, other);
        }
    }

    /** What a table is, and its number among the tables of its module that are what it is. */
    record Numbered(Identity identity, int number, String id) {

        Numbered(Identity identity, int number) {
            this(identity, number, identity.id(number));
        }
    }

    private record Entry(TableSyntaxNode table, Identity identity, Place place, @Nullable String pin) {
    }

    /**
     * The project folder of a module, the file or the folder of the module, and the place given to its tables that
     * stand in no file.
     */
    private record Folder(@Nullable Path project, @Nullable Path root, String unfiled) {

        static Folder of(@Nullable XlsModuleSyntaxNode module) {
            if (module == null) {
                return new Folder(null, null, "");
            }
            var source = module.getModule();
            var project = projectFolderOf(source);
            var file = fileOf(source);
            return new Folder(project, file,
                    file == null ? Objects.toString(source.getUri(), "") : pathOf(file, project));
        }

        /**
         * What the tables of a file are named after: the module, for a file of a module written as a folder of
         * Markdown files, or else the file itself.
         *
         * <p>A file of such a module holds a table as a sheet of a workbook holds its tables, so a file renamed or
         * moved within the folder keeps the ids of its tables.
         */
        Path anchorOf(Path file) {
            return root != null && file.startsWith(root) ? root : file;
        }
    }

    /**
     * The id of a table, numbering the tables of its module the first time one of them is asked about.
     */
    static String idOf(TableSyntaxNode table) {
        return numberedOf(table).id();
    }

    /** What a table is and its number, numbering the tables of its module the first time one of them is asked about. */
    private static Numbered numberedOf(TableSyntaxNode table) {
        var module = moduleOf(table);
        if (module != null) {
            synchronized (module) {
                if (table.numbered() == null) {
                    assign(module);
                }
            }
        }
        var numbered = table.numbered();
        if (numbered == null) {
            // A table no module holds has no other tables to be numbered with.
            numbered = new Numbered(identityOf(table, Folder.of(null)), 1);
            table.number(numbered);
        }
        return numbered;
    }

    /**
     * The id of the table a compiled member comes from.
     *
     * @param info what the compiler knows of where the member is written
     * @return the id of the table, or {@code null} for a member no table holds
     */
    public static @Nullable String idOf(@Nullable IMemberMetaInfo info) {
        return info != null && info.getSyntaxNode() instanceof TableSyntaxNode table ? table.getId() : null;
    }

    /**
     * The id a table written into a module is known by once the module is compiled again.
     *
     * <p>The written table is read as the compiler reads it: its kind and name from its header, its {@code id}
     * property from its properties. It is numbered among the tables of the module that are what it is, at the place
     * it was written to.
     *
     * @param written the written table, its header included
     * @param module  the module as last compiled, or {@code null} when nothing is known of its other tables
     * @return the id of the written table
     */
    public static String idOfWritten(IGridTable written, @Nullable XlsModuleSyntaxNode module) {
        var node = nodeOf(written);
        return node == null ? unreadable(written).id(1) : numberedAsWritten(node, module, null).id();
    }

    /**
     * The id a table OpenL Studio has just written is known by once its module is compiled again.
     *
     * <p>A table that is still what it was keeps its id, also when the write moved it from where it was compiled to
     * make room for what it added. Such a table keeps its number among the tables sharing what it is, until the
     * server restarts, even when the move puts it after them on its sheet.
     *
     * @param table   the table as last compiled
     * @param written the table as written, its header included
     * @return the id of the written table
     */
    public static String idAfterWrite(TableSyntaxNode table, IGridTable written) {
        var node = nodeOf(written);
        if (node == null) {
            return unreadable(written).id(1);
        }
        var numbered = numberedAsWritten(node, moduleOf(table), table);
        var pin = pinOf(node);
        if (pin != null && isMoved(table, written.getRegion()) && numbered.id().equals(table.getId())) {
            PINNED.put(pin, numbered);
        }
        return numbered.id();
    }

    /**
     * Keeps the ids of the tables that share what a table is, as OpenL Studio deletes that table.
     *
     * <p>The tables after it keep their numbers until the server restarts, instead of each taking the id of the one
     * before it. The id of the deleted table names no table then, until a table that is what it was takes it.
     *
     * @param table the table as last compiled, before it is deleted
     */
    public static void deleting(TableSyntaxNode table) {
        var module = moduleOf(table);
        if (module == null) {
            return;
        }
        var identity = numberedOf(table).identity();
        for (var entry : entries(module, table)) {
            if (entry.pin() != null && entry.identity().equals(identity)) {
                PINNED.put(entry.pin(), numberedOf(entry.table()));
            }
        }
    }

    /**
     * The ids the tables of a module are known by once a file or a folder of their project moves.
     *
     * <p>A table of a workbook that moves keeps its kind, its key and its number, under the new place of the
     * workbook; it keeps a number OpenL Studio pinned it to as well. A table the compiler built for a module whose
     * workbook moves is named after the new place. The tables of the other workbooks keep their ids.
     *
     * <p>A module written as a folder of Markdown files names its tables after the folder: a file moved within the
     * folder keeps the ids of its tables, and a moved folder gives every table of the module the id it has at the
     * new place.
     *
     * @param module the module as last compiled
     * @param from   the file or the folder that moves
     * @param to     where it moves
     * @return the new id of each table of the moved workbooks, by the id it has now
     */
    public static Map<String, String> idsAfterMove(XlsModuleSyntaxNode module, Path from, Path to) {
        var source = from.toAbsolutePath().normalize();
        var target = to.toAbsolutePath().normalize();
        var folder = Folder.of(module);
        var moduleFile = fileOf(module.getModule());
        var ids = new LinkedHashMap<String, String>();
        for (var entry : entries(module, null)) {
            var file = fileOfTable(entry.table());
            var numbered = numberedOf(entry.table());
            var moved = movedTo(file != null ? folder.anchorOf(file) : moduleFile, source, target);
            var renamed = moved == null ? numbered : new Numbered(new Identity(pathOf(moved, folder.project()),
                    numbered.identity().kind(), numbered.identity().key()), numbered.number());
            if (moved != null) {
                ids.put(numbered.id(), renamed.id());
            }
            var movedFile = file == null ? null : movedTo(file, source, target);
            if (movedFile != null) {
                movePin(entry, movedFile, renamed);
            }
        }
        return ids;
    }

    /**
     * Whether the tables of a module have their ids: whether an id of any of them was asked for.
     *
     * @param module a compiled module
     * @return {@code true} when {@link #idsByCell} reads no table to answer
     */
    public static boolean isNumbered(XlsModuleSyntaxNode module) {
        return Arrays.stream(module.getWorkbookSyntaxNodes())
                .flatMap(workbook -> Arrays.stream(workbook.getTableSyntaxNodes()))
                .anyMatch(table -> table.numbered() != null);
    }

    /**
     * The ids of the tables written on the sheets of a module, by the cell each table starts at.
     *
     * <p>A cell is told by its workbook file, its sheet, its row and its column. The tables gathered from parts and
     * the tables the compiler built stand at no cell and are left out.
     *
     * @param module a compiled module
     * @return the id of each table, by the cell its header starts at
     */
    public static Map<String, String> idsByCell(XlsModuleSyntaxNode module) {
        var ids = new LinkedHashMap<String, String>();
        for (var workbook : module.getWorkbookSyntaxNodes()) {
            for (var sheet : workbook.getWorksheetSyntaxNodes()) {
                for (var table : sheet.getTableSyntaxNodes()) {
                    var cell = pinOf(table);
                    if (cell != null) {
                        ids.put(cell, table.getId());
                    }
                }
            }
        }
        return ids;
    }

    /** Where a file is once a file or a folder moves, or {@code null} for a file the move leaves where it is. */
    private static @Nullable Path movedTo(@Nullable Path file, Path from, Path to) {
        if (file == null || !file.startsWith(from)) {
            return null;
        }
        return to.resolve(from.relativize(file)).normalize();
    }

    /** Pins a table that keeps its number at its place in a moved workbook. */
    private static void movePin(Entry entry, Path file, Numbered renamed) {
        var pin = entry.pin() == null ? null : PINNED.remove(entry.pin());
        if (pin != null && pin.equals(entry.table().numbered())) {
            PINNED.put(pinAt(file, entry.table()), renamed);
        }
    }

    /** Whether a table stands elsewhere than the cell it was compiled at. */
    private static boolean isMoved(TableSyntaxNode table, IGridRegion region) {
        var compiled = table.getGridLocation().getStart();
        return region.getTop() != compiled.getLine(null) || region.getLeft() != compiled.getColumn(null);
    }

    private static Numbered numberedAsWritten(TableSyntaxNode node, @Nullable XlsModuleSyntaxNode module,
                                              @Nullable TableSyntaxNode replaced) {
        var identity = identityOf(node, Folder.of(module));
        if (replaced != null) {
            var kept = numberedOf(replaced);
            if (kept.identity().equals(identity)) {
                // Still what it was: it keeps its number. The table is not read again, as the grid shows it as
                // written.
                return kept;
            }
        }
        if (module == null) {
            return new Numbered(identity, 1);
        }
        var place = placeOf(node, module);
        var before = entries(module, replaced).stream()
                .filter(entry -> entry.identity().equals(identity) && entry.place().compareTo(place) < 0)
                .count();
        return new Numbered(identity, (int) before + 1);
    }

    /** Numbers the tables of a module and gives each its id. */
    private static void assign(XlsModuleSyntaxNode module) {
        var entries = entries(module, null);
        entries.sort(Comparator.comparing(Entry::place));
        var groups = new LinkedHashMap<Identity, List<Entry>>();
        for (var entry : entries) {
            groups.computeIfAbsent(entry.identity(), identity -> new ArrayList<>()).add(entry);
        }
        var used = new HashSet<String>();
        for (var group : groups.values()) {
            var numbers = numbers(group);
            for (var i = 0; i < group.size(); i++) {
                var entry = group.get(i);
                var numbered = new Numbered(entry.identity(), numbers[i]);
                // Two identities with one digest are told apart as tables of one identity are.
                while (!used.add(numbered.id())) {
                    numbered = new Numbered(entry.identity(), numbered.number() + group.size());
                }
                entry.table().number(numbered);
            }
        }
    }

    /**
     * The numbers of the tables sharing one identity, in the order the module holds them. A pinned table keeps the
     * number it was pinned to, and the others take the numbers left, in order.
     */
    private static int[] numbers(List<Entry> group) {
        var numbers = new int[group.size()];
        Set<Integer> taken = new HashSet<>();
        for (var i = 0; i < group.size(); i++) {
            var number = pinnedNumber(group.get(i));
            if (number > 0 && taken.add(number)) {
                numbers[i] = number;
            }
        }
        var next = 1;
        for (var i = 0; i < numbers.length; i++) {
            if (numbers[i] == 0) {
                while (taken.contains(next)) {
                    next++;
                }
                numbers[i] = next;
                taken.add(next);
            }
        }
        return numbers;
    }

    /** The number a table was pinned to, or 0 when it has none: a pin of a table that is no more what it was. */
    private static int pinnedNumber(Entry entry) {
        var pin = entry.pin() == null ? null : PINNED.get(entry.pin());
        return pin != null && pin.identity().equals(entry.identity()) ? pin.number() : 0;
    }

    /** The tables of a module, with what each is and where it stands, but the excluded one. */
    private static List<Entry> entries(XlsModuleSyntaxNode module, @Nullable TableSyntaxNode excluded) {
        var folder = Folder.of(module);
        var entries = new ArrayList<Entry>();
        var workbooks = module.getWorkbookSyntaxNodes();
        for (var w = 0; w < workbooks.length; w++) {
            var sheets = workbooks[w].getWorksheetSyntaxNodes();
            for (var s = 0; s < sheets.length; s++) {
                for (var table : sheets[s].getTableSyntaxNodes()) {
                    add(entries, table, excluded, folder, w, s, Place.ON_SHEET);
                }
            }
            for (var table : workbooks[w].getMergedTableParts()) {
                add(entries, table, excluded, folder, w, sheets.length, Place.GATHERED);
            }
        }
        return entries;
    }

    /** Adds a table to the entries; a numbered table is what it was numbered as, and is not read again. */
    private static void add(List<Entry> entries, TableSyntaxNode table, @Nullable TableSyntaxNode excluded,
                            Folder folder, int workbook, int sheet, int part) {
        if (table != excluded) {
            var file = fileOfTable(table);
            var region = table.getGridTable().getRegion();
            var place = new Place(workbook, file == null ? Place.BUILT : part, sheet, region.getTop(),
                    region.getLeft());
            var numbered = table.numbered();
            var identity = numbered != null ? numbered.identity() : identityOf(table, file, folder);
            entries.add(new Entry(table, identity, place, file == null ? null : pinAt(file, table)));
        }
    }

    /** Where a written table stands among the tables of the module it is written to. */
    private static Place placeOf(TableSyntaxNode node, XlsModuleSyntaxNode module) {
        var source = node.getXlsSheetSourceCodeModule();
        var sheetUri = source == null ? null : source.getUri();
        var workbookUri = source == null ? null : source.getWorkbookSource().getUri();
        var workbooks = module.getWorkbookSyntaxNodes();
        var workbook = workbooks.length;
        var sheet = Integer.MAX_VALUE;
        for (var w = 0; w < workbooks.length; w++) {
            if (Objects.equals(workbookUri, workbooks[w].getWorkbookSourceCodeModule().getUri())) {
                workbook = w;
                sheet = sheetIndex(workbooks[w], sheetUri);
                break;
            }
        }
        var region = node.getGridTable().getRegion();
        return new Place(workbook, Place.ON_SHEET, sheet, region.getTop(), region.getLeft());
    }

    private static int sheetIndex(WorkbookSyntaxNode workbook, @Nullable String sheetUri) {
        var sheets = workbook.getWorksheetSyntaxNodes();
        for (var s = 0; s < sheets.length; s++) {
            if (Objects.equals(sheetUri, sheets[s].getModule().getUri())) {
                return s;
            }
        }
        // a sheet the module was compiled without comes after its sheets
        return sheets.length;
    }

    /** The table as the compiler reads it from what is written, or {@code null} when its header cannot be read. */
    private static @Nullable TableSyntaxNode nodeOf(IGridTable written) {
        if (!(written.getGrid() instanceof XlsSheetGridModel sheet)) {
            return null;
        }
        try {
            return XlsHelper.createTableSyntaxNode(written, sheet.getSheetSource());
        } catch (OpenLCompilationException e) {
            return null;
        }
    }

    /** The identity of a table whose header cannot be read: what its first cell says, in no module. */
    private static Identity unreadable(IGridTable written) {
        return new Identity("", Kind.OTHER.label, normalized(written.getCell(0, 0).getStringValue()));
    }

    private static Identity identityOf(TableSyntaxNode table, Folder folder) {
        return identityOf(table, fileOfTable(table), folder);
    }

    private static Identity identityOf(TableSyntaxNode table, @Nullable Path file, Folder folder) {
        var kind = file == null ? Kind.GENERATED : Kind.of(table.getNodeType());
        var workbookProject = projectFolderOf(workbookOf(table));
        // Neither may be known, as for a table compiled from a workbook of no project
        var project = workbookProject != null ? workbookProject : folder.project();
        var module = file == null ? folder.unfiled() : pathOf(folder.anchorOf(file), project);
        return new Identity(module, kind.label, keyOf(table, kind));
    }

    /** The key of a table among the tables of its kind. */
    private static String keyOf(TableSyntaxNode table, Kind kind) {
        var header = Objects.toString(table.getGridTable().getCell(0, 0).getStringValue(), "").trim();
        var rest = afterFirstWord(header);
        var words = rest.isEmpty() ? new String[0] : WHITESPACE.split(rest);
        var key = switch (kind) {
            case METHOD -> {
                var id = declaredId(table);
                yield id != null ? ID_PROPERTY + "=" + id : nameOf(rest);
            }
            case GENERATED -> nameOf(rest);
            case TYPE -> words.length > 0 ? StringUtils.trimToNull(words[0].split("<", 2)[0]) : null;
            case DATA -> words.length > 1 ? words[1] : null;
            case TEST, RUN -> words.length > 1 ? words[1] : words.length > 0 ? words[0] : null;
            case TABLE_PART -> words.length > 2 ? words[0] + "#" + words[2] : null;
            case PROPERTIES -> scopeOf(table);
            case OTHER -> normalized(header);
            case CONSTANTS, CONDITIONS, ACTIONS, RETURNS, ENVIRONMENT -> "";
        };
        // A header whose name cannot be read is told by what it says.
        return key != null ? key : "!" + normalized(rest);
    }

    private static String afterFirstWord(String header) {
        var space = 0;
        while (space < header.length() && !Character.isWhitespace(header.charAt(space))) {
            space++;
        }
        return header.substring(space).trim();
    }

    /** The name of a method: the word before its parameters, or its last word when it has none. */
    private static @Nullable String nameOf(String rest) {
        var paren = rest.indexOf('(');
        var end = paren < 0 ? rest.length() : paren;
        while (end > 0 && Character.isWhitespace(rest.charAt(end - 1))) {
            end--;
        }
        var start = end;
        while (start > 0 && Character.isJavaIdentifierPart(rest.charAt(start - 1))) {
            start--;
        }
        return start < end ? rest.substring(start, end) : null;
    }

    /** The {@code id} property the table declares, or {@code null} when it declares none. */
    private static @Nullable String declaredId(TableSyntaxNode table) {
        var section = PropertiesHelper.getPropertiesTableSection(table.getTable());
        if (section == null || section.getWidth() < 2) {
            return null;
        }
        for (var row = 0; row < section.getHeight(); row++) {
            var line = section.getRow(row);
            if (ID_PROPERTY.equals(StringUtils.trim(textOf(line.getColumn(0))))) {
                return StringUtils.trimToNull(textOf(line.getColumn(1)));
            }
        }
        return null;
    }

    /** What a {@code Properties} table applies to: its scope and its category. */
    private static String scopeOf(TableSyntaxNode table) {
        var body = table.getTable();
        String scope = null;
        String category = null;
        for (var row = 1; row < body.getHeight(); row++) {
            var line = body.getRow(row);
            if (line.getWidth() > 1) {
                var name = StringUtils.trim(textOf(line.getColumn(0)));
                if ("scope".equals(name)) {
                    scope = StringUtils.trim(textOf(line.getColumn(1)));
                } else if ("category".equals(name)) {
                    category = StringUtils.trim(textOf(line.getColumn(1)));
                }
            }
        }
        return "scope=" + Objects.toString(scope, "") + ";category=" + Objects.toString(category, "");
    }

    private static @Nullable String textOf(ILogicalTable cell) {
        return cell.getSource().getCell(0, 0).getStringValue();
    }

    private static String normalized(@Nullable String text) {
        return text == null ? "" : WHITESPACE.matcher(text.trim()).replaceAll(" ");
    }

    /** Where a table stands now, in the file it is written in, or {@code null} for a table that is in no file. */
    private static @Nullable String pinOf(TableSyntaxNode table) {
        var file = fileOfTable(table);
        return file == null ? null : pinAt(file, table);
    }

    private static String pinAt(Path file, TableSyntaxNode table) {
        var region = table.getGridTable().getRegion();
        return file + "!" + table.getXlsSheetSourceCodeModule().getSheetName() + "!" + region.getTop() + "!"
                + region.getLeft();
    }

    /**
     * The module a table was compiled in.
     *
     * @param node a table, or anything else of a module
     * @return the module, or {@code null} for a table no module holds
     */
    public static @Nullable XlsModuleSyntaxNode moduleOf(ISyntaxNode node) {
        var parent = node.getParent();
        while (parent != null && !(parent instanceof XlsModuleSyntaxNode)) {
            parent = parent.getParent();
        }
        return (XlsModuleSyntaxNode) parent;
    }

    private static @Nullable Path fileOfTable(TableSyntaxNode table) {
        var workbook = workbookOf(table);
        return workbook == null ? null : fileOf(workbook);
    }

    /** The source the workbook of a table is read from, or {@code null} for a table read from no sheet. */
    private static @Nullable IOpenSourceCodeModule workbookOf(TableSyntaxNode table) {
        var sheet = table.getXlsSheetSourceCodeModule();
        return sheet == null ? null : sheet.getWorkbookSource().getSource();
    }

    private static @Nullable Path projectFolderOf(@Nullable IOpenSourceCodeModule source) {
        return source instanceof IProjectSourceCodeModule projectFile
                ? projectFile.getProjectFolder().toAbsolutePath().normalize()
                : null;
    }

    /** The file a source is read from, or {@code null} for one that is in no file. */
    private static @Nullable Path fileOf(IOpenSourceCodeModule source) {
        var uri = source.getFileUri();
        if (uri == null || !uri.startsWith("file:")) {
            return null;
        }
        try {
            return Path.of(URI.create(uri)).toAbsolutePath().normalize();
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            return null;
        }
    }

    /** The place of a file in its project, or the file itself outside any project. */
    private static String pathOf(Path file, @Nullable Path project) {
        return FileNameFormatter.normalizePath(project == null ? file : project.relativize(file));
    }
}
