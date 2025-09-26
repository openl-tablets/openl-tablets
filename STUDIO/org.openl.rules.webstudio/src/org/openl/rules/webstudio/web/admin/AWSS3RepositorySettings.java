package org.openl.rules.webstudio.web.admin;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;

@Schema(allOf = RepositorySettings.class)
public class AWSS3RepositorySettings extends RepositorySettings {

    private static final String SERVICE_ENDPOINT_PATH_SUFFIX = ".service-endpoint";
    private static final String BUCKET_NAME_PATH_SUFFIX = ".bucket-name";
    private static final String REGION_NAME_PATH_SUFFIX = ".region-name";
    private static final String ACCESS_KEY_PATH_SUFFIX = ".access-key";
    private static final String SECRET_KEY_PATH_SUFFIX = ".secret-key";
    private static final String SSE_ALGORITHM_PATH_SUFFIX = ".sse-algorithm";
    private static final String LISTENER_TIMER_PERIOD_PATH_SUFFIX = ".listener-timer-period";

    @Parameter(description = "This field should be left blank to use the standard AWS S3. To use a non-standard service endpoint, enter the URL here.")
    @SettingPropertyName(suffix = SERVICE_ENDPOINT_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private String serviceEndpoint;

    @Parameter(description = "A bucket is a logical unit of object storage in the AWS object storage service. Bucket names are globally unique, regardless of the AWS region where the bucket is created.")
    @SettingPropertyName(suffix = BUCKET_NAME_PATH_SUFFIX)
    @NotBlank
    @JsonView(Views.Base.class)
    private String bucketName;

    @Parameter(description = "Select a geographically closest AWS region to optimize latency, minimize costs, and address regulatory requirements.")
    @SettingPropertyName(suffix = REGION_NAME_PATH_SUFFIX)
    @NotBlank
    @JsonView(Views.Base.class)
    private String regionName;

    @Parameter(description = "Alphanumeric text string that identifies the account owner.")
    @SettingPropertyName(suffix = ACCESS_KEY_PATH_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String accessKey;

    @Parameter(description = "Plays the role of a password.")
    @SettingPropertyName(suffix = SECRET_KEY_PATH_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String secretKey;

    @Parameter(description = "You can select server side encryption algorithm to encrypt data in S3 bucket.")
    @SettingPropertyName(suffix = SSE_ALGORITHM_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private ServerSideEncryption sseAlgorithm;

    @Parameter(description = "Repository changes check interval. Must be greater than 0.")
    @SettingPropertyName(suffix = LISTENER_TIMER_PERIOD_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    @Min(1)
    @NotNull
    private Integer listenerTimerPeriod;

    private final String serviceEndpointPath;
    private final String bucketNamePath;
    private final String regionNamePath;
    private final String accessKeyPath;
    private final String secretKeyPath;
    private final String sseAlgorithmPath;
    private final String listenerTimerPeriodPath;

    AWSS3RepositorySettings(PropertiesHolder properties, String configPrefix, RepositoryMode repositoryMode) {
        super(properties, configPrefix, repositoryMode);
        serviceEndpointPath = configPrefix + SERVICE_ENDPOINT_PATH_SUFFIX;
        bucketNamePath = configPrefix + BUCKET_NAME_PATH_SUFFIX;
        regionNamePath = configPrefix + REGION_NAME_PATH_SUFFIX;
        accessKeyPath = configPrefix + ACCESS_KEY_PATH_SUFFIX;
        secretKeyPath = configPrefix + SECRET_KEY_PATH_SUFFIX;
        sseAlgorithmPath = configPrefix + SSE_ALGORITHM_PATH_SUFFIX;
        listenerTimerPeriodPath = configPrefix + LISTENER_TIMER_PERIOD_PATH_SUFFIX;

        load(properties);
    }

    private void load(PropertiesHolder properties) {
        serviceEndpoint = properties.getProperty(serviceEndpointPath);
        bucketName = properties.getProperty(bucketNamePath);
        regionName = properties.getProperty(regionNamePath);
        accessKey = properties.getProperty(accessKeyPath);
        secretKey = properties.getProperty(secretKeyPath);
        sseAlgorithm = ServerSideEncryption.fromValue(properties.getProperty(sseAlgorithmPath));
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

    @JsonIgnore
    public List<Region> getAllRegions() {
        return Region.regions();
    }

    public List<AWSS3Region> getAllAllowedRegions() {
        return Region.regions().stream()
                .map(AWSS3Region::from)
                .toList();
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

    public ServerSideEncryption getSseAlgorithm() {
        return sseAlgorithm;
    }

    public void setSseAlgorithm(ServerSideEncryption sseAlgorithm) {
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
