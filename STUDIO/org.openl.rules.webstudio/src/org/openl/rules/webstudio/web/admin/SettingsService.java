package org.openl.rules.webstudio.web.admin;

import java.io.IOException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface SettingsService {

    /**
     * Load settings
     *
     * @param settings settings to load
     */
    void load(@NotNull SettingsHolder settings);

    /**
     * Save settings to the memory storage
     *
     * @param settings settings to save
     */
    void store(@Valid SettingsHolder settings);

    /**
     * Revert all settings to the default values
     *
     * @param settings settings to reset
     */
    void revert(@NotNull SettingsHolder settings);

    /**
     * Apply all saved settings to the application
     *
     * @throws IOException if an I/O error occurs
     */
    void commit() throws IOException;

}
