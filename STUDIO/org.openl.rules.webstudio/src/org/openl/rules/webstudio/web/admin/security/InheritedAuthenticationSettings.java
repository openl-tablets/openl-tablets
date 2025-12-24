package org.openl.rules.webstudio.web.admin.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.config.PropertiesHolder;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

@Schema(allOf = AuthenticationSettings.class)
public class InheritedAuthenticationSettings extends AuthenticationSettings {

    private static final String SEP = ",";

    private static final String ADMINISTRATORS = "security.administrators";
    public static final String DEFAULT_GROUP = "security.default-group";

    @Parameter(description = "List of users with administrator privileges")
    @SettingPropertyName(ADMINISTRATORS)
    @Valid
    @NotEmpty
    private Set<@NotBlank @Size(max = 50) String> administrators;

    @Parameter(description = "Default group for any authenticated user")
    @SettingPropertyName(DEFAULT_GROUP)
    @Size(max = 65)
    private String defaultGroup;

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        administrators = Optional.ofNullable(properties.getProperty(ADMINISTRATORS))
                .map(s -> Stream.of(s.split(SEP))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet()))
                .filter(CollectionUtils::isNotEmpty)
                .orElse(null);
        defaultGroup = properties.getProperty(DEFAULT_GROUP);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        properties.setProperty(ADMINISTRATORS, String.join(SEP, administrators));
        properties.setProperty(DEFAULT_GROUP, defaultGroup);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(ADMINISTRATORS, DEFAULT_GROUP);
        super.revert(properties);
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
}
