/*
 * Created on Jun 16, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

package org.openl.rules.lang.xls.syntax;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import org.openl.meta.StringValue;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.syntax.impl.NaryNode;
import org.openl.types.IOpenMember;
import org.openl.util.HashingUtils;

/**
 * @author snshor
 */
public class TableSyntaxNode extends NaryNode {
    public static final TableSyntaxNode[] EMPTY_ARRAY = new TableSyntaxNode[0];

    @Getter
    private ILogicalTable table;

    private final HeaderSyntaxNode headerNode;

    @Getter
    @Setter
    private ITableProperties tableProperties;

    @Getter
    @Setter
    private IOpenMember member;

    @Getter
    private final Map<String, ILogicalTable> subTables = new HashMap<>();

    @Getter
    @Setter
    private Object validationResult;

    @Getter
    @Setter
    private MetaInfoReader metaInfoReader = EmptyMetaInfoReader.getInstance();

    /**
     * Read without a lock: the record is immutable, so its final fields are seen whole, and a reader that misses it
     * asks {@link TableIds}, which numbers the module under its lock.
     */
    private TableIds.Numbered numbered;

    /** The version of the table, computed when it is first asked for. */
    private final AtomicReference<String> version = new AtomicReference<>();

    public TableSyntaxNode(String type,
                           GridLocation pos,
                           XlsSheetSourceCodeModule module,
                           IGridTable gridTable,
                           HeaderSyntaxNode header) {
        super(type, pos, null, module);
        table = LogicalTableHelper.logicalTable(gridTable);
        headerNode = header;
        header.setParent(this);
    }

    public void setTable(IGridTable gridTable) {
        table = LogicalTableHelper.logicalTable(gridTable);
    }

    public String getDisplayName() {
        return table.getSource().getCell(0, 0).getStringValue();
    }

    public GridLocation getGridLocation() {
        return (GridLocation) getLocation();
    }

    public HeaderSyntaxNode getHeader() {
        return headerNode;
    }

    public StringValue getHeaderLineValue() {
        var value = table.getSource().getCell(0, 0).getStringValue();

        if (value == null) {
            return new StringValue("");
        }
        return new StringValue(value, value, value, new GridCellSourceCodeModule(table.getSource(), 0, 0, null));
    }

    public @Nullable ILogicalTable getTable(String view) {
        return subTables.get(view);
    }

    public IGridTable getGridTable() {
        return table.getSource();
    }

    /**
     * The version of the table: a digest of the text of its cells.
     *
     * <p>The cells of a compiled table are read from the workbook as the module was compiled, so the version stays
     * the same until the module is compiled again or the table is written, and it is computed once. A table changed
     * in any way, in OpenL Studio or on disk, is a table of a new compilation with a version of its own.
     */
    public String getVersion() {
        var known = version.get();
        if (known == null) {
            known = digestOf(getGridTable());
            version.compareAndSet(null, known);
        }
        return known;
    }

    /**
     * Says that the table was written since the module was compiled.
     *
     * <p>A module is not always compiled again after a write, and until it is, the table keeps the cells it was
     * compiled from. The table takes a new version at once: a write based on the version read before this write is
     * told apart from a write based on the version read after it.
     */
    public void markWritten() {
        version.set(HashingUtils.sha256Hex("written " + getVersion()));
    }

    /** The digest of the text of the cells, read a cell at a time: a table of any size is never held as one text. */
    private static String digestOf(IGridTable grid) {
        return HashingUtils.sha256Hex(text -> {
            for (var row = 0; row < grid.getHeight(); row++) {
                for (var column = 0; column < grid.getWidth(); column++) {
                    text.accept(String.valueOf(grid.getCell(column, row).getStringValue()));
                    text.accept("\t");
                }
                text.accept("\n");
            }
        });
    }

    /**
     * Gets the table body without header and properties section.
     *
     * @return table body, without header and properties section (if exists).
     */
    public @Nullable ILogicalTable getTableBody() {
        int startRow = hasPropertiesDefinedInTable() ? 2 : 1;

        if (table.getHeight() <= startRow) {
            return null;
        }
        return table.getRows(startRow);
    }

    public String getUri() {
        return getGridTable().getUri();
    }

    public XlsUrlParser getUriParser() {
        return getGridTable().getUriParser();
    }

    /**
     * The id of the table, made of what the table is rather than of where it stands.
     *
     * <p>The id is kept while the table moves, on its sheet or to another sheet of its workbook, and changes when the
     * table is renamed. See {@link TableIds}.
     *
     * @return the id of the table
     */
    public String getId() {
        var given = numbered;
        return given != null ? given.id() : TableIds.idOf(this);
    }

    /** What the table is and its number among the tables sharing it, or {@code null} until its module is numbered. */
    TableIds.Numbered numbered() {
        return numbered;
    }

    void number(TableIds.Numbered numbered) {
        this.numbered = numbered;
    }

    public XlsSheetSourceCodeModule getXlsSheetSourceCodeModule() {
        return (XlsSheetSourceCodeModule) getModule();
    }

    /**
     * Checks if <code>{@link TableSyntaxNode}</code> has properties that were physically defined in appropriate table
     * in data source. <br>
     * Properties set by default are ignoring.
     *
     * @return <code>TRUE</code> if <code>{@link TableSyntaxNode}</code> has properties that were physically defined in
     * appropriate table in data source.
     */
    public boolean hasPropertiesDefinedInTable() {
        return tableProperties != null && tableProperties.getPropertiesSection() != null;
    }

    public boolean isExecutableNode() {
        if (getMember() != null) {
            Class<?> memberClass = getMember().getClass();
            var annotations = memberClass.getAnnotations();

            for (Annotation annotation : annotations) {
                if (annotation instanceof Executable) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Use this method instead of {@link #getType()}. Returns the enum constant for the current node.
     *
     * @return the {@link XlsNodeTypes} for current TableSyntaxNode
     */
    public XlsNodeTypes getNodeType() {
        return XlsNodeTypes.getEnumByValue(getType());
    }
}
