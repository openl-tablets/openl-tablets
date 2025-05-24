package org.openl.rules.webstudio.web.admin.security;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.rest.settings.model.validation.ADConnectionConstraint;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;

@ADConnectionConstraint
public class ADAuthenticationSettings extends InheritedAuthenticationSettings {

    private static final String DOMAIN = "security.ad.domain";
    private static final String SERVER_URL = "security.ad.server-url";
    private static final String SEARCH_FILTER = "security.ad.search-filter";
    private static final String GROUP_FILTER = "security.ad.group-filter";

    @Parameter(description = "Default domain against which a user is logged in.", example = "example.com")
    @SettingPropertyName(DOMAIN)
    @NotBlank
    private String domain;

    @Parameter(description = "LDAP server URL.", example = "ldap://ldap.example.com:3268")
    @SettingPropertyName(SERVER_URL)
    @NotBlank
    private String serverUrl;

    @Parameter(description = "Filter for searching for a logged in user in the LDAP system.", example = "(uid={0})")
    @SettingPropertyName(SEARCH_FILTER)
    @NotBlank
    private String searchFilter;

    @Parameter(description = "Filter for searching for a group in the LDAP system.", example = "(member={0})")
    @SettingPropertyName(GROUP_FILTER)
    private String groupFilter;

    @Parameter(description = "Base64 encoded credentials for checking the connection to the LDAP server.")
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getGroupFilter() {
        return groupFilter;
    }

    public void setGroupFilter(String groupFilter) {
        this.groupFilter = groupFilter;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
}
