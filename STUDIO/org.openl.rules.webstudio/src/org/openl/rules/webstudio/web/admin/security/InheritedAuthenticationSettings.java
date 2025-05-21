package org.openl.rules.webstudio.web.admin.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

public class InheritedAuthenticationSettings extends AuthenticationSettings {

    private static final String SEP = ",";

    private static final String ADMINISTRATORS = "security.administrators";
    private static final String DEFAULT_GROUP = "security.default-group";
    private static final String ALLOW_PROJECT_CREATE_DELETE = "security.allow-project-create-delete";

    @Valid
    @NotNull
    private DBSettings db;

    @Parameter(description = "List of users with administrator privileges")
    @SettingPropertyName(ADMINISTRATORS)
    @Valid
    @NotEmpty
    private Set<@NotBlank @Size(max = 50) String> administrators;

    @Parameter(description = "Default group for any authenticated user")
    @SettingPropertyName(DEFAULT_GROUP)
    private String defaultGroup;

    @Parameter(description = "Global permission to create and delete projects")
    @SettingPropertyName(ALLOW_PROJECT_CREATE_DELETE)
    private boolean allowProjectCreateDelete;

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        db = new DBSettings();
        db.load(properties);
        administrators = Optional.ofNullable(properties.getProperty(ADMINISTRATORS))
                .map(s -> Stream.of(s.split(SEP))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet()))
                .filter(CollectionUtils::isNotEmpty)
                .orElse(null);
        defaultGroup = properties.getProperty(DEFAULT_GROUP);
        allowProjectCreateDelete = Optional.ofNullable(properties.getProperty(ALLOW_PROJECT_CREATE_DELETE))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        db.store(properties);
        properties.setProperty(ADMINISTRATORS, String.join(SEP, administrators));
        properties.setProperty(DEFAULT_GROUP, defaultGroup);
        properties.setProperty(ALLOW_PROJECT_CREATE_DELETE, allowProjectCreateDelete);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        db.revert(properties);
        properties.revertProperties(ADMINISTRATORS, DEFAULT_GROUP, ALLOW_PROJECT_CREATE_DELETE);
        super.revert(properties);
    }

    public DBSettings getDb() {
        return db;
    }

    public void setDb(DBSettings db) {
        this.db = db;
    }

    public Set<String> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(Set<String> administrators) {
        this.administrators = administrators;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public boolean isAllowProjectCreateDelete() {
        return allowProjectCreateDelete;
    }

    public void setAllowProjectCreateDelete(boolean allowProjectCreateDelete) {
        this.allowProjectCreateDelete = allowProjectCreateDelete;
    }
}
