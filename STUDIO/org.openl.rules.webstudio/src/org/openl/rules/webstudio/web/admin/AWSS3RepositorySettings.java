package org.openl.rules.webstudio.web.admin;

import java.util.Optional;

import org.openl.config.PropertiesHolder;

public class AWSS3RepositorySettings extends RepositorySettings {
    private String bucketName;
    private String regionName;
    private String accessKey;
    private String secretKey;
    private int listenerTimerPeriod;

    private final String bucketNamePath;
    private final String regionNamePath;
    private final String accessKeyPath;
    private final String secretKeyPath;
    private final String listenerTimerPeriodPath;

    AWSS3RepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        bucketNamePath = configPrefix + ".bucket-name";
        regionNamePath = configPrefix + ".region-name";
        accessKeyPath = configPrefix + ".access-key";
        secretKeyPath = configPrefix + ".secret-key";
        listenerTimerPeriodPath = configPrefix + ".listener-timer-period";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        bucketName = properties.getProperty(bucketNamePath);
        regionName = properties.getProperty(regionNamePath);
        accessKey = properties.getProperty(accessKeyPath);
        secretKey = properties.getProperty(secretKeyPath);
        listenerTimerPeriod = Integer.parseInt(
                Optional.ofNullable(properties.getProperty(listenerTimerPeriodPath))
                        .orElse(properties.getProperty("repo-aws-s3.listener-timer-period"))
        );
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

    public int getListenerTimerPeriod() {
        // Convert to seconds
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        // Convert to milliseconds
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);

        propertiesHolder.setProperty(bucketNamePath, bucketName);
        propertiesHolder.setProperty(regionNamePath, regionName);
        propertiesHolder.setProperty(accessKeyPath, accessKey);
        propertiesHolder.setProperty(secretKeyPath, secretKey);
        propertiesHolder.setProperty(listenerTimerPeriodPath, listenerTimerPeriod);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(
                bucketNamePath,
                regionNamePath,
                accessKeyPath,
                secretKeyPath,
                listenerTimerPeriodPath
        );
        load(properties);
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
