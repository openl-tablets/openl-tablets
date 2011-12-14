package org.openl.rules.ui.copy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.tableeditor.renderkit.TableProperty.TablePropertyBuilder;
import org.openl.util.conf.Version;

/**
 * @author Andrei Astrouski
 */
public class NewVersionTableCopier extends TableCopier {

    private static final String VERSION_PROP_NAME = "version";
    private static final String ACTIVE_PROP_NAME = "active";

    public NewVersionTableCopier(String tableUri) {
        super(tableUri);
        checkPropertiesExistance();
    }

    private void checkPropertiesExistance() {
        TableProperty versionProperty = super.getVersion();
        if (versionProperty == null) {
            // Property "version" is absent in base table
            versionProperty = new TablePropertyBuilder(VERSION_PROP_NAME,
                    TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(VERSION_PROP_NAME))
                    .displayName(TablePropertyDefinitionUtils.getPropertyDisplayName(VERSION_PROP_NAME))
                    .value(INIT_VERSION).build();
            getPropertiesManager().addProperty(versionProperty);
        }
    }

    @Override
    protected void doCopy() throws CreateTableException {
        super.doCopy();
        updateOriginalTable();
    }

    private void updateOriginalTable() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ACTIVE_PROP_NAME, "false");
        Version version = getOriginalVersion();
        if (version == null) {
            properties.put(VERSION_PROP_NAME, INIT_VERSION);
        }
        updatePropertiesForOriginalTable(properties);
    }

    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> properties = super.buildProperties();
        properties.put(ACTIVE_PROP_NAME, true);
        return properties;
    }

    public Version getOriginalVersion() {
        IOpenLTable originalTable = getCopyingTable();
        if (originalTable != null) {
            ITableProperties tableProperties = originalTable.getProperties();
            String version = tableProperties.getVersion();
            try {
                return Version.parseVersion(version, 0, "..");
            } catch (RuntimeException e) {
                return null;
            }
        } else {
            return null;
        }
        
    }

    @Override
    public Version getMinNextVersion() {
        Version originalVersion = getOriginalVersion();
        if (originalVersion == null) {
            originalVersion = Version.parseVersion(INIT_VERSION, 0, "..");
        }
        originalVersion.setVariant(originalVersion.getVariant() + 1);
        return originalVersion;
    }

    @Override
    public List<TableProperty> getPropertiesToDisplay() {
        List<TableProperty> properties = new ArrayList<TableProperty>();
        properties.add(getProperty(VERSION_PROP_NAME));
        return properties;
    }

}
