package org.openl.rules.ui;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.util.conf.Version;

/**
 * @author Andrei Astrouski
 */
public class NewVersionTableCopier extends TablePropertyCopier {
    public NewVersionTableCopier(String tableUri) {
        super(tableUri, true);
    }

    @Override
    protected void doCopy() throws CreateTableException {
        super.doCopy();
        updateOriginalTable();
    }

    private void updateOriginalTable() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("active", "false");
        Version version = getOriginalVersion();
        if (version == null) {
            properties.put("version", INIT_VERSION);
        }
        updatePropertiesForOriginalTable(properties);
    }

    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> properties = super.buildProperties();
        properties.put("active", true);
        return properties;
    }

    @Override
    public String getName() {
        return "changeVersion";
    }

    public Version getOriginalVersion() {
        TableSyntaxNode originalNode = getCopyingTable();
        ITableProperties tableProperties = originalNode.getTableProperties();
        String version = tableProperties.getVersion();
        try {
            return Version.parseVersion(version, 0, "..");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public Version getMinNextVersion() {
        Version originalVersion = getOriginalVersion();
        if (originalVersion == null) {
            originalVersion = Version.parseVersion(INIT_VERSION, 0, "..");
        }
        originalVersion.setVariant(originalVersion.getVariant() + 1);
        return originalVersion;
    }

}
