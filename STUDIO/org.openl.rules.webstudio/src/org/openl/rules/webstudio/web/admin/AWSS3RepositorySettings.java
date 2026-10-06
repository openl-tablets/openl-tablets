package org.openl.rules.webstudio.web.admin;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.util.StringUtils;

@Schema(allOf = RepositorySettings.class)
public class AWSS3RepositorySettings extends RepositorySettings {

    private static final String SERVICE_ENDPOINT_PATH_SUFFIX = ".service-endpoint";
    private static final String BUCKET_NAME_PATH_SUFFIX = ".bucket-name";
    private static final String REGION_NAME_PATH_SUFFIX = ".region-name";
    private static final String ACCESS_KEY_PATH_SUFFIX = ".access-key";
    private static final String SECRET_KEY_PATH_SUFFIX = ".secret-key";
    private static final String SSE_ALGORITHM_PATH_SUFFIX = ".sse-algorithm";
    private static final String SSE_KMS_KEY_ID_PATH_SUFFIX = ".sse-kms-key-id";
    private static final String LISTENER_TIMER_PERIOD_PATH_SUFFIX = ".listener-timer-period";

    @Getter
    @Parameter(description = "This field should be left blank to use the standard AWS S3. To use a non-standard service endpoint, enter the URL here.")
    @Setter
    @SettingPropertyName(suffix = SERVICE_ENDPOINT_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private String serviceEndpoint;

    @Getter
    @Parameter(description = "A bucket is a logical unit of object storage in the AWS object storage service. Bucket names are globally unique, regardless of the AWS region where the bucket is created.")
    @Setter
    @SettingPropertyName(suffix = BUCKET_NAME_PATH_SUFFIX)
    @NotBlank
    @JsonView(Views.Base.class)
    private String bucketName;

    @Getter
    @Parameter(description = "Select a geographically closest AWS region to optimize latency, minimize costs, and address regulatory requirements.")
    @Setter
    @SettingPropertyName(suffix = REGION_NAME_PATH_SUFFIX)
    @NotBlank
    @JsonView(Views.Base.class)
    private String regionName;

    @Getter
    @Parameter(description = "Alphanumeric text string that identifies the account owner.")
    @Setter
    @SettingPropertyName(suffix = ACCESS_KEY_PATH_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String accessKey;

    @Getter
    @Parameter(description = "Plays the role of a password.")
    @Setter
    @SettingPropertyName(suffix = SECRET_KEY_PATH_SUFFIX, secret = true)
    @JsonView(Views.Base.class)
    private String secretKey;

    @Getter
    @Parameter(description = "You can select server side encryption algorithm to encrypt data in S3 bucket.")
    @Setter
    @SettingPropertyName(suffix = SSE_ALGORITHM_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private ServerSideEncryption sseAlgorithm;

    @Getter
    @Parameter(description = "The ID, ARN or alias of the AWS KMS key for the 'aws:kms' and 'aws:kms:dsse' algorithms. If it is left blank, the AWS managed key 'aws/s3' is used.")
    @Setter
    @SettingPropertyName(suffix = SSE_KMS_KEY_ID_PATH_SUFFIX)
    @JsonView(Views.Base.class)
    private String sseKmsKeyId;

    @Getter
    @Parameter(description = "Repository changes check interval. Must be greater than 0.")
    @Setter
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
    private final String sseKmsKeyIdPath;
    private final String listenerTimerPeriodPath;

    AWSS3RepositorySettings(PropertiesHolder properties, String configPrefix, RepositoryMode repositoryMode) {
        super(properties, configPrefix, repositoryMode);
        serviceEndpointPath = configPrefix + SERVICE_ENDPOINT_PATH_SUFFIX;
        bucketNamePath = configPrefix + BUCKET_NAME_PATH_SUFFIX;
        regionNamePath = configPrefix + REGION_NAME_PATH_SUFFIX;
        accessKeyPath = configPrefix + ACCESS_KEY_PATH_SUFFIX;
        secretKeyPath = configPrefix + SECRET_KEY_PATH_SUFFIX;
        sseAlgorithmPath = configPrefix + SSE_ALGORITHM_PATH_SUFFIX;
        sseKmsKeyIdPath = configPrefix + SSE_KMS_KEY_ID_PATH_SUFFIX;
        listenerTimerPeriodPath = configPrefix + LISTENER_TIMER_PERIOD_PATH_SUFFIX;

        loadProperties(properties);
    }

    private void loadProperties(PropertiesHolder properties) {
        serviceEndpoint = properties.getProperty(serviceEndpointPath);
        bucketName = properties.getProperty(bucketNamePath);
        regionName = properties.getProperty(regionNamePath);
        accessKey = properties.getProperty(accessKeyPath);
        secretKey = properties.getProperty(secretKeyPath);
        sseAlgorithm = ServerSideEncryption.fromValue(StringUtils.trim(properties.getProperty(sseAlgorithmPath)));
        sseKmsKeyId = properties.getProperty(sseKmsKeyIdPath);
        listenerTimerPeriod = Optional.ofNullable(properties.getProperty(listenerTimerPeriodPath))
                .map(Integer::parseInt)
                .orElse(null);
    }

    public List<AWSS3Region> getAllAllowedRegions() {
        return Region.regions().stream()
                .map(AWSS3Region::from)
                .toList();
    }

    public Set<ServerSideEncryption> getAllSseAlgorithms() {
        return ServerSideEncryption.knownValues();
    }

    @Override
    protected void store(PropertiesHolder propertiesHolder) {
        super.store(propertiesHolder);

        propertiesHolder.setProperty(serviceEndpointPath, serviceEndpoint);
        propertiesHolder.setProperty(bucketNamePath, bucketName);
        propertiesHolder.setProperty(regionNamePath, regionName);
        propertiesHolder.setProperty(accessKeyPath, accessKey);
        propertiesHolder.setProperty(secretKeyPath, secretKey);
        // The SDK reports 'no algorithm' as an unknown one, whose text is "null". An empty value, unlike a removed
        // one, replaces the stored value when unsaved settings are validated.
        var noSseAlgorithm = sseAlgorithm == null || sseAlgorithm == ServerSideEncryption.UNKNOWN_TO_SDK_VERSION;
        propertiesHolder.setProperty(sseAlgorithmPath, noSseAlgorithm ? "" : sseAlgorithm);
        propertiesHolder.setProperty(sseKmsKeyIdPath, StringUtils.trimToEmpty(sseKmsKeyId));
        propertiesHolder.setProperty(listenerTimerPeriodPath, listenerTimerPeriod);
    }

    @Override
    protected void revert(PropertiesHolder properties) {
        super.revert(properties);

        properties.revertProperties(
                serviceEndpointPath,
                bucketNamePath,
                regionNamePath,
                accessKeyPath,
                secretKeyPath,
                sseAlgorithmPath,
                sseKmsKeyIdPath,
                listenerTimerPeriodPath
        );
        loadProperties(properties);
    }
}
