package org.openl.rules.webstudio.web.install;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

@ManagedBean
@SessionScoped
public class InstallWizard {

    private final Log LOG = LogFactory.getLog(InstallWizard.class);

    private int step;

    private static final String PAGE_PREFIX = "step";
    private static final String PAGE_POSTFIX = "?faces-redirect=true";

    @NotBlank
    private String workingDir;
    private boolean newWorkingDir;

    private String userMode = "single";
    private String appMode = "production";

    @NotBlank
    private String dbHost;
    @NotBlank
    private String dbPort;
    @NotBlank
    private String dbName;
    @NotBlank
    private String dbUsername;
    private String dbPassword;

    private ConfigurationManager appConfig;
    private ConfigurationManager systemConfig;

    public InstallWizard() {
        appConfig = new ConfigurationManager(
                false, System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        workingDir = appConfig.getStringProperty("webstudio.home");
    }

    public String start() {
        step = 1;
        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String prev() {
        return PAGE_PREFIX + --step + PAGE_POSTFIX;
    }

    public String next() {
        // Get defaults from 'system.properties'
        if (++step == 2 && newWorkingDir) {
            systemConfig = new ConfigurationManager(true,
                    workingDir + "/system-settings/system.properties",
                    System.getProperty("webapp.root") + "/WEB-INF/conf/system.properties");

            userMode = systemConfig.getStringProperty("user.mode");
        }

        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String finish() {
        try {
            systemConfig.setProperty("user.mode", userMode);
            systemConfig.save();

            appConfig.setProperty("webstudio.home", workingDir);
            appConfig.setProperty("webstudio.configured", true);
            appConfig.save();
            System.setProperty("webstudio.home", workingDir);
            System.setProperty("webstudio.configured", "true");

            XmlWebApplicationContext context = (XmlWebApplicationContext) WebApplicationContextUtils
                    .getWebApplicationContext(FacesUtils.getServletContext());

            context.setConfigLocations(new String[] {
                    "/WEB-INF/spring/webstudio-beans.xml",
                    "/WEB-INF/spring/system-config-beans.xml",
                    "/WEB-INF/spring/repository-beans.xml",
                    "/WEB-INF/spring/security-beans.xml",
                    "/WEB-INF/spring/security/security-" + userMode + ".xml"
            });
            context.refresh();

            FacesUtils.redirectToRoot();
        } catch (Exception e) {
            LOG.error("Failed while saving the configuration", e);
        } finally {
            step = 1;
        }

        return null;
    }

    public int getStep() {
        return step;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        newWorkingDir = !workingDir.equals(this.workingDir);
        this.workingDir = workingDir;
    }

    public String getUserMode() {
        return userMode;
    }

    public void setUserMode(String userMode) {
        this.userMode = userMode;
    }

    public String getAppMode() {
        return appMode;
    }

    public void setAppMode(String appMode) {
        this.appMode = appMode;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

}
