package org.openl.rules.repository.aws;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import org.openl.util.StringUtils;

/**
 * The server-side encryption that is requested for every object the repository writes.
 *
 * <p>The algorithm is one of the values that AWS S3 accepts in the {@code x-amz-server-side-encryption} header, such
 * as {@code AES256}, {@code aws:kms} or {@code aws:kms:dsse}. Without an algorithm, no encryption is requested and
 * the default encryption of the bucket applies.
 *
 * <p>A KMS key can be set only for an algorithm that uses AWS KMS. Without a key, AWS uses its managed key
 * {@code aws/s3}.
 *
 * @param algorithm the encryption algorithm, or {@code null} if no encryption is requested
 * @param kmsKeyId  the ID, ARN or alias of the AWS KMS key, or {@code null} if the default key is used
 * @author Yury Molchan
 */
record S3Encryption(@Nullable ServerSideEncryption algorithm, @Nullable String kmsKeyId) {

    private static final Set<ServerSideEncryption> KMS_ALGORITHMS = EnumSet.of(ServerSideEncryption.AWS_KMS,
            ServerSideEncryption.AWS_KMS_DSSE);

    // Older versions of OpenL Studio saved this text when no algorithm was chosen.
    private static final String LEGACY_NONE = "null";

    /**
     * No encryption is requested.
     */
    static final S3Encryption NONE = new S3Encryption(null, null);

    S3Encryption {
        if (kmsKeyId != null && !KMS_ALGORITHMS.contains(algorithm)) {
            throw new IllegalArgumentException(
                    "The KMS key is set, but the server-side encryption algorithm is %s. A KMS key requires %s."
                            .formatted(algorithm == null ? "not set" : "'" + algorithm + "'",
                                    quote(KMS_ALGORITHMS, " or ")));
        }
    }

    /**
     * Reads the encryption from the repository settings.
     *
     * <p>A blank value means that the setting is not defined.
     *
     * @param algorithm the value of {@code sse-algorithm}
     * @param kmsKeyId  the value of {@code sse-kms-key-id}
     * @throws IllegalArgumentException if the algorithm is not supported, or the KMS key is set for an algorithm that
     *                                  does not use AWS KMS
     */
    static S3Encryption of(@Nullable String algorithm, @Nullable String kmsKeyId) {
        return new S3Encryption(parseAlgorithm(algorithm), StringUtils.trimToNull(kmsKeyId));
    }

    private static @Nullable ServerSideEncryption parseAlgorithm(@Nullable String value) {
        var name = StringUtils.trimToNull(value);
        if (name == null || LEGACY_NONE.equals(name)) {
            return null;
        }
        var algorithm = ServerSideEncryption.fromValue(name);
        if (algorithm == ServerSideEncryption.UNKNOWN_TO_SDK_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported server-side encryption algorithm '%s'. Supported algorithms: %s."
                            .formatted(name, quote(ServerSideEncryption.knownValues(), ", ")));
        }
        return algorithm;
    }

    private static String quote(Set<ServerSideEncryption> algorithms, String delimiter) {
        return algorithms.stream()
                .map(algorithm -> "'" + algorithm + "'")
                .sorted()
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Requests the encryption of the object that the given request writes.
     */
    void applyTo(PutObjectRequest.Builder request) {
        request.serverSideEncryption(algorithm).ssekmsKeyId(kmsKeyId);
    }

    /**
     * Requests the encryption of the object that the given request creates as a copy.
     */
    void applyTo(CopyObjectRequest.Builder request) {
        request.serverSideEncryption(algorithm).ssekmsKeyId(kmsKeyId);
    }
}
