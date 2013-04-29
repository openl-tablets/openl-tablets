package org.openl.rules.webstudio.web.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
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
    private String dbVendor;
    private String sqlErrorsFilePath = "db/sql-errors.properties";
    private String dbSchema;

    private UIInput dbURLInput;
    private UIInput dbLoginInput;
    private UIInput dbPasswordInput;

    private ConfigurationManager appConfig;
    private ConfigurationManager systemConfig;
    private ConfigurationManager dbConfig;
    //private ConfigurationManager dbMySqlConfig;
    private ConfigurationManager sqlErrorsConfig;
    private ConfigurationManager externalDBConfig;

    private Map<String, Object> dbErrors;

    public InstallWizard() {
        appConfig = new ConfigurationManager(false,
            System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        workingDir = appConfig.getPath("webstudio.home");

        externalDBConfig = new ConfigurationManager(false,
            System.getProperty("webapp.root") + "/WEB-INF/conf/db/db-mysql.properties");
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
                sqlErrorsConfig = new ConfigurationManager(false,  null,
                    System.getProperty("webapp.root") + "/WEB-INF/conf/" + sqlErrorsFilePath);

                dbErrors = sqlErrorsConfig.getProperties();

                userMode = systemConfig.getStringProperty("user.mode");

                boolean innerDb = dbConfig.getStringProperty("db.driver").contains("hsqldb");
                appMode = innerDb ? "demo" : "production";

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

                dbConfig.setProperty("db.url", dbPrefix + dbUrl);
                dbConfig.setProperty("db.user", dbUsername);
                dbConfig.setProperty("db.password", dbPassword);
                dbConfig.setProperty("db.driver", externalDBConfig.getStringProperty("db.driver"));
                dbConfig.setProperty("db.hibernate.dialect", externalDBConfig.getStringProperty("db.hibernate.dialect"));
                dbConfig.setProperty("db.hibernate.hbm2ddl.auto", externalDBConfig.getStringProperty("db.hibernate.hbm2ddl.auto"));
                dbConfig.setProperty("db.schema", this.dbSchema);
                dbConfig.setProperty("db.validationQuery", externalDBConfig.getStringProperty("db.validationQuery"));
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
        String dbPasswordString = (String) dbPasswordInput.getSubmittedValue();

        if (!"demo".equals(appMode)) {
            if (StringUtils.isBlank(dbVendor)) {
                throw new ValidatorException(new FacesMessage("Select database type"));

            } else if (StringUtils.isEmpty(dbUrl)) {
                throw new ValidatorException(new FacesMessage("Database URL can not be blank"));
            }
            else {
                testDBConnection(dbUrl, dbUsername, dbPasswordString);
            }
        }

    }

    /**
     * Validates WebStudio working directory for write access. If specified
     * folder is not writable the validation error will appears
     * 
     * @param context
     * @param toValidate
     * @param value
     */
    public void workingDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        String studioPath;
        File studioDir;

        if (!StringUtils.isEmpty((String) value)) {
            studioPath = ConfigurationManager.normalizePath((String) value);
            studioDir = new File(studioPath);

            if (studioDir.exists()) {
                if (studioDir.isDirectory()) {

                    if (studioDir.canWrite()) {
                        /*
                         * If canWrite() returns true the temp file will be
                         * created. It's needed because in Windows OS method
                         * canWrite() returns true if folder isn't marked 'read
                         * only' but such folders can have security permissions
                         * 'deny all'
                         */
                        isWritable(studioDir);
                    } else {
                        throw new ValidatorException(new FacesMessage("There is not enough access rights for installing WebStudio into the folder: '" + studioPath + "'"));
                    }
                } else {
                    throw new ValidatorException(new FacesMessage("'" + studioPath + "' is not a folder"));
                }
            } else {
                File parentFolder = studioDir.getParentFile();
                File existingFolder = null;

                while (parentFolder != null) {
                    if (parentFolder.exists()) {
                        existingFolder = parentFolder.getAbsoluteFile();

                        break;
                    }
                    parentFolder = parentFolder.getParentFile();
                }
                boolean hasAccess = studioDir.mkdirs();

                if (!hasAccess) {

                    isWritable(studioDir);

                } else {
                    deleteFolder(existingFolder, studioDir);
                }
            }

        } else {
            throw new ValidatorException(new FacesMessage("WebStudio working directory name can not be blank"));
        }
    }

    /**
     * Creates a temp file for validating folder write permisions
     * @param file is a folder where temp file will be created
     * @return true if specified folder is writable, otherwise returns false
     */
    public boolean isWritable(File file) {
        boolean isAccessable = false;

        try {
            File tmpFile = File.createTempFile("temp", null, file);
            isAccessable = true;
            tmpFile.delete();

        } catch (IOException ioe) {
            throw new ValidatorException(new FacesMessage(ioe.getMessage() + " for '" + file.getAbsolutePath() + "'"));
        }
        return isAccessable;
    }

    /**
     * Deletes the folder which was created for validating folder permissions
     * 
     * @param existingFolder folder which already exists on file system
     * @param studioFolder folder were studio will be installed
     */
    private void deleteFolder(File existingFolder, File  studioFolder) {

        studioFolder.delete();

        while (!studioFolder.getAbsolutePath().equalsIgnoreCase(existingFolder.getAbsolutePath())) {
            studioFolder.delete();
            studioFolder = studioFolder.getParentFile();
        }
    }

    /**
     * Returns collection of properties files for external databases
     * @return
     */
    public Collection<File> getDBPropetiesFiles() {
        File dbPropFolder = new File(System.getProperty("webapp.root") + "/WEB-INF/conf/db");
        Collection<File> dbPropFiles = new ArrayList<File>();

        if (dbPropFolder.isDirectory()) {
            for (File file : dbPropFolder.listFiles()) {
                if (StringUtils.startsWith(file.getName(), "db-")) {
                    dbPropFiles.add(file);
                }
            }
        }
        return dbPropFiles;
    }

    /**
     * Returns a Map of data base vendors
     * @return
     */
    public List<SelectItem> getDBVendors() {
       List<SelectItem> dbVendors = new ArrayList<SelectItem>();
       Properties dbProps = new Properties();

       for (File propFile : getDBPropetiesFiles()) {
           try {
            dbProps.load(new FileInputStream(propFile));
            String propertyFilePath = System.getProperty("webapp.root") + "/WEB-INF/conf/db/" + propFile.getName();
            String dbVendor = dbProps.getProperty("db.vendor");

            dbVendors.add(new SelectItem(propertyFilePath, dbVendor));
        } catch (FileNotFoundException e) {
            LOG.error("The file " + propFile.getAbsolutePath() + " not found", e);
        } catch (IOException e) {
            LOG.error("Error while loading file " + propFile.getAbsolutePath(), e);
        }
       }
        return dbVendors;
    }

    /**
     * Listener for vendor selectOnMenu
     * @param e ajax event
     */
    public void dbVendorChanged(AjaxBehaviorEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();

        if (uiInput.getLocalValue() != null) {
            String propertyFilePath = uiInput.getValue().toString();
            externalDBConfig = new ConfigurationManager(false, propertyFilePath);

            String url = externalDBConfig.getStringProperty("db.url");

            if (!StringUtils.isEmpty(url)) {
                String dbUrlSeparator = externalDBConfig.getStringProperty("db.url.separator");
                String dbUrl = (externalDBConfig.getStringProperty("db.url")).split(dbUrlSeparator)[1];
                String prefix = (externalDBConfig.getStringProperty("db.url")).split(dbUrlSeparator)[0] + dbUrlSeparator;
                String dbLogin = externalDBConfig.getStringProperty("db.user");
                String dbDriver = externalDBConfig.getStringProperty("db.driver");

                setDbUrl(dbUrl);
                setDbUsername(dbLogin);
                setDbDriver(dbDriver);
                this.dbPrefix = prefix;

                // For Oracle database schema is a username
                if (StringUtils.containsIgnoreCase (dbVendor, "oracle")) {
                    this.dbSchema = externalDBConfig.getStringProperty("db.username");
                }
            }
        }else {
            // Reset database url and dtabase user name when no database type is selected
            this.dbUrl = "";
            this.dbUsername = "";
        }
    }

    /**
     * Ajax event for changing database url.
     * 
     * @param e AjaxBehavior event
     */
    public void urlChanged(AjaxBehaviorEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();
        String url = uiInput.getValue().toString();
        setDbUrl(url);
    }

    /**
     * Ajax event for changing database username
     * 
     * @param e AjaxBehavior event
     */
    public void usernameChanged(AjaxBehaviorEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();
        String username = uiInput.getValue().toString();
        setDbUsername(username);
    }

    /**
     * Ajax event for changing application mode: demo or production
     * 
     * @param e AjaxBehavior event
     */
    public void appmodeChanged(AjaxBehaviorEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();
        appMode = uiInput.getValue().toString();
    }

    public ConfigurationManager getExternalDBConfig() {
        return externalDBConfig;
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

    public String getFolderSeparator() {
        
        return File.separator;
    }

    public String getDbVendor() {
        return dbVendor;
    }

    public void setDbVendor(String dbVendor) {
        this.dbVendor = dbVendor;
    }

    public void setExternalDBConfig(ConfigurationManager externalDBConfig) {
        this.externalDBConfig = externalDBConfig;
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

}
