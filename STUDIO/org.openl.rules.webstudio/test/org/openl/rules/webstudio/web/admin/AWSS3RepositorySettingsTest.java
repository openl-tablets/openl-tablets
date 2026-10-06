package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import org.openl.config.InMemoryProperties;

/**
 * The settings of an AWS S3 repository, as OpenL Studio reads, validates, stores and reverts them.
 *
 * @author Yury Molchan
 */
class AWSS3RepositorySettingsTest {

    private static final String ALGORITHM = "repository.s3.sse-algorithm";
    private static final String KMS_KEY = "repository.s3.sse-kms-key-id";
    private static final String SERVICE_ENDPOINT = "repository.s3.service-endpoint";

    private static InMemoryProperties properties(Map<String, Object> stored) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("stored", new HashMap<>(stored)));
        return new InMemoryProperties(environment);
    }

    private static AWSS3RepositorySettings settings(InMemoryProperties properties) {
        return new AWSS3RepositorySettings(properties, "repository.s3", null);
    }

    @Test
    void readsTheAlgorithmAndTheKmsKey() {
        // The repository ignores the blanks around the algorithm, so the settings show the algorithm it uses.
        var settings = settings(properties(Map.of(ALGORITHM, " aws:kms:dsse ", KMS_KEY, "alias/openl")));

        assertSame(ServerSideEncryption.AWS_KMS_DSSE, settings.getSseAlgorithm());
        assertEquals("alias/openl", settings.getSseKmsKeyId());
    }

    /**
     * Older versions of OpenL Studio saved the text {@code null} when no algorithm was chosen.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "null"})
    void readsNoAlgorithmAsNone(String algorithm) {
        var settings = settings(properties(Map.of(ALGORITHM, algorithm)));

        assertSame(ServerSideEncryption.UNKNOWN_TO_SDK_VERSION, settings.getSseAlgorithm());
    }

    @Test
    void storesTheAlgorithmAsTheValueS3Accepts() {
        var target = properties(Map.of());
        var settings = settings(target);

        settings.setSseAlgorithm(ServerSideEncryption.AWS_KMS_DSSE);
        settings.setSseKmsKeyId("arn:aws:kms:us-east-1:111122223333:key/1234abcd");
        settings.store(target);

        assertEquals("aws:kms:dsse", target.getProperty(ALGORITHM));
        assertEquals("arn:aws:kms:us-east-1:111122223333:key/1234abcd", target.getProperty(KMS_KEY));
    }

    @Test
    void clearedEncryptionReplacesTheStoredValuesWhenUnsavedSettingsAreValidated() {
        var target = properties(Map.of(ALGORITHM, "aws:kms", KMS_KEY, "alias/openl"));
        var settings = settings(target);

        settings.setSseAlgorithm(ServerSideEncryption.UNKNOWN_TO_SDK_VERSION);
        settings.setSseKmsKeyId(null);
        settings.store(target);

        assertEquals("", target.getProperty(ALGORITHM));
        assertEquals("", target.getProperty(KMS_KEY));
    }

    @Test
    void revertsTheKmsKeyWithTheOtherSettings() {
        var target = properties(Map.of(ALGORITHM, "aws:kms", KMS_KEY, "alias/openl"));
        var settings = settings(target);
        settings.setSseAlgorithm(ServerSideEncryption.AES256);
        settings.setSseKmsKeyId("alias/changed");
        settings.store(target);

        settings.revert(target);

        assertSame(ServerSideEncryption.AWS_KMS, settings.getSseAlgorithm());
        assertEquals("alias/openl", settings.getSseKmsKeyId());
        assertNull(target.getConfig().get(KMS_KEY));
    }

    @Test
    void revertsTheServiceEndpoint() {
        var target = properties(Map.of(SERVICE_ENDPOINT, "http://minio:9000"));
        var settings = settings(target);
        settings.setServiceEndpoint("http://changed:9000");
        settings.store(target);

        settings.revert(target);

        assertEquals("http://minio:9000", settings.getServiceEndpoint());
        assertTrue(target.getConfig().containsKey(SERVICE_ENDPOINT));
        assertNull(target.getConfig().get(SERVICE_ENDPOINT));
    }
}
