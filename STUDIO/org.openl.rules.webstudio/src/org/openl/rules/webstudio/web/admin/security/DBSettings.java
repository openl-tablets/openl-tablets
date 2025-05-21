package org.openl.rules.webstudio.web.admin.security;

import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.rest.settings.model.validation.DBConnectionConstraint;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;
import org.openl.rules.webstudio.web.admin.SettingsHolder;

@DBConnectionConstraint
public class DBSettings implements SettingsHolder {

    private static final String DB_URL = "db.url";
    private static final String DB_USER = "db.user";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_MAX_POOL_SIZE = "db.maximumPoolSize";

    @Parameter(description = "Database URL", example = "jdbc:postgresql://localhost:5432/studio")
    @SettingPropertyName(DB_URL)
    @NotBlank(message = "Database URL cannot be blank.")
    private String url;

    @Parameter(description = "Database user", example = "root")
    @SettingPropertyName(DB_USER)
    @Size(max = 100, message = "Username length must be less than 100.")
    private String user;

    @Parameter(description = "Database password", example = "password")
    @SettingPropertyName(value = DB_PASSWORD, secret = true)
    private String password;

    @Parameter(description = "Maximum pool size", example = "10")
    @SettingPropertyName(DB_MAX_POOL_SIZE)
    @Min(value = 1, message = "Maximum pool size must be greater than 0.")
    private Integer maximumPoolSize;

    @Override
    public void load(PropertiesHolder properties) {
        url = properties.getProperty(DB_URL);
        user = properties.getProperty(DB_USER);
        password = properties.getProperty(DB_PASSWORD);
        maximumPoolSize = Optional.ofNullable(properties.getProperty(DB_MAX_POOL_SIZE))
                .map(Integer::parseInt)
                .orElse(null);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(DB_URL, url);
        properties.setProperty(DB_USER, user);
        properties.setProperty(DB_PASSWORD, password);
        properties.setProperty(DB_MAX_POOL_SIZE, maximumPoolSize);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(DB_URL, DB_USER, DB_PASSWORD, DB_MAX_POOL_SIZE);
        load(properties);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

}
