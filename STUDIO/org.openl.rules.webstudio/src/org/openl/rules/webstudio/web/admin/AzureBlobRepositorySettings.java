package org.openl.rules.webstudio.web.admin;

import java.util.Optional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.studio.settings.converter.SettingPropertyName;

@Schema(allOf = RepositorySettings.class)
public class AzureBlobRepositorySettings extends RepositorySettings {

    private static final String URI_PROPERTY_SUFFIX = ".uri";
    private static final String ACCOUNT_NAME_PROPERTY_SUFFIX = ".account-name";
    private static final String ACCOUNT_KEY_PROPERTY_SUFFIX = ".account-key";
    private static final String LISTENER_TIMER_PERIOD_PROPERTY_SUFFIX = ".listener-timer-period";

    @Getter
    @Parameter(description = "URL")
    @Setter
    @SettingPropertyName(suffix = URI_PROPERTY_SUFFIX)
    @NotBlank
    @JsonView(Views.Base.class)
    private String uri;

    @Getter
    @Parameter(description = "Account name")
    @Setter
    @SettingPropertyName(suffix = ACCOUNT_NAME_PROPERTY_SUFFIX)
    @JsonView(Views.Base.class)
    private String accountName;

    @Getter
    @Parameter(description = "Account key")
    @Setter
    @SettingPropertyName(suffix = ACCOUNT_KEY_PROPERTY_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String accountKey;

    @Getter
    @Parameter(description = "Repository changes check interval. Must be greater than 0.")
    @Setter
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
