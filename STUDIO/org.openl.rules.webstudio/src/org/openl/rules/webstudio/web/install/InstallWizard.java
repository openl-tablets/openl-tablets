package org.openl.rules.webstudio.web.install;

import static org.openl.rules.webstudio.web.admin.AdministrationSettings.DESIGN_REPOSITORY_CONFIGS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.validator.ValidatorException;
import javax.naming.directory.InvalidSearchFilterException;

import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.util.WebStudioValidationUtils;
import org.openl.rules.webstudio.web.admin.ConnectionProductionRepoController;
import org.openl.rules.webstudio.web.admin.FolderStructureSettings;
import org.openl.rules.webstudio.web.admin.GroupManagementSettings;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositoryValidators;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;

@Service
@SessionScope
public class InstallWizard implements Serializable {

    private static final String SINGLE_USER_MODE = "single";
    private static final String AD_USER_MODE = "ad";
    private static final String CAS_USER_MODE = "cas";
    private static final String SAML_USER_MODE = "saml";
    private static final String OAUTH2_USER_MODE = "oauth2";
    private static final String USER_MODE_DEMO = "demo";
    private static final String VIEWERS_GROUP = "Authenticated";

    private final Logger log = LoggerFactory.getLogger(InstallWizard.class);

    private int step;

    private static final String PAGE_PREFIX = "step";
    private static final String PAGE_POSTFIX = "?faces-redirect=true";

    private boolean showErrorMessage = false;

    private String userMode = "demo";

    private String dbUrl;
    private String dbUsername;
    private String dbPassword;

    private String adDomain;
    private String adUrl;
    private String adUsername;
    private String adPassword;
    private String ldapFilter;
    private String groupFilter;

    private CASSettings casSettings;
    private SAMLSettings samlSettings;
    private Oauth2Settings oauth2Settings;

    private RepositoryConfiguration designRepositoryConfiguration;
    private RepositoryConfiguration deployConfigRepositoryConfiguration;

    private RepositoryEditor productionRepositoryEditor;
    private RepositoryFactoryProxy productionRepositoryFactoryProxy;

    // Reuse existing controllers
    private ConnectionProductionRepoController connectionProductionRepoController;

    private String defaultGroup;
    private String externalAdmins;

    private final PropertyResolver propertyResolver;
    private final PropertiesHolder properties;

    public InstallWizard(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        this.properties = new InMemoryProperties(propertyResolver);
    }

    private static FacesMessage createErrorMessage(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    public String start() {
        step = 1;
        return next();
    }

    public String reconfigure() throws IOException {
        HashMap<String, String> props = new HashMap<>();
        props.put("webstudio.configured", "false");
        DynamicPropertySource.get().save(props);
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
                    RepositoryValidators.validateConnection(designRepositoryConfiguration);

                    if (!isUseDesignRepo() && !getProductionRepositoryConfigurations().isEmpty()) {
                        RepositoryValidators.validate(deployConfigRepositoryConfiguration);
                        RepositoryValidators.validateConnection(deployConfigRepositoryConfiguration);
                    }

                    productionRepositoryEditor.validate();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    WebStudioUtils.addErrorMessage(e.getMessage());
                    return null;
                }
            }

            // Go to next step
            ++step;
            if (step == 2) {
                // Get defaults
                String designRepoId = Objects.requireNonNull(propertyResolver.getProperty(DESIGN_REPOSITORY_CONFIGS)).split("\\s*,\\s*")[0];
                designRepositoryConfiguration = new RepositoryConfiguration(designRepoId, properties);
                if (designRepositoryConfiguration.getErrorMessage() != null) {
                    log.error(designRepositoryConfiguration.getErrorMessage());
                }

                deployConfigRepositoryConfiguration = new RepositoryConfiguration("deploy-config",
                        properties);
                if (deployConfigRepositoryConfiguration.getErrorMessage() != null) {
                    log.error(deployConfigRepositoryConfiguration.getErrorMessage());
                }

                initProductionRepositoryEditor();
                userMode = propertyResolver.getProperty("user.mode");
            } else if (step == 3) {
                readDbProperties();
                readAdProperties();
                readCasProperties();
                readSamlProperties();
                readOauth2Properties();

                defaultGroup = propertyResolver.getProperty(GroupManagementSettings.SECURITY_DEF_GROUP_PROP);
                externalAdmins = propertyResolver.getProperty("security.administrators");

            }
            return PAGE_PREFIX + step + PAGE_POSTFIX;
        } catch (Exception e) {
            log.error("Failed while saving the configuration", e);
            if (e.getCause() instanceof FlywayException) {
                WebStudioUtils.addErrorMessage("Cannot migrate the database. Check the logs for details.");
            } else {
                WebStudioUtils.addErrorMessage("Cannot save the configuration. Check the logs for details.");
            }
            step--;
            return null;
        }
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
        groupFilter = propertyResolver.getProperty("security.ad.group-filter");
    }

    private void readCasProperties() {
        casSettings = new CASSettings(propertyResolver.getProperty("security.cas.app-url"),
                propertyResolver.getProperty("security.cas.cas-server-url-prefix"),
                propertyResolver.getProperty("security.cas.attribute.first-name"),
                propertyResolver.getProperty("security.cas.attribute.last-name"),
                propertyResolver.getProperty("security.cas.attribute.display-name"),
                propertyResolver.getProperty("security.cas.attribute.email"),
                propertyResolver.getProperty("security.cas.attribute.groups"));
    }

    private void readSamlProperties() {
        samlSettings = new SAMLSettings(propertyResolver.getProperty("security.saml.entity-id"),
                propertyResolver.getProperty("security.saml.saml-server-metadata-url"),
                propertyResolver.getProperty("security.saml.attribute.username"),
                propertyResolver.getProperty("security.saml.attribute.first-name"),
                propertyResolver.getProperty("security.saml.attribute.last-name"),
                propertyResolver.getProperty("security.saml.attribute.display-name"),
                propertyResolver.getProperty("security.saml.attribute.email"),
                propertyResolver.getProperty("security.saml.attribute.groups"),
                propertyResolver.getProperty("security.saml.server-certificate"));
    }

    private void readOauth2Properties() {
        oauth2Settings = new Oauth2Settings();
        oauth2Settings.setClientId(propertyResolver.getProperty("security.oauth2.client-id"));
        oauth2Settings.setClientSecret(propertyResolver.getProperty("security.oauth2.client-secret"));
        oauth2Settings.setIssuerUri(propertyResolver.getProperty("security.oauth2.issuer-uri"));
        oauth2Settings.setScope(propertyResolver.getProperty("security.oauth2.scope"));
        oauth2Settings.setUsernameAttribute(propertyResolver.getProperty("security.oauth2.attribute.username"));
        oauth2Settings.setFirstNameAttribute(propertyResolver.getProperty("security.oauth2.attribute.first-name"));
        oauth2Settings.setSecondNameAttribute(propertyResolver.getProperty("security.oauth2.attribute.last-name"));
        oauth2Settings.setDisplayNameAttribute(propertyResolver.getProperty("security.oauth2.attribute.display-name"));
        oauth2Settings.setEmailAttribute(propertyResolver.getProperty("security.oauth2.attribute.email"));
        oauth2Settings.setGroupsAttribute(propertyResolver.getProperty("security.oauth2.attribute.groups"));
    }

    public String finish() {
        try {
            if (!USER_MODE_DEMO.equals(userMode) && !SINGLE_USER_MODE.equals(userMode)) {
                // Single and Demo user mode are special cases, which do not require configuration to an external DB.
                properties.setProperty("db.url", dbUrl);
                properties.setProperty("db.user", dbUsername);
                properties.setProperty("db.password", dbPassword);
            }
            if (AD_USER_MODE.equals(userMode)) {

                properties.setProperty("security.ad.domain", adDomain);
                properties.setProperty("security.ad.server-url", adUrl);
                properties.setProperty("security.ad.search-filter", ldapFilter);
                properties.setProperty("security.ad.group-filter", groupFilter);
            } else if (CAS_USER_MODE.equals(userMode)) {
                properties.setProperty("security.cas.app-url", casSettings.getWebStudioUrl());
                properties.setProperty("security.cas.cas-server-url-prefix", casSettings.getCasServerUrl());
                properties.setProperty("security.cas.attribute.first-name", casSettings.getFirstNameAttribute());
                properties.setProperty("security.cas.attribute.display-name", casSettings.getDisplayNameAttribute());
                properties.setProperty("security.cas.attribute.email", casSettings.getEmailAttribute());
                properties.setProperty("security.cas.attribute.last-name", casSettings.getSecondNameAttribute());
                properties.setProperty("security.cas.attribute.groups", casSettings.getGroupsAttribute());
            } else if (SAML_USER_MODE.equals(userMode)) {
                properties.setProperty("security.saml.entity-id", samlSettings.getEntityId());
                properties.setProperty("security.saml.saml-server-metadata-url", samlSettings.getSamlServerMetadataUrl());
                properties.setProperty("security.saml.attribute.username", samlSettings.getUsernameAttribute());
                properties.setProperty("security.saml.attribute.first-name", samlSettings.getFirstNameAttribute());
                properties.setProperty("security.saml.attribute.last-name", samlSettings.getSecondNameAttribute());
                properties.setProperty("security.saml.attribute.display-name", samlSettings.getDisplayNameAttribute());
                properties.setProperty("security.saml.attribute.email", samlSettings.getEmailAttribute());
                properties.setProperty("security.saml.attribute.groups", samlSettings.getGroupsAttribute());
                properties.setProperty("security.saml.server-certificate", samlSettings.getServerCertificate());

                //Generating default keys and certificate.
                if (propertyResolver.getProperty("security.saml.local-key") == null || propertyResolver.getProperty("security.saml.local-certificate") == null) {
                    Pair<String, String> pair = KeyPairCertUtils.generateCertificate();
                    if (pair != null) {
                        properties.setProperty("security.saml.local-key", pair.getKey());
                        properties.setProperty("security.saml.local-certificate", pair.getValue());
                    }
                }
            } else if (OAUTH2_USER_MODE.equals(userMode)) {
                properties.setProperty("security.oauth2.client-id", oauth2Settings.getClientId());
                properties.setProperty("security.oauth2.issuer-uri", oauth2Settings.getIssuerUri());
                properties.setProperty("security.oauth2.client-secret", oauth2Settings.getClientSecret());
                properties.setProperty("security.oauth2.scope", oauth2Settings.getScope());
                properties.setProperty("security.oauth2.attribute.username", oauth2Settings.getUsernameAttribute());
                properties.setProperty("security.oauth2.attribute.first-name", oauth2Settings.getFirstNameAttribute());
                properties.setProperty("security.oauth2.attribute.last-name", oauth2Settings.getSecondNameAttribute());
                properties.setProperty("security.oauth2.attribute.display-name",
                        oauth2Settings.getDisplayNameAttribute());
                properties.setProperty("security.oauth2.attribute.email", oauth2Settings.getEmailAttribute());
                properties.setProperty("security.oauth2.attribute.groups", oauth2Settings.getGroupsAttribute());
            }

            productionRepositoryEditor.save();

            properties.setProperty("user.mode", userMode);
            properties.setProperty(GroupManagementSettings.SECURITY_DEF_GROUP_PROP, defaultGroup);
            properties.setProperty("security.administrators", externalAdmins);


            designRepositoryConfiguration.commit();
            if (!isUseDesignRepo() && !getProductionRepositoryConfigurations().isEmpty()) {
                deployConfigRepositoryConfiguration.commit();
            }
            properties.setProperty("webstudio.configured", true);
            DynamicPropertySource.get().save(properties.getConfig());

            destroyRepositoryObjects();

            FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .redirect(WebStudioUtils.getExternalContext().getRequestContextPath() + "/");

            return "/";
        } catch (Exception e) {
            log.error("Failed while saving the configuration", e);
            if (e.getCause() instanceof FlywayException) {
                WebStudioUtils.addErrorMessage("Cannot migrate the database. Check the logs for details.");
            } else {
                WebStudioUtils.addErrorMessage("Cannot save the configuration. Check the logs for details.");
            }
            return null;
        }
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
                HashMap<String, String> properties = new HashMap<>();
                PropertiesUtils.load(getClass().getResource(SQL_ERRORS_FILE_PATH), properties::put);
                errorMessage = properties.get(Integer.toString(errorCode));
            } catch (Exception e) {
                log.error("Cannot to load {} file.", SQL_ERRORS_FILE_PATH, e);
            }
            if (errorMessage == null) {
                errorMessage = "Incorrect database URL, login or password.";
            }

            throw new ValidatorException(createErrorMessage(errorMessage));
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
                throw new ValidatorException(createErrorMessage("Database URL cannot be blank."));
            } else {
                if (StringUtils.isNotEmpty(dbUsername) && dbUsername.length() > 100) {
                    throw new ValidatorException(createErrorMessage("Username length must be less than 100."));
                }
                testDBConnection(dbUrl, dbUsername, dbPasswordString);
            }
        }

    }

    public void adValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String domain = (String) ((UIInput) viewRoot.findComponent("step3Form:adDomain")).getValue();
        String url = (String) ((UIInput) viewRoot.findComponent("step3Form:adUrl")).getValue();
        String ldapFilter = (String) ((UIInput) viewRoot.findComponent("step3Form:ldapFilter")).getValue();
        String username = (String) ((UIInput) viewRoot.findComponent("step3Form:adUsername")).getValue();
        String password = (String) ((UIInput) toValidate).getSubmittedValue();

        if (StringUtils.isBlank(domain)) {
            throw new ValidatorException(createErrorMessage("Active Directory domain cannot be blank."));
        }
        if (StringUtils.isBlank(url)) {
            throw new ValidatorException(createErrorMessage("Active Directory URL cannot be blank."));
        }
        if (StringUtils.isBlank(ldapFilter)) {
            throw new ValidatorException(createErrorMessage("User filter cannot be blank."));
        }

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
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
                    throw new ValidatorException(createErrorMessage(message));
                }
                throw new ValidatorException(createErrorMessage(e.getMessage()));
            } catch (RuntimeException e) {
                throw new ValidatorException(createErrorMessage(getCauseExceptionMessage(e)));
            }
        }
    }

    public void casValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String webStudioUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:casWebStudioUrl"))
                .getSubmittedValue();
        String serverUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:casServerUrl")).getSubmittedValue();

        if (StringUtils.isBlank(webStudioUrl)) {
            throw new ValidatorException(createErrorMessage("OpenL Studio server URL cannot be blank."));
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new ValidatorException(createErrorMessage("CAS server URL cannot be blank."));
        }
    }

    public void samlValidator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String entityId = (String) ((UIInput) viewRoot.findComponent("step3Form:samlEntityId")).getSubmittedValue();
        String serverUrl = (String) ((UIInput) viewRoot.findComponent("step3Form:samlServerUrl")).getSubmittedValue();
        String publicServerCert = (String) ((UIInput) viewRoot.findComponent("step3Form:samlServerCertificate"))
                .getSubmittedValue();

        if (StringUtils.isBlank(entityId)) {
            throw new ValidatorException(createErrorMessage("Entity ID cannot be blank."));
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new ValidatorException(createErrorMessage("SAML server metadata URL cannot be blank."));
        }

        if (StringUtils.isNotBlank(publicServerCert)) {
            try {
                byte[] decoded = Base64.getMimeDecoder().decode(publicServerCert);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
                cert.checkValidity();
            } catch (Exception e) {
                throw new ValidatorException(createErrorMessage("Entered SAML server certificate is not valid."));
            }
        }
    }

    public void oauth2Validator(FacesContext context, UIComponent toValidate, Object value) {
        UIViewRoot viewRoot = context.getViewRoot();

        String clientID = (String) ((UIInput) viewRoot.findComponent("step3Form:oauth2ClientId")).getSubmittedValue();
        if (StringUtils.isBlank(clientID)) {
            throw new ValidatorException(createErrorMessage("Client ID cannot be blank."));
        }
        String issuerUri = (String) ((UIInput) viewRoot.findComponent("step3Form:oauth2IssuerUri")).getSubmittedValue();
        if (StringUtils.isBlank(issuerUri)) {
            throw new ValidatorException(createErrorMessage("Issuer uri cannot be blank."));
        }
        String clientSecret = (String) ((UIInput) viewRoot.findComponent("step3Form:oauth2ClientSecret")).getSubmittedValue();
        if (StringUtils.isBlank(clientSecret)) {
            throw new ValidatorException(createErrorMessage("Client secret cannot be blank."));
        }
        String scope = (String) ((UIInput) viewRoot.findComponent("step3Form:oauth2Scope")).getSubmittedValue();
        if (StringUtils.isBlank(scope)) {
            throw new ValidatorException(createErrorMessage("Scope cannot be blank."));
        }
    }

    public void externalAdminsValidator(FacesContext context, UIComponent toValidate, Object value) {
        String admins = (String) value;
        if (StringUtils.isBlank(admins) || admins.trim().equals(",")) {
            throw new ValidatorException(createErrorMessage("Administrators field must not be empty."));
        }

        String[] allAdmins = StringUtils.split(admins, ',');
        for (String admin : allAdmins) {
            if (admin.length() > 50) {
                throw new ValidatorException(createErrorMessage("Administrator username length must be less than 50."));
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
     * Validates OpenL Studio working directory for write access. If specified folder is not writable the validation error
     * will appears
     */
    public void workingDirValidator(FacesContext context, UIComponent toValidate, Object value) {
        WebStudioValidationUtils.directoryValidator(value, "OpenL Studio working directory");
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

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void setAllowAccessToNewUsers(Boolean allowAccessToNewUsers) {
        defaultGroup = Boolean.TRUE.equals(allowAccessToNewUsers) ? VIEWERS_GROUP : "";
    }

    public Boolean getAllowAccessToNewUsers() {
        return StringUtils.isNotBlank(defaultGroup);
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

    public String getGroupFilter() {
        return groupFilter;
    }

    public void setGroupFilter(String groupFilter) {
        this.groupFilter = groupFilter;
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

    public Oauth2Settings getOauth2Settings() {
        return oauth2Settings;
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
        return StringUtils.isNotBlank(properties.getProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG));
    }

    public void setUseDesignRepo(boolean useDesignRepo) {
        // TODO: We should point specific design repository
        String designRepoId = Objects.requireNonNull(propertyResolver.getProperty(DESIGN_REPOSITORY_CONFIGS)).split("\\s*,\\s*")[0];
        properties.setProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG, useDesignRepo ? designRepoId : null);
    }

    public FolderStructureSettings getDesignFolderStructure() {
        return new FolderStructureSettings(designRepositoryConfiguration);
    }

    public FolderStructureSettings getDeployConfigFolderStructure() {
        return new FolderStructureSettings(deployConfigRepositoryConfiguration);
    }

    public List<RepositoryConfiguration> getProductionRepositoryConfigurations() {
        return productionRepositoryEditor.getRepositoryConfigurations();
    }

    public void deleteProductionRepository(String configName) {
        try {
            productionRepositoryEditor.deleteRepository(configName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
        }
    }

    public ConnectionProductionRepoController getConnectionProductionRepoController() {
        return connectionProductionRepoController;
    }

    @PreDestroy
    public void destroy() {
        destroyRepositoryObjects();
    }

    private void initProductionRepositoryEditor() {
        destroyRepositoryObjects();

        productionRepositoryFactoryProxy = new RepositoryFactoryProxy(propertyResolver, RepositoryMode.PRODUCTION);
        productionRepositoryEditor = new RepositoryEditor(productionRepositoryFactoryProxy, properties);

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
}
