package org.openl.rules.webstudio.web.install;

public class SAMLSettings {
    private String entityId;
    private String samlServerMetadataUrl;
    private String usernameAttribute;
    private String firstNameAttribute;
    private String secondNameAttribute;
    private String displayNameAttribute;
    private String emailAttribute;
    private String groupsAttribute;
    private String serverCertificate;

    public SAMLSettings(String entityId,
            String samlServerMetadataUrl,
            String usernameAttribute,
            String firstNameAttribute,
            String secondNameAttribute,
            String displayNameAttribute,
            String emailAttribute,
            String groupsAttribute,
            String serverCertificate) {
        this.entityId = entityId;
        this.samlServerMetadataUrl = samlServerMetadataUrl;
        this.usernameAttribute = usernameAttribute;
        this.firstNameAttribute = firstNameAttribute;
        this.secondNameAttribute = secondNameAttribute;
        this.groupsAttribute = groupsAttribute;
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

    public String getServerCertificate() {
        return serverCertificate;
    }

    public void setServerCertificate(String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }
}
