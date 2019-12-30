package org.openl.rules.webstudio.web.install;

import javax.servlet.ServletContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigNames;
import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.filter.ReloadableDelegatingFilter;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.GroupManagementServiceWrapper;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.util.PreferencesManager;
import org.openl.rules.webstudio.web.admin.ConnectionProductionRepoController;
import org.openl.rules.webstudio.web.admin.FolderStructureSettings;
import org.openl.rules.webstudio.web.admin.ProductionRepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.rules.webstudio.web.admin.RepositoryValidators;
import org.openl.rules.webstudio.web.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.spring.env.PropertySourcesLoader;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.db.JDBCDriverRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.annotation.PostConstruct;
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
import javax.naming.directory.InvalidSearchFilterException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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
    private String ldapFilter;

    private CASSettings casSettings;
    private SAMLSettings samlSettings;

    private RepositoryConfiguration designRepositoryConfiguration;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;

    private ProductionRepositoryEditor productionRepositoryEditor;
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;

    // Reuse existing controllers
    private ConnectionProductionRepoController connectionProductionRepoController;

    @ManagedProperty(value = "#{groupManagementService}")
    private GroupManagementService groupManagementService;
    private XmlWebApplicationContext dbContext;
    private Boolean allowAccessToNewUsers;
    private String externalAdmins;

    @ManagedProperty(value = "#{environment}")
    private PropertyResolver propertyResolver;
    private PropertiesHolder properties;

    @PostConstruct
    public void init() {
        workingDir = propertyResolver.getProperty(PreferencesManager.WEBSTUDIO_WORKING_DIR_KEY);
        workingDirChanged = true;
    }

    public String start() {
        step = 1;
        return PAGE_PREFIX + step + PAGE_POSTFIX;
    }

    public String reconfigure() {
        PreferencesManager.INSTANCE.setInstallerMode(getAppName());
        ReloadableDelegatingFilter.reloadApplicationContext(FacesUtils.getServletContext());
        return next();
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
                    validateConnectionToDesignRepo(designRepositoryConfiguration, ConfigNames.DESIGN_CONFIG);

                    if (!isUseDesignRepo()) {
                        RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                        validateConnectionToDesignRepo(deployConfigRepositoryConfiguration, ConfigNames.DEPLOY_CONFIG);
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
                // Get defaults
                if (workingDirChanged) {
                    designRepositoryConfiguration = new RepositoryConfiguration(ConfigNames.DESIGN_CONFIG, properties);
                    if (designRepositoryConfiguration.getErrorMessage() != null) {
                        log.error(designRepositoryConfiguration.getErrorMessage());
                    }

                    deployConfigRepositoryConfiguration = new RepositoryConfiguration(ConfigNames.DEPLOY_CONFIG,
                            properties);
                    if (deployConfigRepositoryConfiguration.getErrorMessage() != null) {
                        log.error(deployConfigRepositoryConfiguration.getErrorMessage());
                    }

                    initProductionRepositoryEditor();

                    userMode = propertyResolver.getProperty("user.mode");

                    workingDirChanged = false;
                }
            } else if (step == 3) {
                readDbProperties();
                readAdProperties();
                readCasProperties();
                readSamlProperties();

                switch (userMode) {
                    case AD_USER_MODE:
                        groupsAreManagedInStudio = propertyResolver
                                .getRequiredProperty("security.ad.groups-are-managed-in-studio", Boolean.class);
                        allowAccessToNewUsers = !StringUtils
                                .isBlank(propertyResolver.getProperty("security.ad.default-group"));
                        break;
                    case CAS_USER_MODE:
                        groupsAreManagedInStudio = StringUtils
                                .isBlank(propertyResolver.getProperty("security.cas.attribute.groups"));
                        allowAccessToNewUsers = !StringUtils
                                .isBlank(propertyResolver.getProperty("security.cas.default-group"));
                        break;
                    case SAML_USER_MODE:
                        groupsAreManagedInStudio = StringUtils
                                .isBlank(propertyResolver.getProperty("security.saml.attribute.groups"));
                        allowAccessToNewUsers = !StringUtils
                                .isBlank(propertyResolver.getProperty("security.saml.default-group"));
                        break;
                }
            } else if (step == 4) {
                initializeDbContext();

                if (groupManagementService instanceof GroupManagementServiceWrapper) {
                    // GroupManagementService delegate is transactional and properly initialized
                    GroupManagementService delegate = (GroupManagementService) dbContext
                            .getBean("groupManagementService");
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
                                                String configName) throws RepositoryValidationException {
        try {
            PropertyResolver propertiesResolver = DelegatedPropertySource
                    .createPropertiesResolver(designRepositoryConfiguration.getPropertiesToValidate());
            Repository repository = RepositoryInstatiator.newRepository(configName, propertiesResolver);
            if (repository instanceof Closeable) {
                // Release resources after validation
                IOUtils.closeQuietly((Closeable) repository);
            }
        } catch (RRepositoryException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            String message = "Incorrect Design Repository configuration: " + (rootCause == null ? e
                    .getMessage() : rootCause.getMessage());
            throw new RepositoryValidationException(message, e);
        }
    }

    private void initializeDbContext() {
        destroyDbContext();

        setProductionDbProperties();

        migrateDatabase();

        // Load groupDao and initialize groupManagementService
        dbContext = new XmlWebApplicationContext();
        dbContext.setServletContext(FacesUtils.getServletContext());
        dbContext.setConfigLocations("/WEB-INF/spring/security/db-services.xml");
        dbContext.addBeanFactoryPostProcessor(beanFactory -> beanFactory.registerSingleton("propertyLoader",
                new DelegatedPropertySourceLoader(properties)));
        dbContext.refresh();
    }

    private void readDbProperties() {
        dbUrl = properties.getProperty("db.url");
        dbUsername = properties.getProperty("db.user");
        dbPassword = properties.getProperty("db.password");
    }

    private void readAdProperties() {
        adDomain = propertyResolver.getProperty("security.ad.domain");
        adUrl = propertyResolver.getProperty("security.ad.server-url");
        ldapFilter = propertyResolver.getProperty("security.ad.search-filter");
    }

    private void readCasProperties() {
        casSettings = new CASSettings(propertyResolver.getProperty("security.cas.app-url"),
                propertyResolver.getProperty("security.cas.cas-server-url-prefix"),
                propertyResolver.getProperty("security.cas.default-group"),
                propertyResolver.getProperty("security.cas.attribute.first-name"),
                propertyResolver.getProperty("security.cas.attribute.last-name"),
                propertyResolver.getProperty("security.cas.attribute.groups"));
    }

    private void readSamlProperties() {
        samlSettings = new SAMLSettings(propertyResolver.getProperty("security.saml.app-url"),
                propertyResolver.getProperty("security.saml.saml-server-metadata-url"),
                propertyResolver.getRequiredProperty("security.saml.request-timeout", Integer.class),
                propertyResolver.getProperty("security.saml.keystore-file-path"),
                propertyResolver.getProperty("security.saml.keystore-password"),
                propertyResolver.getProperty("security.saml.keystore-sp-alias"),
                propertyResolver.getProperty("security.saml.keystore-sp-password"),
                propertyResolver.getProperty("security.saml.default-group"),
                propertyResolver.getProperty("security.saml.attribute.username"),
                propertyResolver.getProperty("security.saml.attribute.first-name"),
                propertyResolver.getProperty("security.saml.attribute.last-name"),
                propertyResolver.getProperty("security.saml.attribute.groups"),
                propertyResolver.getProperty("security.saml.authentication-contexts"),
                propertyResolver.getRequiredProperty("security.saml.local-logout", Boolean.class));
    }

    public String finish() {
        try {
            if (MULTI_USER_MODE.equals(userMode)) {
                setProductionDbProperties();
                migrateDatabase();
            } else {
                if (AD_USER_MODE.equals(userMode)) {
                    fillDbForUserManagement();

                    properties.setProperty("security.ad.domain", adDomain);
                    properties.setProperty("security.ad.server-url", adUrl);
                    properties.setProperty("security.ad.search-filter", ldapFilter);
                    properties.setProperty("security.ad.groups-are-managed-in-studio", groupsAreManagedInStudio);
                    properties.setProperty("security.ad.default-group", allowAccessToNewUsers ? VIEWERS_GROUP : "");
                } else if (CAS_USER_MODE.equals(userMode)) {
                    fillDbForUserManagement();

                    casSettings.setDefaultGroup(allowAccessToNewUsers ? VIEWERS_GROUP : "");

                    properties.setProperty("security.cas.app-url", casSettings.getWebStudioUrl());
                    properties.setProperty("security.cas.cas-server-url-prefix", casSettings.getCasServerUrl());
                    properties.setProperty("security.cas.default-group", casSettings.getDefaultGroup());
                    properties.setProperty("security.cas.attribute.first-name", casSettings.getFirstNameAttribute());
                    properties.setProperty("security.cas.attribute.last-name", casSettings.getSecondNameAttribute());
                    properties.setProperty("security.cas.attribute.groups", casSettings.getGroupsAttribute());
                } else if (SAML_USER_MODE.equals(userMode)) {
                    fillDbForUserManagement();

                    samlSettings.setDefaultGroup(allowAccessToNewUsers ? VIEWERS_GROUP : "");

                    properties.setProperty("security.saml.app-url", samlSettings.getWebStudioUrl());
                    properties.setProperty("security.saml.saml-server-metadata-url",
                            samlSettings.getSamlServerMetadataUrl());
                    properties.setProperty("security.saml.request-timeout", samlSettings.getRequestTimeout());
                    properties.setProperty("security.saml.keystore-file-path", samlSettings.getKeystoreFilePath());
                    properties.setProperty("security.saml.keystore-password", samlSettings.getKeystorePassword());
                    properties.setProperty("security.saml.keystore-sp-alias", samlSettings.getKeystoreSpAlias());
                    properties.setProperty("security.saml.keystore-sp-password", samlSettings.getKeystoreSpPassword());
                    properties.setProperty("security.saml.default-group", samlSettings.getDefaultGroup());
                    properties.setProperty("security.saml.attribute.username", samlSettings.getUsernameAttribute());
                    properties.setProperty("security.saml.attribute.first-name", samlSettings.getFirstNameAttribute());
                    properties.setProperty("security.saml.attribute.last-name", samlSettings.getSecondNameAttribute());
                    properties.setProperty("security.saml.attribute.groups", samlSettings.getGroupsAttribute());
                    properties.setProperty("security.saml.authentication-contexts",
                            samlSettings.getAuthenticationContexts());
                    properties.setProperty("security.saml.local-logout", samlSettings.isLocalLogout());
                }
            }

            productionRepositoryEditor.save();

            properties.setProperty("user.mode", userMode);

            designRepositoryConfiguration.commit();
            if (!isUseDesignRepo()) {
                deployConfigRepositoryConfiguration.commit();
            }
            properties.writeTo(new File(workingDir, getAppName() + ".properties"));

            PreferencesManager.INSTANCE.webStudioConfigured(getAppName());

            destroyRepositoryObjects();
            destroyDbContext();

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
            initializeDbContext();
            GroupManagementService groupManagementService = (GroupManagementService) dbContext
                    .getBean("groupManagementService");
            UserManagementService userManagementService = (UserManagementService) dbContext
                    .getBean("userManagementService");

            if (allowAccessToNewUsers) {
                if (!groupManagementService.isGroupExist(VIEWERS_GROUP)) {
                    Group group = new SimpleGroup(VIEWERS_GROUP,
                            null,
                            new ArrayList<>(Collections.singletonList(Privileges.VIEW_PROJECTS)));
                    groupManagementService.addGroup(group);
                }
            }

            if (!groupManagementService.isGroupExist(ADMINISTRATORS_GROUP)) {
                Group group = new SimpleGroup(ADMINISTRATORS_GROUP,
                        null,
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
            migrateDatabase();
        } else {
            setProductionDbProperties();
        }
    }

    private void setProductionDbProperties() {
        properties.setProperty("db.url", dbUrl);
        properties.setProperty("db.user", dbUsername);
        properties.setProperty("db.password", dbPassword);
    }

    /**
     * Methods tests connection to DB. Depending on the SQL error code corresponding validate exception will be thrown
     * SQL errors loading from sql-errors.properties.
     */

    /*
     * If a new database is added to the project, just add new sql error into the file sql-errors.properties
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
            log.error("Code: {}. {}.", errorCode, sqle.getMessage(), sqle);
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
                errorMessage = "Incorrect database URL, login or password.";
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
                throw new ValidatorException(FacesUtils.createErrorMessage("Database URL cannot be blank."));
            } else {
                if (StringUtils.isNotEmpty(dbUsername) && dbUsername.length() > 100) {
                    throw new ValidatorException(
                            FacesUtils.createErrorMessage("Username length must be less than 100."));
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
        String ldapFilter = (String) ((UIInput) viewRoot.findComponent("step3Form:ldapFilter")).getValue();
        String password = (String) ((UIInput) toValidate).getSubmittedValue();

        if (StringUtils.isBlank(domain)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Active Directory domain cannot be blank."));
        }
        if (StringUtils.isBlank(url)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Active Directory URL cannot be blank."));
        }

        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            try {
                ActiveDirectoryLdapAuthenticationProvider ldapAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(
                        domain,
                        url);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        username,
                        password);
                ldapAuthenticationProvider.setSearchFilter(ldapFilter);
                ldapAuthenticationProvider.authenticate(authenticationToken);
            } catch (AuthenticationException e) {
                if (e.getCause() instanceof InvalidSearchFilterException) {
                    String message = "Invalid search filter: " + e.getCause().getMessage();
                    throw new ValidatorException(FacesUtils.createErrorMessage(message));
                }
                throw new ValidatorException(FacesUtils.createErrorMessage(e.getMessage()));
            } catch (RuntimeException e) {
                throw new ValidatorException(FacesUtils.createErrorMessage(getCauseExceptionMessage(e)));
            }
        }
    }

    public void casValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String webStudioUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:casWebStudioUrl"))
                .getSubmittedValue();
        String serverUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:casServerUrl")).getSubmittedValue();
        String groupsAttribute = (String) ((UIInput) viewRoot.findComponent("step3Form:casGroupsAttribute"))
                .getSubmittedValue();

        if (StringUtils.isBlank(webStudioUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio server URL cannot be blank."));
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("CAS server url cannot be blank."));
        }

        if (!groupsAreManagedInStudio && StringUtils.isBlank(groupsAttribute)) {
            throw new ValidatorException(FacesUtils.createErrorMessage(
                    "Attribute for Groups cannot be blank or Internal User Management must be selected."));
        }
    }

    public void samlValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String webStudioUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:samlWebStudioUrl"))
                .getSubmittedValue();
        String serverUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:samlServerUrl")).getSubmittedValue();
        String requestTimeout = (String) ((UIInput) viewRoot.findComponent("step3Form:samlRequestTimeout"))
                .getSubmittedValue();
        String keystoreFilePath = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystoreFilePath"))
                .getSubmittedValue();
        String keystorePassword = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystorePassword"))
                .getSubmittedValue();
        String keystoreSpAlias = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystoreSpAlias"))
                .getSubmittedValue();
        String keystoreSpPassword = (String) ((UIInput) viewRoot.findComponent("step3Form:samlKeystoreSpPassword"))
                .getSubmittedValue();
        String groupsAttribute = (String) ((UIInput) viewRoot.findComponent("step3Form:samlGroupsAttribute"))
                .getSubmittedValue();

        if (StringUtils.isBlank(webStudioUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio server URL cannot be blank."));
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("SAML server metadata url cannot be blank."));
        }

        if (StringUtils.isBlank(requestTimeout)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Request timeout cannot be blank."));
        }

        if (StringUtils.isBlank(keystoreFilePath)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore path cannot be blank."));
        }

        if (StringUtils.isBlank(keystorePassword)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore password cannot be blank."));
        }

        if (StringUtils.isBlank(keystoreSpAlias)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore SP alias cannot be blank."));
        }

        if (StringUtils.isBlank(keystoreSpPassword)) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Keystore SP password cannot be blank."));
        }

        if (!groupsAreManagedInStudio && StringUtils.isBlank(groupsAttribute)) {
            throw new ValidatorException(FacesUtils.createErrorMessage(
                    "Attribute for Groups cannot be blank or Internal User Management must be selected."));
        }
    }

    public void externalAdminsValidator(FacesContext context, UIComponent toValidate, Object value) {
        String admins = (String) value;
        if (StringUtils.isBlank(admins) || admins.trim().equals(",")) {
            throw new ValidatorException(FacesUtils.createErrorMessage("Administrators field must not be empty."));
        }

        String[] allAdmins = StringUtils.split(admins, ',');
        for (String admin : allAdmins) {
            if (admin.length() > 50) {
                throw new ValidatorException(
                        FacesUtils.createErrorMessage("Administrator username length must be less than 50."));
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
     * Validates WebStudio working directory for write access. If specified folder is not writable the validation error
     * will appears
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
                         * If canWrite() returns true the temp file will be created. It's needed because in Windows OS
                         * method canWrite() returns true if folder is not marked 'read only' but such folders can have
                         * security permissions 'deny all'
                         */
                        validateIsWritable(studioDir);
                    } else {
                        throw new ValidatorException(FacesUtils.createErrorMessage(String.format(
                                "There is not enough access rights for installing WebStudio into the folder: '%s'.",
                                studioPath)));
                    }
                } else {
                    throw new ValidatorException(
                            FacesUtils.createErrorMessage(String.format("'%s' is not a folder.", studioPath)));
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
            throw new ValidatorException(FacesUtils.createErrorMessage("WebStudio working directory cannot be blank."));
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
                log.warn("Cannot delete temp file {}.", tmpFile.getName());
            }

        } catch (IOException ioe) {
            throw new ValidatorException(
                    FacesUtils.createErrorMessage(String.format("%s for '%s'", ioe.getMessage(), file.getAbsolutePath())));
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
            log.warn("Cannot delete the folder {}.", studioFolder.getName());
        }

        if (existingFolder == null) {
            return;
        }

        while (!studioFolder.getAbsolutePath().equalsIgnoreCase(existingFolder.getAbsolutePath())) {
            if (studioFolder.exists() && !studioFolder.delete()) {
                log.warn("Cannot delete the folder {}.", studioFolder.getName());
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
        groupsAreManagedInStudio = Boolean.parseBoolean(uiInput.getValue().toString());
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

        workingDirChanged = workingDirChanged || !workingDir.equals(this.workingDir);
        this.workingDir = workingDir;

        // Other configurations depend on this property
        PreferencesManager.INSTANCE.setWebStudioHomeDir(getAppName(), this.workingDir);
        StandardServletEnvironment environment = new StandardServletEnvironment();
        ServletContext servletContext = FacesUtils.getServletContext();
        environment.initPropertySources(servletContext, null);

        WebApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        new PropertySourcesLoader().loadEnvironment(environment, appContext);

        propertyResolver = environment;
        properties = new InMemoryProperties(environment);

        String newWorkingDir = propertyResolver.getProperty(PreferencesManager.WEBSTUDIO_WORKING_DIR_KEY);
        if (!workingDir.equals(newWorkingDir)) {
            log.warn("Expected working dir {} but WebStudio sees it as {}", workingDir, newWorkingDir);
            throw new IllegalStateException("WebStudio sees working dir as " + newWorkingDir);
        }
    }

    private String getAppName() {
        return PropertySourcesLoader
                .getAppName(WebApplicationContextUtils.getRequiredWebApplicationContext(FacesUtils.getServletContext()));
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

    public String getLdapFilter() {
        return ldapFilter;
    }

    public void setLdapFilter(String ldapFilter) {
        this.ldapFilter = ldapFilter;
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
        return !Boolean
                .parseBoolean(properties.getProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO));
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        properties.setProperty(DesignTimeRepositoryImpl.USE_SEPARATE_DEPLOY_CONFIG_REPO, !useDesignRepo);
    }

    public FolderStructureSettings getDesignFolderStructure() {
        return new FolderStructureSettings(ConfigNames.DESIGN_CONFIG, properties);
    }

    public FolderStructureSettings getDeployConfigFolderStructure() {
        return new FolderStructureSettings(ConfigNames.DEPLOY_CONFIG, properties);
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

    @PreDestroy
    public void destroy() {
        destroyRepositoryObjects();
        destroyDbContext();
    }

    private void migrateDatabase() {
        try (XmlWebApplicationContext ctx = new XmlWebApplicationContext()) {
            ctx.setServletContext(FacesUtils.getServletContext());
            ctx.setConfigLocations("classpath:META-INF/standalone/spring/security-hibernate-beans.xml");
            ctx.addBeanFactoryPostProcessor(beanFactory -> beanFactory.registerSingleton("propertyLoader",
                    new DelegatedPropertySourceLoader(properties)));
            ctx.refresh();
            ctx.getBean("dbMigration");
        }
    }

    private void initProductionRepositoryEditor() {
        destroyRepositoryObjects();

        productionRepositoryFactoryProxy = new ProductionRepositoryFactoryProxy(propertyResolver);
        productionRepositoryEditor = new ProductionRepositoryEditor(productionRepositoryFactoryProxy, properties);

        connectionProductionRepoController = new ConnectionProductionRepoController();
        connectionProductionRepoController.setProperties(properties);
        connectionProductionRepoController.setProductionRepositoryFactoryProxy(productionRepositoryFactoryProxy);
        connectionProductionRepoController
                .setProductionRepositoryConfigurations(getProductionRepositoryConfigurations());
        connectionProductionRepoController.clearForm();
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

    private void destroyDbContext() {
        if (dbContext != null) {
            if (groupManagementService instanceof GroupManagementServiceWrapper) {
                ((GroupManagementServiceWrapper) groupManagementService).setDelegate(null);
            }
            dbContext.close();
            dbContext = null;
        }
    }

    public void setGroupManagementService(GroupManagementService groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        properties = new InMemoryProperties(propertyResolver);
    }
}
