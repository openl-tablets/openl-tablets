package org.openl.rules.webstudio.web.install;

public final class CASSettings {
    private String webStudioUrl;
    private String casServerUrl;
    private String firstNameAttribute;
    private String secondNameAttribute;
    private String groupsAttribute;

    public CASSettings(String webStudioUrl,
            String casServerUrl,
            String firstNameAttribute,
            String secondNameAttribute,
            String groupsAttribute) {
        this.webStudioUrl = webStudioUrl;
        this.casServerUrl = casServerUrl;
        this.firstNameAttribute = firstNameAttribute;
        this.secondNameAttribute = secondNameAttribute;
        this.groupsAttribute = groupsAttribute;
    }

    public String getWebStudioUrl() {
        return webStudioUrl;
    }

    public void setWebStudioUrl(String webStudioUrl) {
        this.webStudioUrl = webStudioUrl;
    }

    public String getCasServerUrl() {
        return casServerUrl;
    }

    public void setCasServerUrl(String casServerUrl) {
        this.casServerUrl = casServerUrl;
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
}
