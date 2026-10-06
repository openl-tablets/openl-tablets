package org.openl.rules.repository.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/**
 * How the encryption settings of an AWS S3 repository are read and checked.
 *
 * @author Yury Molchan
 */
class S3EncryptionTest {

    static final String KMS_KEY = "arn:aws:kms:us-east-1:111122223333:key/1234abcd-12ab-34cd-56ef-1234567890ab";

    /**
     * Every algorithm the AWS SDK knows, so a newly added one is covered without changing the tests.
     */
    static Stream<String> algorithms() {
        return ServerSideEncryption.knownValues().stream().map(ServerSideEncryption::toString);
    }

    static Stream<String> nonKmsAlgorithms() {
        return algorithms().filter(algorithm -> !algorithm.startsWith("aws:kms"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "null", " null "})
    void noAlgorithmMeansNoEncryption(String algorithm) {
        assertEquals(S3Encryption.NONE, S3Encryption.of(algorithm, null));
    }

    @Test
    void valuesAreTrimmed() {
        assertEquals(new S3Encryption(ServerSideEncryption.AWS_KMS, "alias/openl"),
                S3Encryption.of("  aws:kms ", "  alias/openl "));
        assertEquals(new S3Encryption(ServerSideEncryption.AWS_KMS, null), S3Encryption.of("aws:kms", "  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AES-256", "AWS_KMS", "NULL"})
    void unsupportedAlgorithmIsRefused(String algorithm) {
        var e = assertThrows(IllegalArgumentException.class, () -> S3Encryption.of(algorithm, null));

        var supported = algorithms().map(name -> "'" + name + "'").sorted().collect(Collectors.joining(", "));
        assertEquals("Unsupported server-side encryption algorithm '" + algorithm + "'. Supported algorithms: "
                + supported + ".", e.getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonKmsAlgorithms")
    void kmsKeyIsRefusedForAlgorithmWithoutKms(String algorithm) {
        var e = assertThrows(IllegalArgumentException.class, () -> S3Encryption.of(algorithm, KMS_KEY));

        assertEquals("The KMS key is set, but the server-side encryption algorithm is '" + algorithm
                + "'. A KMS key requires 'aws:kms' or 'aws:kms:dsse'.", e.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "null")
    void kmsKeyIsRefusedWithoutAlgorithm(String algorithm) {
        var e = assertThrows(IllegalArgumentException.class, () -> S3Encryption.of(algorithm, KMS_KEY));

        assertEquals("The KMS key is set, but the server-side encryption algorithm is not set. "
                + "A KMS key requires 'aws:kms' or 'aws:kms:dsse'.", e.getMessage());
    }
}
