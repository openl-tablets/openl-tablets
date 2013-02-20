package org.openl.rules.webstudio.web.install;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
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
    private boolean showErrorMessage = false;

    private String userMode = "single";
    private String appMode = "production";

    @NotBlank
    private String dbUrl;
    @NotBlank
    private String dbUsername;
    @NotBlank
    private String dbPassword;
    private String dbDriver;
    private String dbPrefix;

    private UIInput dbURLInput;
    private UIInput dbLoginInput;
    private UIInput dbPasswordInput;

    private ConfigurationManager appConfig;
    private ConfigurationManager systemConfig;
    private ConfigurationManager dbConfig;
    private ConfigurationManager dbMySqlConfig;
    private ConfigurationManager sqlErrorsConfig;

    private Map<String, Object> dbErrors;

    public InstallWizard() {
        appConfig = new ConfigurationManager(false,
            System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        workingDir = appConfig.getPath("webstudio.home");

        dbMySqlConfig = new ConfigurationManager(false,
            System.getProperty("webapp.root") + "/WEB-INF/conf/db-mysql.properties");
    }

    public String start() {
        step = 1;
        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String prev() {
        return PAGE_PREFIX + --step + PAGE_POSTFIX;
    }

    public String next() {
        if (++step == 2) {

            workingDir = ConfigurationManager.normalizePath(workingDir);

            // Get defaults from 'system.properties'
            if (newWorkingDir || systemConfig == null) {
                systemConfig = new ConfigurationManager(true,
                    workingDir + "/system-settings/system.properties",
                    System.getProperty("webapp.root") + "/WEB-INF/conf/system.properties");

                dbConfig = new ConfigurationManager(true,
                    workingDir + "/system-settings/db.properties",
                    System.getProperty("webapp.root") + "/WEB-INF/conf/db.properties");
                sqlErrorsConfig = new ConfigurationManager(false,
                    null,
                    System.getProperty("webapp.root") + "/WEB-INF/conf/sql-errors.properties");

                dbErrors = sqlErrorsConfig.getProperties();

                userMode = systemConfig.getStringProperty("user.mode");

                boolean innerDb = dbConfig.getStringProperty("db.driver").contains("hsqldb");
                appMode = innerDb ? "demo" : "production";

                ConfigurationManager defaultDbConfig = !innerDb ? dbConfig : dbMySqlConfig;
                dbUrl = defaultDbConfig.getStringProperty("db.url").split("//")[1];
                dbPrefix = defaultDbConfig.getStringProperty("db.url").split("//")[0] + "//";
                dbUsername = defaultDbConfig.getStringProperty("db.user");
                dbPassword = defaultDbConfig.getStringProperty("db.password");
                dbDriver = defaultDbConfig.getStringProperty("db.driver");
            }
        }

        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String finish() {

        try {
            systemConfig.setProperty("user.mode", userMode);
            systemConfig.save();

            appConfig.setPath("webstudio.home", workingDir);
            appConfig.setProperty("webstudio.configured", true);
            appConfig.save();
            System.setProperty("webstudio.home", workingDir);
            System.setProperty("webstudio.configured", "true");

            if (appMode.equals("production")) {

                dbConfig.setProperty("db.url", dbMySqlConfig.getStringProperty("db.url").split("//")[0] + "//" + dbUrl);
                dbConfig.setProperty("db.user", dbUsername);
                dbConfig.setProperty("db.password", dbPassword);
                dbConfig.setProperty("db.driver", dbMySqlConfig.getStringProperty("db.driver"));
                dbConfig.setProperty("db.hibernate.dialect", dbMySqlConfig.getStringProperty("db.hibernate.dialect"));
                dbConfig.save();

            } else {
                dbConfig.restoreDefaults();
            }

            XmlWebApplicationContext context = (XmlWebApplicationContext) WebApplicationContextUtils.getWebApplicationContext(FacesUtils.getServletContext());

            context.setConfigLocations(new String[] { "/WEB-INF/spring/webstudio-beans.xml",
                    "/WEB-INF/spring/system-config-beans.xml",
                    "/WEB-INF/spring/repository-beans.xml",
                    "/WEB-INF/spring/security-beans.xml",
                    "/WEB-INF/spring/security/security-" + userMode + ".xml" });

            context.refresh();
            FacesUtils.redirectToRoot();

        } catch (Exception e) {
            LOG.error("Failed while saving the configuration", e);
        } finally {
            step = 1;
        }

        return null;
    }

    /**
     * Methods tests connection to DB. Depending on the SQL error code
     * corresponding validate exception will be thrown SQL errors loading from
     * sql-errors.properties.
     */

    /*
     * If a new database is added to the project, just add new sql error into
     * the file sql-errors.properties
     */
    public void testDBConnection(String url, String login, String password) {
        Connection conn = null;
        int errorCode = 0;

        try {
            Class.forName(dbDriver);
            conn = DriverManager.getConnection((dbPrefix + url), login, password);

        } catch (SQLException sqle) {
            errorCode = sqle.getErrorCode();
            String errorMessage = (String) dbErrors.get("" + errorCode);

            if (errorMessage != null) {
                LOG.error(sqle.getMessage(), sqle);
                throw new ValidatorException(new FacesMessage(errorMessage));
            } else {
                LOG.error(sqle.getMessage(), sqle);
                throw new ValidatorException(new FacesMessage("Incorrect database URL, login or password"));
            }

        } catch (ClassNotFoundException cnfe) {
            LOG.error(cnfe.getMessage(), cnfe);
            throw new ValidatorException(new FacesMessage("Incorrectd database driver"));
        } catch (Exception e) {
            LOG.error("Unexpected error, see instalation log", e);
            throw new ValidatorException(new FacesMessage("Unexpected error: " + e.getMessage()));
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

    }

    public void dbValidator(FacesContext context, UIComponent toValidate, Object value) {
        String dbURLString = (String) dbURLInput.getLocalValue();
        String dbLoginString = (String) dbLoginInput.getLocalValue();
        String dbPasswordString = (String) dbPasswordInput.getSubmittedValue();

        if (StringUtils.isBlank(dbURLString)) {
            throw new ValidatorException(new FacesMessage("Database URL may not be empty"));
        } else {
            testDBConnection(dbURLString, dbLoginString, dbPasswordString);
        }
    }

    public void workingDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        File studioWorkingDir;
        File tmpFile = null;
        boolean hasAccess;

        try {
            studioWorkingDir = new File((String) value);

            if (studioWorkingDir.exists()) {
                tmpFile = new File(studioWorkingDir.getAbsolutePath() + File.separator + "tmp");

                hasAccess = tmpFile.mkdir();

                if (!hasAccess) {
                    throw new ValidatorException(new FacesMessage("Can't get access to the folder '" + (String) value + 
                        "'    Please, contact to your system administrator."));
                }
            } else {
                if (studioWorkingDir.mkdirs() == false) {
                    showErrorMessage = true;
                    throw new ValidatorException(new FacesMessage("Incorrect folder name."));
                } else {
                    showErrorMessage = false;
                    deleteFolder((String) value);
                }
            }
        } catch (Exception e) {
            throw new ValidatorException(new FacesMessage(e.getMessage()));

        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    /* Deleting the folder which were created for validating folder permissions */
    public void deleteFolder(String folderPath) {
        File workFolder = new File(folderPath);
        File parent = workFolder.getParentFile();

        while (parent != null) {
            workFolder.delete();
            parent = workFolder.getParentFile();
            workFolder = parent;
        }
    }

    public int getStep() {
        return step;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        String normWorkingDir = ConfigurationManager.normalizePath(workingDir);
        newWorkingDir = !normWorkingDir.equals(this.workingDir);
        this.workingDir = normWorkingDir;
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

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
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

    public String getDbDriver() {
        return dbDriver;
    }

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public UIInput getDbURLInput() {
        return dbURLInput;
    }

    public void setDbURLInput(UIInput dbURLInput) {
        this.dbURLInput = dbURLInput;
    }

    public UIInput getDbLoginInput() {
        return dbLoginInput;
    }

    public void setDbLoginInput(UIInput dbLoginInput) {
        this.dbLoginInput = dbLoginInput;
    }

    public UIInput getDbPasswordInput() {
        return dbPasswordInput;
    }

    public void setDbPasswordInput(UIInput dbPasswordInput) {
        this.dbPasswordInput = dbPasswordInput;
    }

    public boolean isShowErrorMessage() {
        return showErrorMessage;
    }

    public void setShowErrorMessage(boolean showErrorMessage) {
        this.showErrorMessage = showErrorMessage;
    }

}
