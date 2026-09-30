package org.openl.rules.webstudio.service;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.PropertyResolver;

/**
 * The settings of one user, read at once.
 *
 * <p>A setting the user has not changed reads as its default from the configuration.
 *
 * <p>A setting with neither a stored value nor a usable default reads as {@code null}, and a warning names it. A
 * broken default costs that one setting, not the whole profile.
 *
 * @param stored   the values the user saved, by setting key
 * @param defaults the configuration the defaults come from
 * @author Yury Molchan
 */
@Slf4j
public record UserSettings(Map<String, String> stored, PropertyResolver defaults) {

    public @Nullable String getString(String key) {
        var value = stored.get(key);
        return value != null ? value : defaults.getProperty(key);
    }

    public @Nullable Boolean getBoolean(String key) {
        var value = getFilled(key);
        return value == null ? null : Boolean.valueOf(value);
    }

    public @Nullable Integer getInteger(String key) {
        var value = getFilled(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("The '{}' user setting holds '{}', which is not a number.", key, value);
            return null;
        }
    }

    private @Nullable String getFilled(String key) {
        var value = getString(key);
        if (value == null || value.isBlank()) {
            log.warn("The '{}' user setting has neither a value nor a default.", key);
            return null;
        }
        return value.trim();
    }
}
