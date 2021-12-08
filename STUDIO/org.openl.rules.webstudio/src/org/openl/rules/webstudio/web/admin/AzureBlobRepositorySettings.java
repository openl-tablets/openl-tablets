package org.openl.rules.webstudio.web.admin;

import java.util.Optional;

import org.openl.config.PropertiesHolder;

public class AzureBlobRepositorySettings extends RepositorySettings {
    private String uri;
    private String accountName;
    private String accountKey;
    private int listenerTimerPeriod;

    private final String uriProperty;
    private final String accountNameProperty;
    private final String accountKeyProperty;
    private final String listenerTimerPeriodProperty;

    AzureBlobRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        uriProperty = configPrefix + ".uri";
        accountNameProperty = configPrefix + ".account-name";
        accountKeyProperty = configPrefix + ".account-key";
        listenerTimerPeriodProperty = configPrefix + ".listener-timer-period";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriProperty);
        accountName = properties.getProperty(accountNameProperty);
        accountKey = properties.getProperty(accountKeyProperty);
        listenerTimerPeriod = Integer.parseInt(
                Optional.ofNullable(properties.getProperty(listenerTimerPeriodProperty))
                        .orElse(properties.getProperty("repo-azure-blob.listener-timer-period"))
        );
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

    public int getListenerTimerPeriod() {
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
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

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);

        if (other instanceof AzureBlobRepositorySettings) {
            AzureBlobRepositorySettings otherSettings = (AzureBlobRepositorySettings) other;
            setUri(otherSettings.getUri());
            setListenerTimerPeriod(otherSettings.getListenerTimerPeriod());
        }
    }
}
