package org.openl.rules.webstudio.web.install;

public class SAMLSettings {
    private String webStudioUrl;
    private String entityId;
    private String samlServerMetadataUrl;
    private int requestTimeout;
    private final String keystoreFilePath;
    private final String keystorePassword;
    private final String keystoreSpAlias;
    private final String keystoreSpPassword;
    private String usernameAttribute;
    private String firstNameAttribute;
    private String secondNameAttribute;
    private String displayNameAttribute;
    private String emailAttribute;
    private String groupsAttribute;
    private String authenticationContexts;
    private boolean localLogout;
    private String samlScheme;
    private String samlServerName;
    private int serverPort;
    private boolean includeServerPortInRequestUrl;
    private String contextPath;
    private int maxAuthenticationAge;
    private boolean metadataTrustCheck;
    private boolean isAppAfterBalancer;
    private String serverCertificate;

    public SAMLSettings(String webStudioUrl,
            String entityId,
            String samlServerMetadataUrl,
            int requestTimeout,
            String keystoreFilePath,
            String keystorePassword,
            String keystoreSpAlias,
            String keystoreSpPassword,
            String usernameAttribute,
            String firstNameAttribute,
            String secondNameAttribute,
            String displayNameAttribute,
            String emailAttribute,
            String groupsAttribute,
            String authenticationContexts,
            boolean localLogout,
            String samlScheme,
            String samlServerName,
            int serverPort,
            boolean includeServerPortInRequestUrl,
            String contextPath,
            int maxAuthenticationAge,
            boolean metadataTrustCheck,
            boolean isAppAfterBalancer,
            String serverCertificate) {
        this.webStudioUrl = webStudioUrl;
        this.entityId = entityId;
        this.samlServerMetadataUrl = samlServerMetadataUrl;
        this.requestTimeout = requestTimeout;
        this.keystoreFilePath = keystoreFilePath;
        this.keystoreSpAlias = keystoreSpAlias;
        this.keystorePassword = keystorePassword;
        this.keystoreSpPassword = keystoreSpPassword;
        this.usernameAttribute = usernameAttribute;
        this.firstNameAttribute = firstNameAttribute;
        this.secondNameAttribute = secondNameAttribute;
        this.groupsAttribute = groupsAttribute;
        this.authenticationContexts = authenticationContexts;
        this.localLogout = localLogout;
        this.samlScheme = samlScheme;
        this.samlServerName = samlServerName;
        this.serverPort = serverPort;
        this.includeServerPortInRequestUrl = includeServerPortInRequestUrl;
        this.contextPath = contextPath;
        this.maxAuthenticationAge = maxAuthenticationAge;
        this.metadataTrustCheck = metadataTrustCheck;
        this.isAppAfterBalancer = isAppAfterBalancer;
        this.serverCertificate = serverCertificate;
        this.displayNameAttribute = displayNameAttribute;
        this.emailAttribute = emailAttribute;
    }

    public String getDisplayNameAttribute() {
        return displayNameAttribute;
    }

    public void setDisplayNameAttribute(String displayNameAttribute) {
        this.displayNameAttribute = displayNameAttribute;
    }

    public String getEmailAttribute() {
        return emailAttribute;
    }

    public void setEmailAttribute(String emailAttribute) {
        this.emailAttribute = emailAttribute;
    }

    public String getWebStudioUrl() {
        return webStudioUrl;
    }

    public void setWebStudioUrl(String webStudioUrl) {
        this.webStudioUrl = webStudioUrl;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSamlServerMetadataUrl() {
        return samlServerMetadataUrl;
    }

    public void setSamlServerMetadataUrl(String samlServerMetadataUrl) {
        this.samlServerMetadataUrl = samlServerMetadataUrl;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getKeystoreFilePath() {
        return keystoreFilePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeystoreSpAlias() {
        return keystoreSpAlias;
    }

    public String getKeystoreSpPassword() {
        return keystoreSpPassword;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    public String getFirstNameAttribute() {
        return firstNameAttribute;
    }

    public void setFirstNameAttribute(String firstNameAttribute) {
        this.firstNameAttribute = firstNameAttribute;
    }

    public String getSecondNameAttribute() {
        return secondNameAttribute;
    }

    public void setSecondNameAttribute(String secondNameAttribute) {
        this.secondNameAttribute = secondNameAttribute;
    }

    public String getGroupsAttribute() {
        return groupsAttribute;
    }

    public void setGroupsAttribute(String groupsAttribute) {
        this.groupsAttribute = groupsAttribute;
    }

    public String getAuthenticationContexts() {
        return authenticationContexts;
    }

    public void setAuthenticationContexts(String authenticationContexts) {
        this.authenticationContexts = authenticationContexts;
    }

    public boolean isLocalLogout() {
        return localLogout;
    }

    public void setLocalLogout(boolean localLogout) {
        this.localLogout = localLogout;
    }

    public String getSamlScheme() {
        return samlScheme;
    }

    public void setSamlScheme(String samlScheme) {
        this.samlScheme = samlScheme;
    }

    public String getSamlServerName() {
        return samlServerName;
    }

    public void setSamlServerName(String samlServerName) {
        this.samlServerName = samlServerName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public int getMaxAuthenticationAge() {
        return maxAuthenticationAge;
    }

    public void setMaxAuthenticationAge(int maxAuthenticationAge) {
        this.maxAuthenticationAge = maxAuthenticationAge;
    }

    public boolean isMetadataTrustCheck() {
        return metadataTrustCheck;
    }

    public void setMetadataTrustCheck(boolean metadataTrustCheck) {
        this.metadataTrustCheck = metadataTrustCheck;
    }

    public boolean isIncludeServerPortInRequestUrl() {
        return includeServerPortInRequestUrl;
    }

    public void setIncludeServerPortInRequestUrl(boolean includeServerPortInRequestUrl) {
        this.includeServerPortInRequestUrl = includeServerPortInRequestUrl;
    }

    public boolean isAppAfterBalancer() {
        return isAppAfterBalancer;
    }

    public void setAppAfterBalancer(boolean appAfterBalancer) {
        isAppAfterBalancer = appAfterBalancer;
    }

    public String getServerCertificate() {
        return serverCertificate;
    }

    public void setServerCertificate(String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }
}
