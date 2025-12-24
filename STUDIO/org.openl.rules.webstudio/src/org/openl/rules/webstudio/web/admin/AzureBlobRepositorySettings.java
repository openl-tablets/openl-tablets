package org.openl.rules.webstudio.web.admin;

import java.util.Optional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.studio.settings.converter.SettingPropertyName;

@Schema(allOf = RepositorySettings.class)
public class AzureBlobRepositorySettings extends RepositorySettings {

    private static final String URI_PROPERTY_SUFFIX = ".uri";
    private static final String ACCOUNT_NAME_PROPERTY_SUFFIX = ".account-name";
    private static final String ACCOUNT_KEY_PROPERTY_SUFFIX = ".account-key";
    private static final String LISTENER_TIMER_PERIOD_PROPERTY_SUFFIX = ".listener-timer-period";

    @Parameter(description = "URL")
    @SettingPropertyName(suffix = URI_PROPERTY_SUFFIX)
    @NotBlank
    @JsonView(Views.Base.class)
    private String uri;

    @Parameter(description = "Account name")
    @SettingPropertyName(suffix = ACCOUNT_NAME_PROPERTY_SUFFIX)
    @JsonView(Views.Base.class)
    private String accountName;

    @Parameter(description = "Account key")
    @SettingPropertyName(suffix = ACCOUNT_KEY_PROPERTY_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String accountKey;

    @Parameter(description = "Repository changes check interval. Must be greater than 0.")
    @SettingPropertyName(suffix = LISTENER_TIMER_PERIOD_PROPERTY_SUFFIX)
    @JsonView(Views.Base.class)
    @Min(1)
    @NotNull
    private Integer listenerTimerPeriod;

    private final String uriProperty;
    private final String accountNameProperty;
    private final String accountKeyProperty;
    private final String listenerTimerPeriodProperty;

    AzureBlobRepositorySettings(PropertiesHolder properties, String configPrefix, RepositoryMode repositoryMode) {
        super(properties, configPrefix, repositoryMode);
        uriProperty = configPrefix + URI_PROPERTY_SUFFIX;
        accountNameProperty = configPrefix + ACCOUNT_NAME_PROPERTY_SUFFIX;
        accountKeyProperty = configPrefix + ACCOUNT_KEY_PROPERTY_SUFFIX;
        listenerTimerPeriodProperty = configPrefix + LISTENER_TIMER_PERIOD_PROPERTY_SUFFIX;

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriProperty);
        accountName = properties.getProperty(accountNameProperty);
        accountKey = properties.getProperty(accountKeyProperty);
        listenerTimerPeriod = Optional.ofNullable(properties.getProperty(listenerTimerPeriodProperty))
                .map(Integer::parseInt)
                .orElse(null);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public Integer getListenerTimerPeriod() {
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(Integer listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);

        propertiesHolder.setProperty(uriProperty, uri);
        propertiesHolder.setProperty(accountNameProperty, accountName);
        propertiesHolder.setProperty(accountKeyProperty, accountKey);
        propertiesHolder.setProperty(listenerTimerPeriodProperty, listenerTimerPeriod);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriProperty, accountNameProperty, accountKeyProperty, listenerTimerPeriodProperty);
        load(properties);
    }
}
