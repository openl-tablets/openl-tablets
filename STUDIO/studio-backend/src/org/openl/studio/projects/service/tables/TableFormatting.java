package org.openl.studio.projects.service.tables;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.web.admin.AdministrationSettings;

/**
 * Whether OpenL Studio formats a table with the OpenL table style when it is saved, as the administrator sets it.
 *
 * @author Yury Molchan
 */
@Component
@RequiredArgsConstructor
public class TableFormatting {

    private final Environment environment;

    /**
     * Whether every table saved, created, copied or uploaded is formatted. Read on every save, so a change of the
     * setting applies to the next save.
     *
     * @return {@code true} unless the administrator turned formatting on save off
     */
    public boolean onSave() {
        return environment.getProperty(AdministrationSettings.FORMAT_TABLES_ON_SAVE, Boolean.class, true);
    }
}
