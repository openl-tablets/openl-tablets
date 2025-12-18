package org.openl.rules.webstudio.web.servlet.wellknown;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceMetadataModel {
    @JsonProperty("resource")
    private String resource;
    @JsonProperty("authorization_servers")
    private List<String> authorizationServers;
    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public List<String> getAuthorizationServers() {
        return authorizationServers;
    }

    public void setAuthorizationServers(List<String> authorizationServers) {
        this.authorizationServers = authorizationServers;
    }

    public List<String> getScopesSupported() {
        return scopesSupported;
    }

    public void setScopesSupported(List<String> scopesSupported) {
        this.scopesSupported = scopesSupported;
    }
}