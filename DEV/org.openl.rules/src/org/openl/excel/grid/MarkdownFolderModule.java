package org.openl.excel.grid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.binding.impl.NumericStringComparator;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.util.FileTypeHelper;

/**
 * A module written as a folder of Markdown files, a table to a file.
 *
 * <p>Every Markdown file of the folder and of its sub-folders is a workbook of the module, read in the order of the
 * paths, a number in a name by its value as a file manager sorts: {@code rate2.md} before {@code rate10.md}, and
 * {@code rate.md} before {@code rate1.md}. A table lies on a sheet named after the folder its file is in, so a
 * sub-folder groups its tables.
 *
 * <p>The tables that set up the whole module are written once, each in a file of a fixed name at the root of the
 * folder: the Environment table in {@code Environment.md}, the Properties table in {@code Properties.md} and the
 * Constants table in {@code Constants.md}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownFolderModule {

    private static final List<ModuleTable> MODULE_TABLES = List.of(
            new ModuleTable(XlsNodeTypes.XLS_ENVIRONMENT, "Environment"),
            new ModuleTable(XlsNodeTypes.XLS_PROPERTIES, "Properties"),
            new ModuleTable(XlsNodeTypes.XLS_CONSTANTS, "Constants"));

    /** The order the files of a folder are read in, the same on every file system. */
    private static final Comparator<Path> READING_ORDER = Comparator.comparing(
            path -> path.toString().replace('\\', '/'), NumericStringComparator.INSTANCE);

    /**
     * Returns the file or the folder of the file system a source is read from, or {@code null} for any other source.
     */
    static @Nullable Path pathOf(IOpenSourceCodeModule source) {
        try {
            return Path.of(new URI(source.getFileUri())).toAbsolutePath().normalize();
        } catch (Exception e) {
            // Not a file of the file system: an archive entry, a virtual module
            return null;
        }
    }

    /**
     * Returns the Markdown files of a module folder, each known by its place under the address of the module.
     *
     * <p>The folder can lie in the file system of an archive, as a deployment read in place.
     */
    static List<ModuleFile> files(IOpenSourceCodeModule module, Path folder) {
        var uri = module.getUri();
        var base = uri.endsWith("/") ? uri : uri + "/";
        try (var paths = Files.find(folder,
                Integer.MAX_VALUE,
                (path, attributes) -> attributes.isRegularFile() && FileTypeHelper.isMarkdownFile(path.toString()))) {
            return paths.sorted(READING_ORDER).map(path -> {
                var inFolder = folder.relativize(path).toString().replace('\\', '/');
                return new ModuleFile(inFolder, path, new FileSourceCodeModule(path, base + uriPath(inFolder)));
            }).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a relative path as a URI path, escaping what a URI cannot hold, such as spaces.
     */
    private static String uriPath(String path) {
        try {
            return new URI(null, null, path, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the errors of the tables of a file that set up the module but are not written where they belong.
     *
     * @param path     the path of the file in the module folder
     * @param workbook the tables read from the file
     */
    static List<SyntaxNodeException> misplacedTables(String path, WorkbookSyntaxNode workbook) {
        var reserved = MODULE_TABLES.stream().filter(table -> table.file().equals(path)).findFirst().orElse(null);
        return Arrays.stream(workbook.getTableSyntaxNodes())
                .flatMap(table -> misplaced(reserved, table).stream())
                .toList();
    }

    /**
     * Returns the error of a table written in a file it does not belong in.
     *
     * @param reserved the module table the file is reserved for, or {@code null} for a file of any other name
     */
    private static Optional<SyntaxNodeException> misplaced(@Nullable ModuleTable reserved, TableSyntaxNode table) {
        var kind = MODULE_TABLES.stream()
                .filter(moduleTable -> moduleTable.type() == table.getNodeType())
                .findFirst()
                .orElse(null);
        if (kind != null && kind != reserved) {
            return Optional.of(SyntaxNodeExceptionUtils.createError(
                    "The %s table of a module is written in %s at the root of the module folder".formatted(
                            kind.name(), kind.file()),
                    table));
        }
        if (reserved != null && kind != reserved) {
            return Optional.of(SyntaxNodeExceptionUtils.createError(
                    "%s holds the %s table of the module and nothing else".formatted(reserved.file(),
                            reserved.name()),
                    table));
        }
        return Optional.empty();
    }

    /**
     * A Markdown file of a module folder.
     *
     * @param path   the path of the file in the folder, written with {@code /}
     * @param file   the file in the file system
     * @param source the file, known by its place under the address of the module
     */
    record ModuleFile(String path, Path file, IOpenSourceCodeModule source) {
    }

    /**
     * A kind of table written once for the whole module, and the file it is written in.
     */
    private record ModuleTable(XlsNodeTypes type, String name) {
        String file() {
            return name + ".md";
        }
    }

    /**
     * A file of a module folder, known by the address of the module followed by the path of the file in the folder.
     *
     * <p>The address of the module is the one its tables are told apart by, so the tables of a file keep the same
     * address wherever the project is.
     */
    private static final class FileSourceCodeModule extends PathSourceCodeModule {

        private final String uri;

        private FileSourceCodeModule(Path file, String uri) {
            super(file);
            this.uri = uri;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public String getFileUri() {
            return super.getUri();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FileSourceCodeModule that && uri.equals(that.uri);
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }
}
