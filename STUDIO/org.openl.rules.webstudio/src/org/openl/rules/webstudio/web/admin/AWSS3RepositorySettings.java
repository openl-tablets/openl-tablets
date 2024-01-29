package org.openl.rules.webstudio.web.admin;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import org.openl.config.PropertiesHolder;

public class AWSS3RepositorySettings extends RepositorySettings {
    private String serviceEndpoint;
    private String bucketName;
    private String regionName;
    private String accessKey;
    private String secretKey;
    private String sseAlgorithm;
    private Integer listenerTimerPeriod;

    private final String serviceEndpointPath;
    private final String bucketNamePath;
    private final String regionNamePath;
    private final String accessKeyPath;
    private final String secretKeyPath;
    private final String sseAlgorithmPath;
    private final String listenerTimerPeriodPath;

    AWSS3RepositorySettings(PropertiesHolder properties, String configPrefix) {
        super(properties, configPrefix);
        serviceEndpointPath = configPrefix + ".service-endpoint";
        bucketNamePath = configPrefix + ".bucket-name";
        regionNamePath = configPrefix + ".region-name";
        accessKeyPath = configPrefix + ".access-key";
        secretKeyPath = configPrefix + ".secret-key";
        sseAlgorithmPath = configPrefix + ".sse-algorithm";
        listenerTimerPeriodPath = configPrefix + ".listener-timer-period";

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        serviceEndpoint = properties.getProperty(serviceEndpointPath);
        bucketName = properties.getProperty(bucketNamePath);
        regionName = properties.getProperty(regionNamePath);
        accessKey = properties.getProperty(accessKeyPath);
        secretKey = properties.getProperty(secretKeyPath);
        sseAlgorithm = properties.getProperty(sseAlgorithmPath);
        listenerTimerPeriod = Optional.ofNullable(properties.getProperty(listenerTimerPeriodPath))
            .map(Integer::parseInt)
            .orElse(null);
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
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

    public List<Region> getAllRegions() {
        return Region.regions();
    }

    public Set<ServerSideEncryption> getAllSseAlgorithms() {
        return ServerSideEncryption.knownValues();
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

    public Integer getListenerTimerPeriod() {
        return listenerTimerPeriod;
    }

    public void setListenerTimerPeriod(Integer listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    public String getSseAlgorithm() {
        return sseAlgorithm;
    }

    public void setSseAlgorithm(String sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);

        propertiesHolder.setProperty(serviceEndpointPath, serviceEndpoint);
        propertiesHolder.setProperty(bucketNamePath, bucketName);
        propertiesHolder.setProperty(regionNamePath, regionName);
        propertiesHolder.setProperty(accessKeyPath, accessKey);
        propertiesHolder.setProperty(secretKeyPath, secretKey);
        propertiesHolder.setProperty(sseAlgorithmPath, sseAlgorithm);
        propertiesHolder.setProperty(listenerTimerPeriodPath, listenerTimerPeriod);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(
                serviceEndpoint,
                bucketNamePath,
                regionNamePath,
                accessKeyPath,
                secretKeyPath,
                sseAlgorithmPath,
                listenerTimerPeriodPath
        );
        load(properties);
    }
}
