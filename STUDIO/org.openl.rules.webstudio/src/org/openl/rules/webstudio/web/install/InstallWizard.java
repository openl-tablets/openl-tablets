package org.openl.rules.webstudio.web.install;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.security.*;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.GroupManagementServiceWrapper;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.admin.*;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.db.JDBCDriverRegister;
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
    private static final String CAS_USER_MODE = "cas";
    private static final String SAML_USER_MODE = "saml";
    private static final String USER_MODE_DEMO = "demo";
    private static final String VIEWERS_GROUP = "Viewers";
    private static final String ADMINISTRATORS_GROUP = "Administrators";

    private final Logger log = LoggerFactory.getLogger(InstallWizard.class);

    private int step;

    private static final String PAGE_PREFIX = "step";
    private static final String PAGE_POSTFIX = "?faces-redirect=true";

    @NotBlank
    private String workingDir;
    private boolean workingDirChanged;
    private boolean showErrorMessage = false;

    private String userMode = "demo";
    /**
     * TODO: Rename the field and properties to something more clear
     */
    private boolean groupsAreManagedInStudio = true;

    @NotBlank
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;

    private String adDomain;
    private String adUrl;
    private String adUsername;
    private String adPassword;

    private CASSettings casSettings;
    private SAMLSettings samlSettings;

    private ConfigurationManager appConfig;
    private ConfigurationManager systemConfig;
    private ConfigurationManager dbConfig;


    private RepositoryConfiguration designRepositoryConfiguration;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;

    private ProductionRepositoryEditor productionRepositoryEditor;
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    // Reuse existing controllers
    private ConnectionProductionRepoController connectionProductionRepoController;
    private NewProductionRepoController newProductionRepoController;

    @ManagedProperty(value="#{groupManagementService}")
    private GroupManagementService groupManagementService;
    private XmlWebApplicationContext temporaryContext;
    private Boolean allowAccessToNewUsers;
    private String externalAdmins;

    public InstallWizard() {
        appConfig = new ConfigurationManager(true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/config.properties");
        workingDir = appConfig.getStringProperty("webstudio.home");
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
        try {
            // Validations
            if (step == 2) {
                try {
                    RepositoryValidators.validate(designRepositoryConfiguration);
                    validateConnectionToDesignRepo(designRepositoryConfiguration, RepositoryMode.DESIGN);

                    if (!isUseDesignRepo()) {
                        RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                        validateConnectionToDesignRepo(deployConfigRepositoryConfiguration,
                                RepositoryMode.DEPLOY_CONFIG);
                    }

                    productionRepositoryEditor.validate();
                } catch (RepositoryValidationException e) {
                    FacesUtils.addErrorMessage(e.getMessage());
                    return null;
                }
            }

            // Go to next step
            ++step;
            if (step == 2) {
                // Get defaults from 'system.properties'
                if (workingDirChanged || systemConfig == null) {
                    systemConfig = new ConfigurationManager(true,
                            workingDir + "/system-settings/system.properties",
                            System.getProperty("webapp.root") + "/WEB-INF/conf/system.properties");
                    String repoPassKey = StringUtils.trimToEmpty(systemConfig.getStringProperty(ConfigurationManager.REPO_PASS_KEY));
                    // Make it globally available. It will not be changed during application execution.
                    System.setProperty(ConfigurationManager.REPO_PASS_KEY, repoPassKey);

                    designRepositoryConfiguration = new RepositoryConfiguration("", systemConfig, RepositoryMode.DESIGN);
                    deployConfigRepositoryConfiguration = new RepositoryConfiguration("", systemConfig, RepositoryMode.DEPLOY_CONFIG);

                    initProductionRepositoryEditor();

                    dbConfig = new ConfigurationManager(true,
                            workingDir + "/system-settings/db.properties",
                            System.getProperty("webapp.root") + "/WEB-INF/conf/db.properties");

                    userMode = systemConfig.getStringProperty("user.mode");
                }
            } else if (step == 3) {
                readDbProperties();
                readAdProperties();
                readCasProperties();
                readSamlProperties();

                switch (userMode) {
                    case AD_USER_MODE:
                        groupsAreManagedInStudio = systemConfig.getBooleanProperty("security.ad.groups-are-managed-in-studio");
                        allowAccessToNewUsers = !StringUtils.isBlank(systemConfig.getStringProperty("security.ad.default-group"));
                        break;
                    case CAS_USER_MODE:
                        groupsAreManagedInStudio = StringUtils.isBlank(systemConfig.getStringProperty("security.cas.attribute.groups"));
                        allowAccessToNewUsers = !StringUtils.isBlank(systemConfig.getStringProperty("security.cas.default-group"));
                        break;
                    case SAML_USER_MODE:
                        groupsAreManagedInStudio = StringUtils.isBlank(systemConfig.getStringProperty("security.saml.attribute.groups"));
                        allowAccessToNewUsers = !StringUtils.isBlank(systemConfig.getStringProperty("security.saml.default-group"));
                        break;
                }
            } else if (step == 4) {
                initializeTemporaryContext();

                if (groupManagementService instanceof GroupManagementServiceWrapper) {
                    // GroupManagementService delegate is transactional and properly initialized
                    GroupManagementService delegate = (GroupManagementService) temporaryContext.getBean("groupManagementService");
                    // Initialize groupManagementService before first usage in GroupsBean
                    ((GroupManagementServiceWrapper) groupManagementService).setDelegate(delegate);
                }
            }
            return PAGE_PREFIX + step + PAGE_POSTFIX;
        } catch (Exception e) {
            log.error("Failed while saving the configuration", e);
            if (e.getCause() instanceof FlywayException) {
                FacesUtils.addErrorMessage("Cannot migrate the database. Check the logs for details.");
            } else {
                FacesUtils.addErrorMessage("Cannot save the configuration. Check the logs for details.");
            }
            step--;
            return null;
        }
    }

    private void validateConnectionToDesignRepo(RepositoryConfiguration designRepositoryConfiguration,
            RepositoryMode repositoryMode) throws RepositoryValidationException {
        try {
            Map<String, Object> config = designRepositoryConfiguration.getProperties();
            Repository repository = RepositoryFactoryInstatiator.newFactory(config, repositoryMode);
            if (repository instanceof Closeable) {
                // Release resources after validation
                IOUtils.closeQuietly((Closeable) repository);
            }
        } catch (RRepositoryException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String message = "Incorrect Design Repository configuration: " + (rootCause == null ?
                                                                              e.getMessage() :
                                                                              rootCause.getMessage());
            throw new RepositoryValidationException(message, e);
        }
    }

    private void initializeTemporaryContext() {
        destroyTemporaryContext();

        setProductionDbProperties();

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
        dbUrl = dbConfig.getStringProperty("db.url");
        dbUsername = dbConfig.getStringProperty("db.user");
        dbPassword = dbConfig.getStringProperty("db.password");
    }

    private void readAdProperties() {
        adDomain = systemConfig.getStringProperty("security.ad.domain");
        adUrl = systemConfig.getStringProperty("security.ad.server-url");
    }

    private void readCasProperties() {
        casSettings = new CASSettings(
                systemConfig.getStringProperty("security.cas.app-url"),
                systemConfig.getStringProperty("security.cas.cas-server-url-prefix"),
                systemConfig.getStringProperty("security.cas.default-group"),
                systemConfig.getStringProperty("security.cas.attribute.first-name"),
                systemConfig.getStringProperty("security.cas.attribute.last-name"),
                systemConfig.getStringProperty("security.cas.attribute.groups")
        );
    }

    private void readSamlProperties() {
        samlSettings = new SAMLSettings(
                systemConfig.getStringProperty("security.saml.app-url"),
                systemConfig.getStringProperty("security.saml.saml-server-metadata-url"),
                systemConfig.getIntegerProperty("security.saml.request-timeout"),
                systemConfig.getStringProperty("security.saml.keystore-file-path"),
                systemConfig.getStringProperty("security.saml.keystore-password"),
                systemConfig.getStringProperty("security.saml.keystore-sp-alias"),
                systemConfig.getStringProperty("security.saml.keystore-sp-password"),
                systemConfig.getStringProperty("security.saml.default-group"),
                systemConfig.getStringProperty("security.saml.attribute.username"),
                systemConfig.getStringProperty("security.saml.attribute.first-name"),
                systemConfig.getStringProperty("security.saml.attribute.last-name"),
                systemConfig.getStringProperty("security.saml.attribute.groups"),
                systemConfig.getStringProperty("security.saml.authentication-contexts"),
                systemConfig.getBooleanProperty("security.saml.local-logout"));
    }

    public String finish() {
        try {
            if (MULTI_USER_MODE.equals(userMode)) {
                setProductionDbProperties();
                migrateDatabase(dbConfig.getProperties());

                dbConfig.save();
            } else {
                if (AD_USER_MODE.equals(userMode)) {
                    fillDbForUserManagement();
                    dbConfig.save();

                    systemConfig.setProperty("security.ad.domain", adDomain);
                    systemConfig.setProperty("security.ad.server-url", adUrl);
                    systemConfig.setProperty("security.ad.groups-are-managed-in-studio", groupsAreManagedInStudio);
                    systemConfig.setProperty("security.ad.default-group", allowAccessToNewUsers ? VIEWERS_GROUP : "");
                } else if (CAS_USER_MODE.equals(userMode)) {
                    fillDbForUserManagement();
                    dbConfig.save();

                    casSettings.setDefaultGroup(allowAccessToNewUsers ? VIEWERS_GROUP : "");

                    systemConfig.setProperty("security.cas.app-url", casSettings.getWebStudioUrl());
                    systemConfig.setProperty("security.cas.cas-server-url-prefix", casSettings.getCasServerUrl());
                    systemConfig.setProperty("security.cas.default-group", casSettings.getDefaultGroup());
                    systemConfig.setProperty("security.cas.attribute.first-name", casSettings.getFirstNameAttribute());
                    systemConfig.setProperty("security.cas.attribute.last-name", casSettings.getSecondNameAttribute());
                    systemConfig.setProperty("security.cas.attribute.groups", casSettings.getGroupsAttribute());
                } else if (SAML_USER_MODE.equals(userMode)) {
                    fillDbForUserManagement();
                    dbConfig.save();

                    samlSettings.setDefaultGroup(allowAccessToNewUsers ? VIEWERS_GROUP : "");

                    systemConfig.setProperty("security.saml.app-url", samlSettings.getWebStudioUrl());
                    systemConfig.setProperty("security.saml.saml-server-metadata-url", samlSettings.getSamlServerMetadataUrl());
                    systemConfig.setProperty("security.saml.request-timeout", samlSettings.getRequestTimeout());
                    systemConfig.setProperty("security.saml.keystore-file-path", samlSettings.getKeystoreFilePath());
                    systemConfig.setProperty("security.saml.keystore-password", samlSettings.getKeystorePassword());
                    systemConfig.setProperty("security.saml.keystore-sp-alias", samlSettings.getKeystoreSpAlias());
                    systemConfig.setProperty("security.saml.keystore-sp-password", samlSettings.getKeystoreSpPassword());
                    systemConfig.setProperty("security.saml.default-group", samlSettings.getDefaultGroup());
                    systemConfig.setProperty("security.saml.attribute.username", samlSettings.getUsernameAttribute());
                    systemConfig.setProperty("security.saml.attribute.first-name", samlSettings.getFirstNameAttribute());
                    systemConfig.setProperty("security.saml.attribute.last-name", samlSettings.getSecondNameAttribute());
                    systemConfig.setProperty("security.saml.attribute.groups", samlSettings.getGroupsAttribute());
                    systemConfig.setProperty("security.saml.authentication-contexts", samlSettings.getAuthenticationContexts());
                    systemConfig.setProperty("security.saml.local-logout", samlSettings.isLocalLogout());
                } else {
                    dbConfig.restoreDefaults();
                }
            }

            productionRepositoryEditor.save();

            systemConfig.setProperty("user.mode", userMode);

            // TODO: This line also do systemConfig.save() implicitly
            designRepositoryConfiguration.save();
            if (!isUseDesignRepo()) {
                deployConfigRepositoryConfiguration.save();
            }

            System.clearProperty("webstudio.home"); // Otherwise this property will not be saved to file.
            appConfig.setProperty("webstudio.home", workingDir);
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

    private void fillDbForUserManagement() {
        if (groupsAreManagedInStudio) {
            initializeTemporaryContext();
            GroupManagementService groupManagementService = (GroupManagementService) temporaryContext.getBean(
                    "groupManagementService");
            UserManagementService userManagementService = (UserManagementService) temporaryContext.getBean(
                    "userManagementService");

            if (allowAccessToNewUsers) {
                if (!groupManagementService.isGroupExist(VIEWERS_GROUP)) {
                    Group group = new SimpleGroup(VIEWERS_GROUP, null,
                            new ArrayList<>(Collections.singletonList(Privileges.VIEW_PROJECTS)));
                    groupManagementService.addGroup(group);
                }
            }

            if (!groupManagementService.isGroupExist(ADMINISTRATORS_GROUP)) {
                Group group = new SimpleGroup(ADMINISTRATORS_GROUP, null,
                        new ArrayList<>(Collections.singletonList(Privileges.ADMIN)));
                groupManagementService.addGroup(group);
            }
            Group administrator = groupManagementService.getGroupByName(ADMINISTRATORS_GROUP);
            List<Privilege> adminGroups = new ArrayList<>(Collections.singleton(administrator));

            // Delete example users
            for (User user : userManagementService.getAllUsers()) {
                userManagementService.deleteUser(user.getUsername());
            }

            // Create admin users
            for (String username : StringUtils.split(externalAdmins, ',')) {
                if (!username.isEmpty()) {
                    userManagementService.addUser(new SimpleUser(null, null, username, null, adminGroups));
                }
            }
            setProductionDbProperties();
            migrateDatabase(dbConfig.getProperties());
        } else {
            setProductionDbProperties();
        }
    }

    private void setProductionDbProperties() {
        dbConfig.setProperty("db.url", dbUrl);
        dbConfig.setProperty("db.user", dbUsername);
        dbConfig.setProperty("db.password", dbPassword);
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
        Connection conn;

        try {
            JDBCDriverRegister.registerDrivers();
            if (StringUtils.isBlank(login)) {
                conn = DriverManager.getConnection(url);
            } else {
                conn = DriverManager.getConnection(url, login, password);
            }
        } catch (SQLException sqle) {
            int errorCode = sqle.getErrorCode();
            log.error("Code: {}. {}", errorCode, sqle.getMessage(), sqle);
            final String SQL_ERRORS_FILE_PATH = "/sql-errors.properties";
            String errorMessage = null;
            try {
                Properties properties = new Properties();
                properties.load(getClass().getResourceAsStream(SQL_ERRORS_FILE_PATH));
                errorMessage = properties.getProperty(Integer.toString(errorCode));
            } catch (Exception e) {
                log.error("Cannot to load {} file.", SQL_ERRORS_FILE_PATH, e);
            }
            if (errorMessage == null) {
                errorMessage = "Incorrect database URL, login or password";
            }

            throw new ValidatorException(FacesUtils.createErrorMessage(errorMessage));
        }

        try {
            conn.close();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void dbValidator(FacesContext context, UIComponent toValidate, Object value) {
        String dbPasswordString = (String) value;

        if (!USER_MODE_DEMO.equals(userMode)) {
            if (StringUtils.isEmpty(dbUrl)) {
                throw new ValidatorException(FacesUtils.createErrorMessage("Database URL can not be blank"));
            } else {
                if (StringUtils.isNotEmpty(dbUsername) && dbUsername.length() > 100) {
                    throw new ValidatorException(FacesUtils.createErrorMessage("Username length must be less than 100"));
                }
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
        }
        if (StringUtils.isBlank(url)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Active Directory URL can not be blank"));
        }

        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            try {
                ActiveDirectoryLdapAuthenticationProvider ldapAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(
                        domain,
                        url);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        username,
                        password);
                Authentication authentication = ldapAuthenticationProvider.authenticate(authenticationToken);
            } catch (AuthenticationException e) {
                throw new ValidatorException(FacesUtils.createErrorMessage(e.getMessage()));
            } catch (RuntimeException e) {
                throw new ValidatorException(FacesUtils.createErrorMessage(getCauseExceptionMessage(e)));
            }
        }
    }

    public void casValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String webStudioUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:casWebStudioUrl")).getSubmittedValue();
        String serverUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:casServerUrl")).getSubmittedValue();
        String groupsAttribute = (String) ((UIInput) viewRoot.findComponent("step3Form:casGroupsAttribute")).getSubmittedValue();

        if (StringUtils.isBlank(webStudioUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio server URL can not be blank"));
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("CAS server url can not be blank"));
        }

        if (!groupsAreManagedInStudio && StringUtils.isBlank(groupsAttribute)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Attribute for Groups can not be blank or Internal User Management must be selected"));
        }
    }

    public void samlValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String webStudioUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:samlWebStudioUrl")).getSubmittedValue();
        String serverUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:samlServerUrl")).getSubmittedValue();
        String requestTimeout = (String) ((UIInput) viewRoot.findComponent("step3Form:samlRequestTimeout")).getSubmittedValue();
        String keystoreFilePath = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystoreFilePath")).getSubmittedValue();
        String keystorePassword = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystorePassword")).getSubmittedValue();
        String keystoreSpAlias = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystoreSpAlias")).getSubmittedValue();
        String keystoreSpPassword = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystoreSpPassword")).getSubmittedValue();
        String groupsAttribute = (String) ((UIInput) viewRoot.findComponent("step3Form:samlGroupsAttribute")).getSubmittedValue();

        if (StringUtils.isBlank(webStudioUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio server URL can not be blank"));
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("SAML server metadata url can not be blank"));
        }

        if (StringUtils.isBlank(requestTimeout)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Request timeout can not be blank"));
        }

        if (StringUtils.isBlank(keystoreFilePath)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore path can not be blank"));
        }

        if (StringUtils.isBlank(keystorePassword)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore password can not be blank"));
        }

        if (StringUtils.isBlank(keystoreSpAlias)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore SP alias can not be blank"));
        }

        if (StringUtils.isBlank(keystoreSpPassword)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore SP password can not be blank"));
        }

        if (!groupsAreManagedInStudio && StringUtils.isBlank(groupsAttribute)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Attribute for Groups can not be blank or Internal User Management must be selected"));
        }
    }

    public void externalAdminsValidator(FacesContext context, UIComponent toValidate, Object value) {
        String admins = (String) value;
        if (StringUtils.isBlank(admins) || admins.trim().equals(",")) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Administrators field must not be empty"));
        }

        String[] allAdmins = StringUtils.split(admins, ',');
        for (String admin : allAdmins) {
            if (admin.length() > 50) {
                throw new ValidatorException(FacesUtils.createErrorMessage("Administrator username length must be less than 50"));
            }
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
            studioPath = (String) value;
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
                File parentFolder = studioDir.getAbsoluteFile().getParentFile();
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
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio working directory can not be blank"));
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
        if (studioFolder.exists() && !studioFolder.delete()) {
            log.warn("Can't delete the folder {}", studioFolder.getName());
        }

        if (existingFolder == null) {
            return;
        }

        while (!studioFolder.getAbsolutePath().equalsIgnoreCase(existingFolder.getAbsolutePath())) {
            if (studioFolder.exists() && !studioFolder.delete()) {
                log.warn("Can't delete the folder {}", studioFolder.getName());
            }
            studioFolder = studioFolder.getAbsoluteFile().getParentFile();
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
        workingDirChanged = !workingDir.equals(this.workingDir);
        this.workingDir = workingDir;

        // Other configurations depend on this property
        System.setProperty("webstudio.home", this.workingDir);
    }

    public String getGroupsAreManagedInStudio() {
        return "" + groupsAreManagedInStudio;
    }

    public void setGroupsAreManagedInStudio(String groupsAreManagedInStudio) {
        this.groupsAreManagedInStudio = Boolean.parseBoolean(groupsAreManagedInStudio);
    }

    public void setAllowAccessToNewUsers(Boolean allowAccessToNewUsers) {
        this.allowAccessToNewUsers = allowAccessToNewUsers;
    }

    public Boolean getAllowAccessToNewUsers() {
        return allowAccessToNewUsers;
    }


    public void setExternalAdmins(String externalAdmins) {
        this.externalAdmins = externalAdmins;
    }

    public String getExternalAdmins() {
        return externalAdmins;
    }

    public String getUserMode() {
        return userMode;
    }

    public void setUserMode(String userMode) {
        this.userMode = userMode;
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

    public CASSettings getCasSettings() {
        return casSettings;
    }

    public SAMLSettings getSamlSettings() {
        return samlSettings;
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

    public RepositoryConfiguration getDesignRepositoryConfiguration() {
        return designRepositoryConfiguration;
    }

    public RepositoryConfiguration getDeployConfigRepositoryConfiguration() {
        return deployConfigRepositoryConfiguration;
    }

    public boolean isUseDesignRepo() {
        return !Boolean.parseBoolean(systemConfig.getStringProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO));
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        systemConfig.setProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO, !useDesignRepo);
    }

    public FolderStructureSettings getDesignFolderStructure() {
        return new FolderStructureSettings(systemConfig, RepositoryMode.DESIGN);
    }

    public FolderStructureSettings getDeployConfigFolderStructure() {
        return new FolderStructureSettings(systemConfig, RepositoryMode.DEPLOY_CONFIG);
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
        try (XmlWebApplicationContext ctx = new XmlWebApplicationContext()) {
            ctx.setServletContext(FacesUtils.getServletContext());
            ctx.setConfigLocations("classpath:META-INF/standalone/spring/security-hibernate-beans.xml");
            ctx.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                    beanFactory.registerSingleton("dbConfig", dbProperties);
                }
            });
            ctx.refresh();
            ctx.getBean("dbMigration");
        }
    }

    private void initProductionRepositoryEditor() {
        destroyRepositoryObjects();

        final ConfigurationManagerFactory productionConfigManagerFactory = new ConfigurationManagerFactory(
                true,
                System.getProperty("webapp.root") + "/WEB-INF/conf/rules-production.properties",
                workingDir + "/system-settings/",
                System.getProperty("webapp.root") + "/WEB-INF/conf/"
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
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            productionRepositoryFactoryProxy = null;
        }
    }

    private void destroyTemporaryContext() {
        if (temporaryContext != null) {
            if (groupManagementService instanceof GroupManagementServiceWrapper) {
                ((GroupManagementServiceWrapper) groupManagementService).setDelegate(null);
            }
            temporaryContext.close();
            temporaryContext = null;
        }
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }
}
