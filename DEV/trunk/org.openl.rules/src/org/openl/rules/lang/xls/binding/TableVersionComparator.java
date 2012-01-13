package org.openl.rules.lang.xls.binding;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.types.IOpenMethod;
import org.openl.util.conf.Version;

/**
 * Finds table with biggest version(it will later table) or "active" table;
 * 
 * @return -1 if the first later or "active", 1 if second later "active" and 0
 *         if tables are "inactive" and have similar versions
 */
public class TableVersionComparator implements Comparator<ITableProperties> {

    private static Log LOG = LogFactory.getLog(TableVersionComparator.class);

    public int compare(IOpenMethod first, IOpenMethod second) {
        if (!new DimensionPropertiesMethodKey(first).equals(new DimensionPropertiesMethodKey(second))) {
            throw new IllegalArgumentException("Uncomparable tables. Tasbles should have similar name,signature and dimension properties.");
        }
        return compare(PropertiesHelper.getTableProperties(first), PropertiesHelper.getTableProperties(second));
    }

    public int compare(TableSyntaxNode first, TableSyntaxNode second) {
        return compare(first.getTableProperties(), second.getTableProperties());
    }

    @Override
    public int compare(ITableProperties first, ITableProperties second) {
        if (first.getActive() != second.getActive()) {
            if (first.getActive()) {
                return -1;
            } else if (second.getActive()) {
                return 1;
            }
        }
        Version firstNodeVersion = parseVersionForComparison(first.getVersion());
        Version secondNodeVersion =parseVersionForComparison(second.getVersion());
        return secondNodeVersion.compareTo(firstNodeVersion);
    }

    private static Version DEFAULT_VERSION = Version.parseVersion("0.0.0",0, "..");
    private static Version parseVersionForComparison(String version){
        try {
            return Version.parseVersion(version, 0, "..");
        } catch (RuntimeException e) {
            // it is just fix to avoid tree crashing.
            // we need to validate format of the versions, during compilation of
            // Openl and also on UI.
            LOG.warn(e);
            return DEFAULT_VERSION;
        }
    }
}
