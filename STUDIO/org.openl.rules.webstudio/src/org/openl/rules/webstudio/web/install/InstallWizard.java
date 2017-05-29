package org.openl.rules.webstudio.web.install;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.servlet.ServletContext;

import org.flywaydb.core.api.FlywayException;
import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.db.utils.DBUtils;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.GroupManagementServiceWrapper;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.admin.*;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.web.context.support.XmlWebApplicationContext;

@ManagedBean
@SessionScoped
public class InstallWizard {

    private static final String MULTI_USER_MODE = "multi";
    private static final String AD_USER_MODE = "ad";
    private static final String SEPARATOR_PATTERN = "\\s*,\\s*";
    private static final String APP_MODE_DEMO = "demo";
    private static final String APP_MODE_PRODUCTION = "production";

    private final Logger log = LoggerFactory.getLogger(InstallWizard.class);

    private int step;

    private static final String PAGE_PREFIX = "step";
    private static final String PAGE_POSTFIX = "?faces-redirect=true";

    @NotBlank
    private String workingDir;
    private boolean newWorkingDir;
    private boolean showErrorMessage = false;

    private String userMode = "single";
    private String appMode = APP_MODE_PRODUCTION;
    private boolean groupsAreManagedInStudio = true;

    @NotBlank
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private String dbDriver;
    private String dbPrefix;
    private String dbVendor;
    private String dbSchema;

    private String adDomain;
    private String adUrl;
    private String adUsername;
    private String adPassword;

    private ConfigurationManager appConfig;
    private ConfigurationManager systemConfig;
    private ConfigurationManager dbConfig;
    private ConfigurationManager adConfig;

    private DBUtils dbUtils;

    private RepositoryConfiguration designRepositoryConfiguration;
    private ProductionRepositoryEditor productionRepositoryEditor;
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    // Reuse existing controllers
    private ConnectionProductionRepoController connectionProductionRepoController;
    private NewProductionRepoController newProductionRepoController;

    @ManagedProperty(value="#{groupManagementService}")
    private GroupManagementServiceWrapper groupManagementService;
    private XmlWebApplicationContext temporaryContext;
    private Boolean adAllowAccessToNewUsers;
    private String adAdmins;

    public InstallWizard() {
        appConfig = new ConfigurationManager(true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        workingDir = appConfig.getPath("webstudio.home");

        ServletContext servletContext = FacesUtils.getServletContext();
        dbUtils = new DBUtils(servletContext);
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
        ++step;
        if (step == 2) {
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

                adConfig = new ConfigurationManager(true,
                        workingDir + "/system-settings/security-ad.properties",
                        System.getProperty("webapp.root") + "/WEB-INF/conf/security-ad.properties");

                userMode = systemConfig.getStringProperty("user.mode");

                String savedDbDriver = dbConfig.getStringProperty("db.driver");
                String savedDbUrl = StringUtils.trimToEmpty(dbConfig.getStringProperty("db.url"));
                boolean innerDb = "org.h2.Driver".equals(savedDbDriver) && savedDbUrl.startsWith("jdbc:h2:mem:");
                appMode = innerDb ? APP_MODE_DEMO : APP_MODE_PRODUCTION;

            }
        } else if (step == 3) {
            readDbProperties();
            readAdProperties();
        } else if (step == 4) {
            try {
                initializeTemporaryContext();
                // GroupManagementService delegate is transactional and properly initialized
                GroupManagementService delegate = (GroupManagementService) temporaryContext.getBean("groupManagementService");
                // Initialize groupManagementService before first usage in GroupsBean
                groupManagementService.setDelegate(delegate);
            } catch (Exception e) {
                log.error("Failed while saving the configuration", e);
                if (e.getCause() instanceof FlywayException) {
                    FacesUtils.addErrorMessage("Cannot migrate the database. Check the logs for details.");
                } else {
                    FacesUtils.addErrorMessage("Cannot save the configuration. Check the logs for details.");
                }

            }
        }
        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    private void initializeTemporaryContext() {
        destroyTemporaryContext();

        dbConfig.removeProperty("db.additional.migration.paths");
        final Map<String, Object> dbProperties = dbConfig.getProperties();
        migrateDatabase(dbProperties);

        // Load groupDao and initialize groupManagementService
        temporaryContext = new XmlWebApplicationContext();
        temporaryContext.setServletContext(FacesUtils.getServletContext());
        temporaryContext.setConfigLocations(
                "/WEB-INF/spring/security/db-services.xml");
        temporaryContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws
                                                                                            BeansException {
                beanFactory.registerSingleton("dbConfig", dbProperties);
            }
        });
        temporaryContext.refresh();
    }

    private void readDbProperties() {
        String propsLocation = workingDir + "/system-settings/db.properties";
        ConfigurationManager savedConfig = new ConfigurationManager(false, propsLocation, System.getProperty("webapp.root") + "/WEB-INF/conf/db.properties");

        String url = savedConfig.getStringProperty("db.url");

        if (StringUtils.isNotEmpty(url) && !APP_MODE_DEMO.equals(appMode)) {
            String dbUrlSeparator = savedConfig.getStringProperty("db.url.separator");
            dbUrl = url.split(dbUrlSeparator)[1];
            dbPrefix = url.split(dbUrlSeparator)[0] + dbUrlSeparator;
            dbUsername = savedConfig.getStringProperty("db.user");
            dbPassword = savedConfig.getStringProperty("db.password");
            dbDriver = savedConfig.getStringProperty("db.driver");

            String propertyFilePathForVendor = getPropertyFilePathForVendor(dbDriver);
            dbVendor = propertyFilePathForVendor;

            // For Oracle database schema is a username
            if (StringUtils.containsIgnoreCase(propertyFilePathForVendor, "oracle")) {
                ConfigurationManager externalDBConfig = new ConfigurationManager(false, propertyFilePathForVendor);
                dbSchema = externalDBConfig.getStringProperty("db.username");
            } else {
                dbSchema = null;
            }
        } else {
            dbUrl = null;
            dbPrefix = null;
            dbUsername = null;
            dbPassword = null;
            dbDriver = null;
            dbVendor = null;
            dbSchema = null;
        }
    }

    private void readAdProperties() {
        adDomain = adConfig.getStringProperty("security.ad.domain");
        adUrl = adConfig.getStringProperty("security.ad.url");
        groupsAreManagedInStudio = adConfig.getBooleanProperty("security.ad.groups-are-managed-in-studio");
        adAllowAccessToNewUsers = !StringUtils.isBlank(adConfig.getStringProperty("security.ad.default-group"));
    }

    private String getPropertyFilePathForVendor(String dbDriver) {
        for (File propFile : getDBPropertiesFiles()) {
            InputStream is = null;
            Properties dbProps = new Properties();
            try {
                is = new FileInputStream(propFile);
                dbProps.load(is);
                is.close();

                if (dbDriver.equals(dbProps.getProperty("db.driver"))) {
                    return System.getProperty("webapp.root") + "/WEB-INF/conf/db/" + propFile.getName();
                }

            } catch (FileNotFoundException e) {
                log.error("The file {} is not found", propFile.getAbsolutePath(), e);
            } catch (IOException e) {
                log.error("Error while loading file {}", propFile.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return dbConfig.getStringProperty("db.vendor");
    }

    public String finish() {
        try {
            if (MULTI_USER_MODE.equals(userMode) && appMode.equals(APP_MODE_PRODUCTION)) {
                setProductionDbProperties();
                migrateDatabase(dbConfig.getProperties());

                dbConfig.save();
            } else {
                if (AD_USER_MODE.equals(userMode)) {
                    fillDbForAD();
                    dbConfig.save();

                    adConfig.setProperty("security.ad.domain", adDomain);
                    adConfig.setProperty("security.ad.url", adUrl);
                    adConfig.setProperty("security.ad.groups-are-managed-in-studio", groupsAreManagedInStudio);
                    adConfig.setProperty("security.ad.default-group", adAllowAccessToNewUsers ? "Viewers" : "");
                    adConfig.save();
                } else {
                    dbConfig.restoreDefaults();
                }
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
            destroyTemporaryContext();

            System.setProperty("webstudio.mode", "webstudio");
            ReloadableDelegatingFilter.reloadApplicationContext(FacesUtils.getServletContext());

            FacesUtils.redirectToRoot();

            return "/";
        } catch (Exception e) {
            log.error("Failed while saving the configuration", e);
            if (e.getCause() instanceof FlywayException) {
                FacesUtils.addErrorMessage("Cannot migrate the database. Check the logs for details.");
            } else {
                FacesUtils.addErrorMessage("Cannot save the configuration. Check the logs for details.");
            }
            return null;
        }
    }

    private void fillDbForAD() throws IOException {
        if (!appMode.equals(APP_MODE_DEMO)) {
            if (groupsAreManagedInStudio) {
                if (temporaryContext == null) {
                    initializeTemporaryContext();
                }
                GroupManagementService groupManagementService = (GroupManagementService) temporaryContext.getBean(
                        "groupManagementService");
                UserManagementService userManagementService = (UserManagementService) temporaryContext.getBean(
                        "userManagementService");
                List<Privilege> adminGroups = new ArrayList<Privilege>(Collections.singleton(groupManagementService.getGroupByName("Administrators")));

                // Delete example users
                for (User user : userManagementService.getAllUsers()) {
                    userManagementService.deleteUser(user.getUsername());
                }

                // Create admin users
                for (String username : adAdmins.trim().split(SEPARATOR_PATTERN)) {
                    if (!username.isEmpty()) {
                        userManagementService.addUser(new SimpleUser(null, null, username, "",
                                Constants.USER_ORIGIN_ACTIVE_DIRECTORY, adminGroups));
                    }
                }
                setProductionDbProperties();
                migrateDatabase(dbConfig.getProperties());
            } else {
                setProductionDbProperties();
            }
        } else {
            // Demo mode
            generateAdInitializationScripts();
        }
    }

    private void generateAdInitializationScripts() throws IOException {
        String scriptFolder = workingDir + "/system-settings/ad";

        Calendar c = Calendar.getInstance();
        String scriptPath = scriptFolder + "/V20170416__fill-tables-for-AD.sql";

        dbConfig.restoreDefaults();

        if (groupsAreManagedInStudio) {
            dbConfig.setProperty("db.additional.origins", Constants.USER_ORIGIN_ACTIVE_DIRECTORY);

            // Add several users to Administrators group
            File folder = new File(scriptFolder);
            if (!folder.mkdirs() && !folder.exists()) {
                throw new IOException("Can't create the folder for db initialization scripts");
            }
            StringBuilder script = new StringBuilder();
            script.append("DELETE FROM ${schemaPrefix}User2Group;\n");
            script.append("DELETE FROM ${schemaPrefix}OpenLUser;\n");
            String[] admins = adAdmins.trim().split(SEPARATOR_PATTERN);
            for (String username : admins) {
                if (!username.isEmpty()) {
                    script.append("INSERT INTO ${schemaPrefix}OpenLUser (LoginName, Password, origin) VALUES('")
                            .append(username)
                            .append("', '', '" + Constants.USER_ORIGIN_ACTIVE_DIRECTORY + "');\n");
                }
            }

            StringBuilder conditionsBuilder = new StringBuilder();
            for (String username : admins) {
                if (!username.isEmpty()) {
                    if (conditionsBuilder.length() > 0) {
                        conditionsBuilder.append("\t\tOR ");
                    }
                    conditionsBuilder.append("u.LoginName = '")
                            .append(username)
                            .append("' AND g.GroupName = 'Administrators'\n");
                }
            }

            if (conditionsBuilder.length() > 0) {
                script.append("INSERT INTO ${schemaPrefix}User2Group (UserID, GroupID) (\n"
                        + "\tSELECT u.UserID, g.GroupID\n"
                        + "\tFROM OpenLUser u, UserGroup g\n"
                        + "\tWHERE ")
                        .append(conditionsBuilder)
                        .append(");");
            }

            IOUtils.copyAndClose(IOUtils.toInputStream(script.toString()), new FileOutputStream(scriptPath));
        } else {
            // Copy groups from DB in temporary context to initialization scripts
            File folder = new File(scriptFolder);
            if (!folder.mkdirs() && !folder.exists()) {
                throw new IOException("Can't create the folder for db initialization scripts");
            }
            StringBuilder script = new StringBuilder();
            script.append("DELETE FROM ${schemaPrefix}User2Group;\n");
            script.append("DELETE FROM ${schemaPrefix}Group2Group;\n");
            script.append("DELETE FROM ${schemaPrefix}AccessControlEntry;\n");
            script.append("DELETE FROM ${schemaPrefix}UserGroup;\n");
            GroupManagementService groupManagementService = (GroupManagementService) temporaryContext.getBean(
                    "groupManagementService");

            // Add groups
            for (Group group : groupManagementService.getGroups()) {
                StringBuilder privilegesBuilder = new StringBuilder();
                for (Privilege privilege : group.getPrivileges()) {
                    if (!(privilege instanceof Group)) {
                        if (privilegesBuilder.length() > 0) {
                            privilegesBuilder.append(',');
                        }
                        privilegesBuilder.append(privilege.getName());
                    }
                }
                script.append(
                        "INSERT INTO ${schemaPrefix}UserGroup (GroupName, UserPrivileges) VALUES ('")
                        .append(group.getName())
                        .append("', '")
                        .append(privilegesBuilder)
                        .append("');\n");
            }

            // Add group mapping
            for (Group group : groupManagementService.getGroups()) {
                StringBuilder conditionsBuilder = new StringBuilder();
                for (Privilege privilege : group.getPrivileges()) {
                    if ((privilege instanceof Group)) {
                        if (conditionsBuilder.length() > 0) {
                            conditionsBuilder.append("\t\tOR ");
                        }
                        conditionsBuilder.append("g1.GroupName = '").append(group.getName()).append("'")
                                .append(" AND g2.GroupName = '").append(privilege.getName()).append("'\n");
                    }
                }
                if (conditionsBuilder.length() > 0) {
                    script.append("INSERT INTO ${schemaPrefix}Group2Group (GroupID, IncludedGroupID) (\n"
                            + "\tSELECT g1.GroupID, g2.GroupID\n"
                            + "\tFROM UserGroup g1, UserGroup g2\n"
                            + "\tWHERE ").append(conditionsBuilder)
                            .append(");\n");
                }

            }

            IOUtils.copyAndClose(IOUtils.toInputStream(script.toString()), new FileOutputStream(scriptPath));
        }
        dbConfig.setProperty("db.additional.migration.paths", "filesystem:" + scriptFolder);
    }

    private void setProductionDbProperties() {
        ConfigurationManager externalDBConfig = new ConfigurationManager(false, dbVendor);
        dbConfig.setProperty("db.url", dbPrefix + dbUrl);
        dbConfig.setProperty("db.user", dbUsername);
        dbConfig.setProperty("db.password", dbPassword);
        dbConfig.setProperty("db.driver", externalDBConfig.getStringProperty("db.driver"));
        dbConfig.setProperty("db.hibernate.dialect", externalDBConfig.getStringProperty("db.hibernate.dialect"));
        dbConfig.setProperty("db.hibernate.hbm2ddl.auto", externalDBConfig.getStringProperty("db.hibernate.hbm2ddl.auto"));
        dbConfig.setProperty("db.schema", this.dbSchema);
        dbConfig.setProperty("db.validationQuery", externalDBConfig.getStringProperty("db.validationQuery"));
        dbConfig.setProperty("db.url.separator", externalDBConfig.getStringProperty("db.url.separator"));
        if (AD_USER_MODE.equals(userMode) && groupsAreManagedInStudio) {
            dbConfig.setProperty("db.additional.origins", Constants.USER_ORIGIN_ACTIVE_DIRECTORY);
        } else {
            // By default only internal origin
            dbConfig.removeProperty("db.additional.origins");
        }
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
    private void testDBConnection(String url, String login, String password) {
        Connection conn = dbUtils.createConnection(dbDriver, dbPrefix, url, login, password);
        try {
            conn.close();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void dbValidator(FacesContext context, UIComponent toValidate, Object value) {
        String dbPasswordString = (String) value;

        if (!APP_MODE_DEMO.equals(appMode)) {
            if (StringUtils.isBlank(dbVendor)) {
                throw new ValidatorException(FacesUtils.createErrorMessage("Select database type"));
            } else if (StringUtils.isEmpty(dbUrl)) {
                throw new ValidatorException(FacesUtils.createErrorMessage("Database URL can not be blank"));
            } else {
                testDBConnection(dbUrl, dbUsername, dbPasswordString);
            }
        }

    }

    public void adValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String domain = (String) ((UIInput) viewRoot.findComponent("step3Form:adDomain")).getValue();
        String url = (String) ((UIInput) viewRoot.findComponent("step3Form:adUrl")).getValue();
        String username = (String) ((UIInput) viewRoot.findComponent("step3Form:adUsername")).getValue();
        String password = (String) ((UIInput)toValidate).getSubmittedValue();

        if (StringUtils.isBlank(domain)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Active Directory domain can not be blank"));
        } else if (StringUtils.isBlank(url)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Active Directory URL can not be blank"));
        } else if (StringUtils.isBlank(username)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Empty username is not allowed"));
        } else if (StringUtils.isBlank(password)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Empty password is not allowed"));
        } else {
            try {
                ActiveDirectoryLdapAuthenticationProvider ldapAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(
                        domain,
                        url);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        username,
                        password);
                Authentication authentication = ldapAuthenticationProvider.authenticate(authenticationToken);
                if (authentication.getAuthorities().isEmpty()) {
                    throw new ValidatorException(FacesUtils.createErrorMessage("User '" + username + "' doesn't have any authority."));
                }
            } catch (AuthenticationException e) {
                throw new ValidatorException(FacesUtils.createErrorMessage(e.getMessage()));
            } catch (RuntimeException e) {
                throw new ValidatorException(FacesUtils.createErrorMessage(getCauseExceptionMessage(e)));
            }
        }
    }

    public void adAdminsValidator(FacesContext context, UIComponent toValidate, Object value) {
        String admins = (String) value;
        if (StringUtils.isBlank(admins) || admins.trim().equals(",")) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Administrators field must not be empty"));
        }
    }

    private String getCauseExceptionMessage(Exception e) {
        String errorMessage = e.getMessage();
        Throwable cause = e.getCause();

        if (cause != null) {
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause.getMessage() != null) {
                errorMessage = cause.getMessage();
            }
        }

        return errorMessage;
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
                        validateIsWritable(studioDir);
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

                    validateIsWritable(studioDir);

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
     */
    private void validateIsWritable(File file) {

        try {
            File tmpFile = File.createTempFile("temp", null, file);
            if (!tmpFile.delete()) {
                log.warn("Can't delete temp file {}", tmpFile.getName());
            }

        } catch (IOException ioe) {
            throw new ValidatorException(FacesUtils.createErrorMessage(ioe.getMessage() + " for '" + file.getAbsolutePath() + "'"));
        }
    }

    /**
     * Deletes the folder which was created for validating folder permissions
     *
     * @param existingFolder folder which already exists on file system
     * @param studioFolder   folder were studio will be installed
     */
    private void deleteFolder(File existingFolder, File studioFolder) {

        if (!studioFolder.delete()) {
            log.warn("Can't delete the folder {}", studioFolder.getName());
        }

        while (!studioFolder.getAbsolutePath().equalsIgnoreCase(existingFolder.getAbsolutePath())) {
            if (!studioFolder.delete()) {
                log.warn("Can't delete the folder {}", studioFolder.getName());
            }
            studioFolder = studioFolder.getParentFile();
        }
    }

    /**
     * Returns collection of properties files for external databases
     */
    private Collection<File> getDBPropertiesFiles() {
        File dbPropFolder = new File(System.getProperty("webapp.root") + "/WEB-INF/conf/db");
        Collection<File> dbPropFiles = new ArrayList<File>();

        if (dbPropFolder.isDirectory()) {
            File[] files = dbPropFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("db-")) {
                        dbPropFiles.add(file);
                    }
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

        for (File propFile : getDBPropertiesFiles()) {
            InputStream is = null;
            try {
                is = new FileInputStream(propFile);
                dbProps.load(is);
                is.close();
                String propertyFilePath = System.getProperty("webapp.root") + "/WEB-INF/conf/db/" + propFile.getName();
                String dbVendor = dbProps.getProperty("db.vendor");

                dbVendors.add(new SelectItem(propertyFilePath, dbVendor));
            } catch (FileNotFoundException e) {
                log.error("The file {} is not found", propFile.getAbsolutePath(), e);
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
            ConfigurationManager externalDBConfig = new ConfigurationManager(false, propertyFilePath);

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
                setDbVendor(propertyFilePath);
                this.dbPrefix = prefix;

                // For Oracle database schema is a username
                if (StringUtils.containsIgnoreCase(propertyFilePath, "oracle")) {
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

    public void groupsAreManagedInStudioChanged(AjaxBehaviorEvent e) {
        UIInput uiInput = (UIInput) e.getComponent();
        groupsAreManagedInStudio = Boolean.valueOf(uiInput.getValue().toString());
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

    public String getGroupsAreManagedInStudio() {
        return "" + groupsAreManagedInStudio;
    }

    public void setGroupsAreManagedInStudio(String groupsAreManagedInStudio) {
        this.groupsAreManagedInStudio = Boolean.parseBoolean(groupsAreManagedInStudio);
    }

    public void setAdAllowAccessToNewUsers(Boolean adAllowAccessToNewUsers) {
        this.adAllowAccessToNewUsers = adAllowAccessToNewUsers;
    }

    public Boolean getAdAllowAccessToNewUsers() {
        return adAllowAccessToNewUsers;
    }


    public void setAdAdmins(String adAdmins) {
        this.adAdmins = adAdmins;
    }

    public String getAdAdmins() {
        return adAdmins;
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

    public String getAdAppMode() {
        return getAppMode();
    }

    public void setAdAppMode(String appMode) {
        if (AD_USER_MODE.equals(userMode)) {
            setAppMode(appMode);
        }
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

    public String getAdDomain() {
        return adDomain;
    }

    public void setAdDomain(String adDomain) {
        this.adDomain = adDomain;
    }

    public String getAdUrl() {
        return adUrl;
    }

    public void setAdUrl(String adUrl) {
        this.adUrl = adUrl;
    }

    public String getAdUsername() {
        return adUsername;
    }

    public void setAdUsername(String adUsername) {
        this.adUsername = adUsername;
    }

    public String getAdPassword() {
        return adPassword;
    }

    public void setAdPassword(String adPassword) {
        this.adPassword = adPassword;
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

    public String getAdDbVendor() {
        return getDbVendor();
    }

    public void setAdDbVendor(String dbVendor) {
        if (AD_USER_MODE.equals(userMode)) {
            setDbVendor(dbVendor);
        }
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
        destroyTemporaryContext();
    }

    private void migrateDatabase(final Map<String, Object> dbProperties) {
        XmlWebApplicationContext ctx = null;
        try {
            ctx = new XmlWebApplicationContext();
            ctx.setServletContext(FacesUtils.getServletContext());
            ctx.setConfigLocations("classpath:META-INF/standalone/spring/security-hibernate-beans.xml",
                    "/WEB-INF/spring/security/db/flyway-bean.xml");
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
        connectionProductionRepoController.clearForm();

        newProductionRepoController = new NewProductionRepoController();
        newProductionRepoController.setProductionConfigManagerFactory(productionConfigManagerFactory);
        newProductionRepoController.setProductionRepositoryFactoryProxy(productionRepositoryFactoryProxy);
        newProductionRepoController.setProductionRepositoryConfigurations(getProductionRepositoryConfigurations());
        newProductionRepoController.clearForm();
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

    private void destroyTemporaryContext() {
        if (temporaryContext != null) {
            groupManagementService.setDelegate(null);
            temporaryContext.close();
            temporaryContext = null;
        }
    }

    public void setGroupManagementService(GroupManagementServiceWrapper groupManagementService) {
        this.groupManagementService = groupManagementService;
    }
}
