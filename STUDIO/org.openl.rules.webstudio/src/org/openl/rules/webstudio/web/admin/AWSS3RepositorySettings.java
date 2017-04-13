package org.openl.rules.webstudio.web.admin;

import org.openl.config.ConfigurationManager;

public class AWSS3RepositorySettings extends RepositorySettings {
    private String bucketName;
    private String regionName;
    private String accessKey;
    private String secretKey;
    private Long listenerTimerPeriod;

    private final String BUCKET_NAME;
    private final String REGION_NAME;
    private final String ACCESS_KEY;
    private final String SECRET_KEY;
    private final String LISTENER_TIMER_PERIOD;

    public AWSS3RepositorySettings(ConfigurationManager configManager, String configPrefix) {
        super(configManager, configPrefix);
        BUCKET_NAME = configPrefix + "bucket-name";
        REGION_NAME = configPrefix + "region-name";
        ACCESS_KEY = configPrefix + "access-key";
        SECRET_KEY = configPrefix + "secret-key";
        LISTENER_TIMER_PERIOD = configPrefix + "listener-timer-period";

        bucketName = configManager.getStringProperty(BUCKET_NAME);
        regionName = configManager.getStringProperty(REGION_NAME);
        accessKey = configManager.getStringProperty(ACCESS_KEY);
        secretKey = configManager.getStringProperty(SECRET_KEY);
        listenerTimerPeriod = configManager.getLongProperty(LISTENER_TIMER_PERIOD, 10000L);
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Long getListenerTimerPeriod() {
        // Convert to seconds
        return listenerTimerPeriod / 1000;
    }

    public void setListenerTimerPeriod(Long listenerTimerPeriod) {
        // Convert to milliseconds
        this.listenerTimerPeriod = listenerTimerPeriod * 1000;
    }

    @Override
    protected void store(ConfigurationManager configurationManager) {
        super.store(configurationManager);

        configurationManager.setProperty(BUCKET_NAME, bucketName);
        configurationManager.setProperty(REGION_NAME, regionName);
        configurationManager.setProperty(ACCESS_KEY, accessKey);
        configurationManager.setProperty(SECRET_KEY, secretKey);
        configurationManager.setProperty(LISTENER_TIMER_PERIOD, listenerTimerPeriod);
    }

    @Override
    public void copyContent(RepositorySettings other) {
        super.copyContent(other);

        if (other instanceof AWSS3RepositorySettings) {
            AWSS3RepositorySettings otherSettings = (AWSS3RepositorySettings) other;
            setBucketName(otherSettings.getBucketName());
            setRegionName(otherSettings.getRegionName());
            setAccessKey(otherSettings.getAccessKey());
            setSecretKey(otherSettings.getSecretKey());
            setListenerTimerPeriod(otherSettings.getListenerTimerPeriod());
        }
    }
}
