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
public class VersionPropertyTableCopier extends TableCopier {

    private static final String VERSION_DELIMETER = "..";
    private static final String VERSION_PROP_NAME = "version";
    private static final String ACTIVE_PROP_NAME = "active";

    public VersionPropertyTableCopier(IOpenLTable table) {
        super(table);
        checkPropertiesExistance();
    }

    public Version getOriginalVersion() {
        // get the version of copying table
        //
        Version tableVersion = getTableCurrentVersion();
        if (tableVersion == null) {
            // specify first version if it was not previously defined
            //
            tableVersion = Version.parseVersion(INIT_VERSION, 0, VERSION_DELIMETER);
        }
        return tableVersion;
    }

    @Override
    public Version getMinNextVersion() {
        Version originalVersion = getOriginalVersion();
        originalVersion.setVariant(originalVersion.getVariant() + 1);
        return originalVersion;
    }

    @Override
    public List<TableProperty> getPropertiesToDisplay() {
        List<TableProperty> properties = new ArrayList<>();
        TableProperty versionProperty = getProperty(VERSION_PROP_NAME);
        if (versionProperty != null) {
            // set next min value for version property
            //
            versionProperty.setValue(getMinNextVersion().toString());
        }
        properties.add(versionProperty);
        return properties;
    }

    private void checkPropertiesExistance() {
        TableProperty versionProperty = super.getVersion();
        if (versionProperty == null) {
            // Property "version" is absent in base table
            versionProperty = new TablePropertyBuilder(VERSION_PROP_NAME,
                TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(VERSION_PROP_NAME))
                    .displayName(TablePropertyDefinitionUtils.getPropertyDisplayName(VERSION_PROP_NAME))
                    .value(getOriginalVersion().toString())
                    .build();
            getPropertiesManager().addProperty(versionProperty);
        }
    }

    @Override
    protected void doCopy() throws CreateTableException {
        super.doCopy();
        updateOriginalTable();
    }

    private void updateOriginalTable() {
        Map<String, Object> properties = new HashMap<>();
        // set original table property 'active' to false
        //
        properties.put(ACTIVE_PROP_NAME, "false");

        // reset the original version value (should be done for table that didn`t have
        // this property before)
        //
        Version version = getOriginalVersion();
        properties.put(VERSION_PROP_NAME, version.toString());

        updatePropertiesForOriginalTable(properties);
    }

    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> properties = super.buildProperties();
        properties.put(ACTIVE_PROP_NAME, true);
        return properties;
    }

    private Version getTableCurrentVersion() {
        IOpenLTable copyingTable = getCopyingTable();
        if (copyingTable != null) {
            ITableProperties tableProperties = copyingTable.getProperties();
            String version = tableProperties.getVersion();
            try {
                return Version.parseVersion(version, 0, VERSION_DELIMETER);
            } catch (RuntimeException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
