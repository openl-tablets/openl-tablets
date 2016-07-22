package org.openl.rules.webstudio.web.install;

import org.apache.poi.util.IOUtils;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.db.utils.DBUtils;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.web.admin.*;
import org.openl.rules.webstudio.web.servlet.SessionListener;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@ManagedBean
@SessionScoped
public class InstallWizard {

    private static final String MULTI_USER_MODE = "multi";

    private final Logger log = LoggerFactory.getLogger(InstallWizard.class);

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
    private String dbSchema;

    private UIInput dbURLInput;
    private UIInput dbLoginInput;
    private UIInput dbPasswordInput;

    private ConfigurationManager appConfig;
    private ConfigurationManager systemConfig;
    private ConfigurationManager dbConfig;
    private ConfigurationManager externalDBConfig;

    private DBUtils dbUtils;

    private RepositoryConfiguration designRepositoryConfiguration;
    private ProductionRepositoryEditor productionRepositoryEditor;
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    // Reuse existing controllers
    private ConnectionProductionRepoController connectionProductionRepoController;
    private NewProductionRepoController newProductionRepoController;

    public InstallWizard() {
        appConfig = new ConfigurationManager(true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        workingDir = appConfig.getPath("webstudio.home");

        externalDBConfig = new ConfigurationManager(true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/db/db-mysql.properties");
        dbUtils = new DBUtils();
    }

    public String getPreviousPage() {
        return PAGE_PREFIX + (step - 1) + PAGE_POSTFIX;
    }

    public String start() {
        step = 1;
        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String prev() {
        return PAGE_PREFIX + --step + PAGE_POSTFIX;
    }

    public String next() {
        // Validations
        if (step == 2) {
            try {
                RepositoryValidators.validate(designRepositoryConfiguration);

                productionRepositoryEditor.validate();
            } catch (RepositoryValidationException e) {
                FacesUtils.addErrorMessage(e.getMessage());
                return null;
            }
        }

        // Go to next step
        if (++step == 2) {

            workingDir = ConfigurationManager.normalizePath(workingDir);

            // Get defaults from 'system.properties'
            if (newWorkingDir || systemConfig == null) {
                systemConfig = new ConfigurationManager(true,
                        workingDir + "/system-settings/system.properties",
                        System.getProperty("webapp.root") + "/WEB-INF/conf/system.properties");
                designRepositoryConfiguration = new RepositoryConfiguration("", systemConfig, RepositoryType.DESIGN);

                initProductionRepositoryEditor();

                dbConfig = new ConfigurationManager(true,
                        workingDir + "/system-settings/db.properties",
                        System.getProperty("webapp.root") + "/WEB-INF/conf/db.properties");

                userMode = systemConfig.getStringProperty("user.mode");

                boolean innerDb = dbConfig.getStringProperty("db.driver").contains("hsqldb");
                appMode = innerDb ? "demo" : "production";

            }
        }
        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String finish() {
        try {
            if (MULTI_USER_MODE.equals(userMode) && appMode.equals("production")) {
                dbConfig.setProperty("db.url", dbPrefix + dbUrl);
                dbConfig.setProperty("db.user", dbUsername);
                dbConfig.setProperty("db.password", dbPassword);
                dbConfig.setProperty("db.driver", externalDBConfig.getStringProperty("db.driver"));
                dbConfig.setProperty("db.hibernate.dialect", externalDBConfig.getStringProperty("db.hibernate.dialect"));
                dbConfig.setProperty("db.hibernate.hbm2ddl.auto", externalDBConfig.getStringProperty("db.hibernate.hbm2ddl.auto"));
                dbConfig.setProperty("db.schema", this.dbSchema);
                dbConfig.setProperty("db.validationQuery", externalDBConfig.getStringProperty("db.validationQuery"));
                dbConfig.setProperty("db.url.separator", externalDBConfig.getStringProperty("db.url.separator"));

                migrateDatabase(dbConfig.getProperties());

                dbConfig.save();
            } else {
                dbConfig.restoreDefaults();
            }

            productionRepositoryEditor.save();

            systemConfig.setProperty("user.mode", userMode);

            // TODO: This line also do systemConfig.save() implicitly
            designRepositoryConfiguration.save();

            System.clearProperty("webstudio.home"); // Otherwise this property will not be saved to file.
            appConfig.setPath("webstudio.home", workingDir);
            appConfig.setProperty("webstudio.configured", true);
            appConfig.save();
            System.setProperty("webstudio.home", workingDir);
            System.setProperty("webstudio.configured", "true");
            System.setProperty("user.mode", userMode);

            destroyRepositoryObjects();

            System.setProperty("webstudio.mode", "webstudio");
            final ServletContext servletContext = FacesUtils.getServletContext();
            ReloadableDelegatingFilter.reload(new ReloadableDelegatingFilter.ConfigurationReloader() {
                @Override
                public void reload() {
                    XmlWebApplicationContext context = (XmlWebApplicationContext) WebApplicationContextUtils.getWebApplicationContext(servletContext);
                    context.refresh();

                    SessionListener.getSessionCache(servletContext).invalidateAll();
                }
            });

            FacesUtils.redirectToRoot();

        } catch (Exception e) {
            log.error("Failed while saving the configuration", e);
            if (e.getCause() instanceof FlywayException) {
                FacesUtils.addErrorMessage("Cannot migrate the database. Check the logs for details.");
            } else {
                FacesUtils.addErrorMessage("Cannot save the configuration. Check the logs for details.");
            }
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
        Connection conn = dbUtils.createConnection(dbDriver, dbPrefix, url, login, password);
        try {
            conn.close();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void dbValidator(FacesContext context, UIComponent toValidate, Object value) {
        String dbPasswordString = (String) dbPasswordInput.getSubmittedValue();

        if (!"demo".equals(appMode)) {
            if (StringUtils.isBlank(dbVendor)) {
                throw new ValidatorException(FacesUtils.createErrorMessage("Select database type"));
            } else if (StringUtils.isEmpty(dbUrl)) {
                throw new ValidatorException(FacesUtils.createErrorMessage("Database URL can not be blank"));
            } else {
                testDBConnection(dbUrl, dbUsername, dbPasswordString);
            }
        }

    }

    /**
     * Validates WebStudio working directory for write access. If specified
     * folder is not writable the validation error will appears
     */
    public void workingDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        String studioPath;
        File studioDir;

        if (StringUtils.isNotEmpty((String) value)) {
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
                        throw new ValidatorException(FacesUtils.createErrorMessage("There is not enough access rights for installing WebStudio into the folder: '" + studioPath + "'"));
                    }
                } else {
                    throw new ValidatorException(FacesUtils.createErrorMessage("'" + studioPath + "' is not a folder"));
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
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio working directory name can not be blank"));
        }
    }

    /**
     * Creates a temp file for validating folder write permissions
     *
     * @param file is a folder where temp file will be created
     * @return true if specified folder is writable, otherwise returns false
     */
    public boolean isWritable(File file) {
        boolean isAccessible;

        try {
            File tmpFile = File.createTempFile("temp", null, file);
            isAccessible = true;
            if (!tmpFile.delete()) {
                log.warn("Can't delete temp file {}", tmpFile.getName());
            }

        } catch (IOException ioe) {
            throw new ValidatorException(FacesUtils.createErrorMessage(ioe.getMessage() + " for '" + file.getAbsolutePath() + "'"));
        }
        return isAccessible;
    }

    /**
     * Deletes the folder which was created for validating folder permissions
     *
     * @param existingFolder folder which already exists on file system
     * @param studioFolder   folder were studio will be installed
     */
    private void deleteFolder(File existingFolder, File studioFolder) {

        studioFolder.delete();

        while (!studioFolder.getAbsolutePath().equalsIgnoreCase(existingFolder.getAbsolutePath())) {
            studioFolder.delete();
            studioFolder = studioFolder.getParentFile();
        }
    }

    /**
     * Returns collection of properties files for external databases
     */
    public Collection<File> getDBPropetiesFiles() {
        File dbPropFolder = new File(System.getProperty("webapp.root") + "/WEB-INF/conf/db");
        Collection<File> dbPropFiles = new ArrayList<File>();

        if (dbPropFolder.isDirectory()) {
            for (File file : dbPropFolder.listFiles()) {
                if (file.getName().startsWith("db-")) {
                    dbPropFiles.add(file);
                }
            }
        }
        return dbPropFiles;
    }

    /**
     * Returns a Map of data base vendors
     */
    public List<SelectItem> getDBVendors() {
        List<SelectItem> dbVendors = new ArrayList<SelectItem>();
        Properties dbProps = new Properties();

        for (File propFile : getDBPropetiesFiles()) {
            InputStream is = null;
            try {
                is = new FileInputStream(propFile);
                dbProps.load(is);
                is.close();
                String propertyFilePath = System.getProperty("webapp.root") + "/WEB-INF/conf/db/" + propFile.getName();
                String dbVendor = dbProps.getProperty("db.vendor");

                dbVendors.add(new SelectItem(propertyFilePath, dbVendor));
            } catch (FileNotFoundException e) {
                log.error("The file {} not found", propFile.getAbsolutePath(), e);
            } catch (IOException e) {
                log.error("Error while loading file {}", propFile.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return dbVendors;
    }

    /**
     * Listener for vendor selectOnMenu
     *
     * @param e ajax event
     */
    public void dbVendorChanged(AjaxBehaviorEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();

        if (uiInput.getLocalValue() != null) {
            String propertyFilePath = uiInput.getValue().toString();
            externalDBConfig = new ConfigurationManager(false, propertyFilePath);

            String url = externalDBConfig.getStringProperty("db.url");

            if (StringUtils.isNotEmpty(url)) {
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
                if (StringUtils.containsIgnoreCase(dbVendor, "oracle")) {
                    this.dbSchema = externalDBConfig.getStringProperty("db.username");
                }
            }
        } else {
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

    public void setStep(int step) {
        this.step = step;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        String normWorkingDir = ConfigurationManager.normalizePath(workingDir);
        newWorkingDir = !normWorkingDir.equals(this.workingDir);
        this.workingDir = normWorkingDir;

        // Other configurations depend on this property
        System.setProperty("webstudio.home", this.workingDir);
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

    public RepositoryConfiguration getDesignRepositoryConfiguration() {
        return designRepositoryConfiguration;
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryEditor.getProductionRepositoryConfigurations();
    }

    public void deleteProductionRepository(String configName) {
        try {
            productionRepositoryEditor.deleteProductionRepository(configName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage(e.getMessage());
        }
    }

    public ConnectionProductionRepoController getConnectionProductionRepoController() {
        return connectionProductionRepoController;
    }

    public NewProductionRepoController getNewProductionRepoController() {
        return newProductionRepoController;
    }

    @PreDestroy
    public void destroy() {
        destroyRepositoryObjects();
    }

    private void migrateDatabase(final Map<String, Object> dbProperties) {
        XmlWebApplicationContext ctx = null;
        try {
            ctx = new XmlWebApplicationContext();
            ctx.setServletContext(FacesUtils.getServletContext());
            ctx.setConfigLocations(new String[]{
                    "classpath:META-INF/standalone/spring/security-hibernate-beans.xml",
                    "/WEB-INF/spring/security/db/flyway-bean.xml"
            });
            ctx.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                    beanFactory.registerSingleton("dbConfig", dbProperties);
                }
            });
            ctx.refresh();
            ctx.getBean("dbMigration");
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    private void initProductionRepositoryEditor() {
        destroyRepositoryObjects();

        final ConfigurationManagerFactory productionConfigManagerFactory = new ConfigurationManagerFactory(
                true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/rules-production.properties",
                workingDir + "/system-settings/"
        );
        productionRepositoryFactoryProxy = new ProductionRepositoryFactoryProxy();
        productionRepositoryFactoryProxy.setConfigManagerFactory(productionConfigManagerFactory);
        productionRepositoryEditor = new ProductionRepositoryEditor(
                systemConfig,
                productionConfigManagerFactory,
                productionRepositoryFactoryProxy
        );

        connectionProductionRepoController = new ConnectionProductionRepoController();
        connectionProductionRepoController.setProductionConfigManagerFactory(productionConfigManagerFactory);
        connectionProductionRepoController.setProductionRepositoryFactoryProxy(productionRepositoryFactoryProxy);
        connectionProductionRepoController.setProductionRepositoryConfigurations(getProductionRepositoryConfigurations());

        newProductionRepoController = new NewProductionRepoController();
        newProductionRepoController.setProductionConfigManagerFactory(productionConfigManagerFactory);
        newProductionRepoController.setProductionRepositoryFactoryProxy(productionRepositoryFactoryProxy);
        newProductionRepoController.setProductionRepositoryConfigurations(getProductionRepositoryConfigurations());
    }

    private void destroyRepositoryObjects() {
        if (productionRepositoryFactoryProxy != null) {
            try {
                productionRepositoryFactoryProxy.destroy();
            } catch (RRepositoryException e) {
                log.error(e.getMessage(), e);
            }
            productionRepositoryFactoryProxy = null;
        }
    }
}
