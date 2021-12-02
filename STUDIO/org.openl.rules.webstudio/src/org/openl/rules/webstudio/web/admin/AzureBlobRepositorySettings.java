package org.openl.rules.webstudio.web.admin;

import java.util.Optional;

import org.openl.config.PropertiesHolder;

public class AzureBlobRepositorySettings extends RepositorySettings {
    private String uri;
    private int listenerTimerPeriod;

    private final String uriProperty;
    private final String listenerTimerPeriodProperty;

    AzureBlobRepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        uriProperty = configPrefix + ".uri";
        listenerTimerPeriodProperty = configPrefix + ".listener-timer-period";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        uri = properties.getProperty(uriProperty);
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
        propertiesHolder.setProperty(listenerTimerPeriodProperty, listenerTimerPeriod);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(uriProperty, listenerTimerPeriodProperty);
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
