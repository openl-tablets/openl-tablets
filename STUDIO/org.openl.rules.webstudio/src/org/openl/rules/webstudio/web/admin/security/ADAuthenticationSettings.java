package org.openl.rules.webstudio.web.admin.security;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.studio.settings.model.constraint.ADConnectionConstraint;

@ADConnectionConstraint
@Schema(allOf = AuthenticationSettings.class)
public class ADAuthenticationSettings extends InheritedAuthenticationSettings {

    private static final String DOMAIN = "security.ad.domain";
    private static final String SERVER_URL = "security.ad.server-url";
    private static final String SEARCH_FILTER = "security.ad.search-filter";
    private static final String GROUP_FILTER = "security.ad.group-filter";

    @Getter
    @Parameter(description = "Default domain against which a user is logged in.", example = "example.com")
    @Setter
    @SettingPropertyName(DOMAIN)
    @NotBlank
    private String domain;

    @Getter
    @Parameter(description = "LDAP server URL.", example = "ldap://ldap.example.com:3268")
    @Setter
    @SettingPropertyName(SERVER_URL)
    @NotBlank
    private String serverUrl;

    @Getter
    @Parameter(description = "Filter for searching for a logged in user in the LDAP system.", example = "(uid={0})")
    @Setter
    @SettingPropertyName(SEARCH_FILTER)
    @NotBlank
    private String searchFilter;

    @Getter
    @Parameter(description = "Filter for searching for a group in the LDAP system.", example = "(member={0})")
    @Setter
    @SettingPropertyName(GROUP_FILTER)
    private String groupFilter;

    @Getter
    @Parameter(description = "Base64 encoded credentials for checking the connection to the LDAP server.")
    @Setter
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Credentials credentials;

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        domain = properties.getProperty(DOMAIN);
        serverUrl = properties.getProperty(SERVER_URL);
        searchFilter = properties.getProperty(SEARCH_FILTER);
        groupFilter = properties.getProperty(GROUP_FILTER);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        properties.setProperty(DOMAIN, domain);
        properties.setProperty(SERVER_URL, serverUrl);
        properties.setProperty(SEARCH_FILTER, searchFilter);
        properties.setProperty(GROUP_FILTER, groupFilter);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(DOMAIN,
                SERVER_URL,
                SEARCH_FILTER,
                GROUP_FILTER);
        super.revert(properties);
    }
}
