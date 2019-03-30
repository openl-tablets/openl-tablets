package org.openl.rules.lang.xls.binding;

import java.util.Comparator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.types.IOpenMethod;
import org.openl.util.conf.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds table with biggest version(it will later table) or "active" table;
 *
 * @return -1 if the first later or "active", 1 if second later "active" and 0 if tables are "inactive" and have similar
 *         versions
 */
public final class TableVersionComparator implements Comparator<ITableProperties> {

    private static final TableVersionComparator INSTANCE = new TableVersionComparator();

    public static TableVersionComparator getInstance() {
        return INSTANCE;
    }

    private TableVersionComparator() {
    }

    public int compare(IOpenMethod first, IOpenMethod second) {
        if (!new DimensionPropertiesMethodKey(first).equals(new DimensionPropertiesMethodKey(second))) {
            throw new IllegalArgumentException(
                "Uncomparable tables. Tables should have similar name, signature and dimension properties.");
        }
        return compare(PropertiesHelper.getTableProperties(first), PropertiesHelper.getTableProperties(second));
    }

    public int compare(TableSyntaxNode first, TableSyntaxNode second) {
        return compare(first.getTableProperties(), second.getTableProperties());
    }

    @Override
    public int compare(ITableProperties first, ITableProperties second) {
        Boolean firstActive = first.getActive() == null || first.getActive();
        Boolean secondActive = second.getActive() == null || second.getActive();

        if (firstActive != secondActive) {
            return secondActive.compareTo(firstActive);
        }
        // Case when both tables have the same active status
        //
        Version firstNodeVersion = parseVersionForComparison(first.getVersion());
        Version secondNodeVersion = parseVersionForComparison(second.getVersion());
        return secondNodeVersion.compareTo(firstNodeVersion);
    }

    private static final Version DEFAULT_VERSION = Version.parseVersion("0.0.0", 0, "..");

    private static Version parseVersionForComparison(String version) {
        if (version == null) {
            return DEFAULT_VERSION;
        }
        try {
            return Version.parseVersion(version, 0, "..");
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(TableVersionComparator.class);
            // it is just fix to avoid tree crashing.
            // we need to validate format of the versions, during compilation of
            // Openl and also on UI.
            log.debug("Failed to parse version: [{}]", version);
            return DEFAULT_VERSION;
        }
    }
}
