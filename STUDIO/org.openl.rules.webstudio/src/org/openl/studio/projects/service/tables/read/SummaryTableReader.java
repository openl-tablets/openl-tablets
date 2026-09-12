package org.openl.studio.projects.service.tables.read;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.model.tables.LookupView;
import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.model.tables.SimpleSpreadsheetView;
import org.openl.studio.projects.model.tables.SmartRulesView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TestView;
import org.openl.studio.projects.model.tables.VocabularyView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.MethodKey;

/**
 * Reads any OpenL table to {@link SummaryTableView} model.
 *
 * @author Vladyslav Pikus
 */
@Component
public class SummaryTableReader extends TableReader<SummaryTableView, SummaryTableView.Builder> {

    public SummaryTableReader() {
        super(SummaryTableView::builder);
    }

    /**
     * Reads the table, telling its version from the other versions of the same table.
     *
     * <p>A table shares its signature with the other versions of itself: which of them answers a call is decided by
     * the dimension properties. The dictionary is what knows the versions of a signature, so a table read with it
     * carries the name that tells the versions apart and the signature they are grouped under.
     *
     * <p>A table that has no other version is read as it is read without a dictionary.
     */
    public SummaryTableView read(IOpenLTable openLTable, @Nullable OverloadedMethodsDictionary overloads) {
        var builder = SummaryTableView.builder();
        initialize(builder, openLTable);
        initializeVersion(builder, openLTable.getSyntaxNode(), overloads);
        return builder.build();
    }

    @Override
    protected void initialize(SummaryTableView.Builder builder, IOpenLTable table) {
        super.initialize(builder, table);

        var url = table.getUriParser();
        try {
            var file = new File("").getCanonicalFile()
                    .toPath()
                    .relativize(Path.of(url.getWbPath()))
                    .resolve(url.getWbName())
                    .toString();
            builder.file(file.replace("\\", "/"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve module location", e);
        }
        builder.sheet(url.getWsName());
        builder.pos(url.getRange());

        var tsn = table.getSyntaxNode();
        var member = tsn.getMember();
        if (member instanceof AMethod) {
            initializeMethodSignature(builder, tsn.getHeader());
        }

        // The `active` property is the one a reader is told about outside the properties themselves: a table
        // switched off is written in the module but answers nothing, and the tree draws it apart.
        var properties = table.getProperties();
        if (properties != null && Boolean.FALSE.equals(properties.getActive())) {
            builder.active(Boolean.FALSE);
        }

        if (OpenLTableUtils.isVocabularyTable(table)) {
            builder.tableType(VocabularyView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleSpreadsheet(table)) {
            builder.tableType(SimpleSpreadsheetView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleRules(table)) {
            builder.tableType(SimpleRulesView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSmartRules(table)) {
            builder.tableType(SmartRulesView.TABLE_TYPE);
        } else if (OpenLTableUtils.isTestTable(table)) {
            builder.tableType(TestView.TABLE_TYPE);
        } else if (OpenLTableUtils.isDataTable(table)) {
            builder.tableType(DataView.TABLE_TYPE);
        } else if (OpenLTableUtils.isSimpleLookup(table)) {
            builder.tableType(LookupView.SIMPLE_TABLE_TYPE);
        } else if (OpenLTableUtils.isSmartLookup(table)) {
            builder.tableType(LookupView.SMART_TABLE_TYPE);
        } else {
            builder.tableType(OpenLTableUtils.getTableTypeItems().get(table.getType()));
        }
    }

    /**
     * Names the version the table is, when the table has more than one.
     *
     * <p>Only a table that shares its signature with another carries a version name; a unique table would be
     * named by dimension properties nothing is told apart by.
     */
    private void initializeVersion(SummaryTableView.Builder builder,
                                   TableSyntaxNode tsn,
                                   @Nullable OverloadedMethodsDictionary overloads) {
        if (overloads == null || !(tsn.getMember() instanceof IOpenMethod method) || !overloads.contains(method)) {
            return;
        }
        var versions = overloads.getAllMethodOverloads(method);
        if (versions == null || versions.size() < 2) {
            return;
        }
        builder.overloadGroup(new MethodKey(method).toString());
        var formats = WebStudioFormats.getInstance();
        builder.displayName(TableSyntaxNodeUtils.getTableDisplayValue(tsn, 0, overloads, formats)[INamedThing.SHORT]);
    }

    private void initializeMethodSignature(SummaryTableView.Builder builder, HeaderSyntaxNode header) {
        var headerSource = header.getSourceString();
        var pos = ExecutableTableReader.rollWhitespaces(headerSource, 0);
        var start = pos;
        pos = ExecutableTableReader.rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table type
            builder.tableType(headerSource.substring(start, pos));
        }
        pos = ExecutableTableReader.rollWhitespaces(headerSource, pos);
        if (header.isCollect()) {
            // skip "Collect" keyword
            pos = ExecutableTableReader.rollIdentifier(headerSource, pos);
            pos = ExecutableTableReader.rollWhitespaces(headerSource, pos);
        }
        start = pos;
        pos = ExecutableTableReader.rollIdentifier(headerSource, pos);
        if (start < pos) {
            // it is probably table return type
            builder.returnType(headerSource.substring(start, pos));
        }
        pos = ExecutableTableReader.rollWhitespaces(headerSource, pos);
        builder.signature(headerSource.substring(pos));
    }
}
