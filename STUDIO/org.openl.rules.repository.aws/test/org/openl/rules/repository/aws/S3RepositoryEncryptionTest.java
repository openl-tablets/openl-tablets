package org.openl.rules.repository.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.repository.aws.S3EncryptionTest.KMS_KEY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;

/**
 * Checks the requests that {@link S3Repository} sends to S3, as they appear on the wire.
 *
 * @author Yury Molchan
 */
class S3RepositoryEncryptionTest {

    private static final String ALGORITHM_HEADER = "x-amz-server-side-encryption";
    private static final String KMS_KEY_HEADER = "x-amz-server-side-encryption-aws-kms-key-id";
    private static final String OBJECT_PATH = "/" + FakeS3Server.BUCKET + "/deploy/project/rules.zip";
    private static final String COPY_PATH = "/" + FakeS3Server.BUCKET + "/deploy/project/copy.zip";
    private static final String MARKER_PATH = "/" + FakeS3Server.BUCKET + "/.openl-settings/.modification";

    private FakeS3Server s3;

    @BeforeEach
    void startServer() throws IOException {
        s3 = new FakeS3Server();
    }

    @AfterEach
    void stopServer() {
        s3.close();
    }

    @ParameterizedTest
    @MethodSource("org.openl.rules.repository.aws.S3EncryptionTest#algorithms")
    void everyAlgorithmEncryptsTheSavedObjectAndTheModificationMarker(String algorithm) throws Exception {
        try (var repository = open(algorithm, null)) {
            save(repository);
        }

        var puts = s3.requests("PUT");
        var paths = puts.stream().map(FakeS3Server.Request::path).collect(Collectors.toSet());
        assertEquals(Set.of(OBJECT_PATH, MARKER_PATH), paths);
        for (var put : puts) {
            assertEquals(algorithm, put.header(ALGORITHM_HEADER), put.path());
            assertNull(put.header(KMS_KEY_HEADER), put.path());
            // Older versions stored the algorithm as metadata of the object instead of requesting the encryption.
            assertNull(put.header("x-amz-meta-ssealgorithm"), put.path());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"aws:kms", "aws:kms:dsse"})
    void kmsKeyEncryptsTheSavedObjectAndTheModificationMarker(String algorithm) throws Exception {
        try (var repository = open(algorithm, KMS_KEY)) {
            save(repository);
        }

        var puts = s3.requests("PUT");
        assertEquals(2, puts.size());
        for (var put : puts) {
            assertEquals(algorithm, put.header(ALGORITHM_HEADER), put.path());
            assertEquals(KMS_KEY, put.header(KMS_KEY_HEADER), put.path());
        }
    }

    @Test
    void kmsKeyEncryptsTheCopiedObject() throws Exception {
        try (var repository = open("aws:kms", KMS_KEY)) {
            var destination = new FileData();
            destination.setName("deploy/project/copy.zip");

            repository.copyHistory("deploy/project/rules.zip", destination, "version-1");
        }

        var copy = s3.requests("PUT").stream().filter(put -> put.path().equals(COPY_PATH)).findFirst().orElseThrow();
        assertNotNull(copy.header("x-amz-copy-source"));
        assertEquals("aws:kms", copy.header(ALGORITHM_HEADER));
        assertEquals(KMS_KEY, copy.header(KMS_KEY_HEADER));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "null")
    void withoutAlgorithmNoEncryptionIsRequested(String algorithm) throws Exception {
        try (var repository = open(algorithm, null)) {
            save(repository);
        }

        var puts = s3.requests("PUT");
        assertEquals(2, puts.size());
        for (var put : puts) {
            assertNull(put.header(ALGORITHM_HEADER), put.path());
            assertNull(put.header(KMS_KEY_HEADER), put.path());
        }
    }

    @Test
    void invalidEncryptionFailsBeforeAnythingIsSent() {
        assertThrows(IllegalArgumentException.class, () -> open("AES256", KMS_KEY));

        assertTrue(s3.requests().isEmpty());
    }

    /**
     * Opens the repository the way it is configured, by the names of its settings.
     */
    private Repository open(String algorithm, String kmsKeyId) {
        var settings = new HashMap<String, String>();
        settings.put("service-endpoint", s3.endpoint());
        settings.put("bucket-name", FakeS3Server.BUCKET);
        settings.put("region-name", "us-east-1");
        settings.put("access-key", "access");
        settings.put("secret-key", "secret");
        settings.put("sse-algorithm", algorithm);
        settings.put("sse-kms-key-id", kmsKeyId);
        return new S3RepositoryFactory().create(settings::get);
    }

    private static void save(Repository repository) throws IOException {
        var content = "rules".getBytes(StandardCharsets.UTF_8);
        var data = new FileData();
        data.setName("deploy/project/rules.zip");
        data.setSize(content.length);

        repository.save(data, new ByteArrayInputStream(content));
    }
}
