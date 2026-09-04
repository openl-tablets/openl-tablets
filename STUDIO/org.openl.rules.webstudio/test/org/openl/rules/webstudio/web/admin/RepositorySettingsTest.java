package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;

class RepositorySettingsTest {

    private static final String CONFIG_PREFIX = "repository.design";
    private static final String INCLUDE_EXCEL_FILES =
            CONFIG_PREFIX + ".project-discovery.include-excel-files";

    @Test
    void includesExcelFilesInProjectDiscoveryByDefault() {
        var properties = mock(PropertiesHolder.class);

        var settings = new LocalRepositorySettings(properties, CONFIG_PREFIX, RepositoryMode.DESIGN);

        assertTrue(settings.isIncludeExcelFilesInProjectDiscovery());
    }

    @Test
    void loadsDisabledExcelFileProjectDiscovery() {
        var properties = mock(PropertiesHolder.class);
        when(properties.getProperty(INCLUDE_EXCEL_FILES)).thenReturn("false");

        var settings = new LocalRepositorySettings(properties, CONFIG_PREFIX, RepositoryMode.DESIGN);

        assertFalse(settings.isIncludeExcelFilesInProjectDiscovery());
    }

    @Test
    void storesExcelFileProjectDiscoverySetting() {
        var properties = mock(PropertiesHolder.class);
        var settings = new LocalRepositorySettings(properties, CONFIG_PREFIX, RepositoryMode.DESIGN);
        settings.setIncludeExcelFilesInProjectDiscovery(false);

        settings.store(properties);

        verify(properties).setProperty(INCLUDE_EXCEL_FILES, false);
    }
}
