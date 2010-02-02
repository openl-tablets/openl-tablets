package org.openl.rules.ui;

import java.util.HashMap;
import java.util.Map;

import org.openl.meta.Version;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.xls.builder.CreateTableException;

/**
 * @author Andrei Astrouski
 */
public class NewVersionTableCopier extends TablePropertyCopier {

    public static final String INIT_VERSION = "0.0.1";

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
            return new Version(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Version getMinVersion() {
        Version nextVersion;
        Version originalVersion = getOriginalVersion();
        if (originalVersion != null) {
            nextVersion = new Version(originalVersion);
        } else {
            nextVersion = new Version(INIT_VERSION);
        }
        nextVersion.setVariant(nextVersion.getVariant() + 1);
        return nextVersion;
    }

}
