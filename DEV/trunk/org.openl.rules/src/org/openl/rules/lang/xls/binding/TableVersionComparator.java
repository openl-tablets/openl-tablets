package org.openl.rules.lang.xls.binding;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.util.conf.Version;

/**
 * Finds table with biggest version(it will later table) or "active" table;
 * 
 * @return -1 if the first later or "active", 1 if second later "active" and 0
 *         if tables are "inactive" and have similar versions
 */
public class TableVersionComparator implements Comparator<ExecutableRulesMethod> {

    private static Log LOG = LogFactory.getLog(TableVersionComparator.class);

    @Override
    public int compare(ExecutableRulesMethod first, ExecutableRulesMethod second) {
        if (!new DimensionPropertiesMethodKey(first).equals(new DimensionPropertiesMethodKey(second))) {
            throw new IllegalArgumentException("Uncomparable tables. Tasbles should have similar name,signature and dimension properties.");
        }
        if (first.getMethodProperties().getActive() != null) {
            if (first.getMethodProperties().getActive()) {
                return -1;
            } else if (second.getMethodProperties().getActive()) {
                return 1;
            }
        } else {
            return 0;
        }
        try {
            Version firstNodeVersion = Version.parseVersion(first.getMethodProperties().getVersion(), 0, "..");
            Version secondNodeVersion = Version.parseVersion(second.getMethodProperties().getVersion(), 0, "..");
            return secondNodeVersion.compareTo(firstNodeVersion);
        } catch (RuntimeException e) {
            // it is just fix to avoid tree crashing.
            // we need to validate format of the versions, during compilation of
            // Openl and also on UI.
            LOG.error(e);
        }
        return 0;
    }

}
