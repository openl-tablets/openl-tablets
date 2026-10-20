package org.openl.studio.settings.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SecretSettingsTest {

    @Test
    void aSettingAModelDeclaresSecretIsSecret() {
        assertTrue(SecretSettings.isSecret("db.password"));
        assertTrue(SecretSettings.isSecret("mail.password"));
        assertTrue(SecretSettings.isSecret("security.oauth2.client-secret"));
    }

    @Test
    void aSettingOfARepositoryIsSecretByTheSuffixItsModelDeclares() {
        assertTrue(SecretSettings.isSecret("repository.design.password"));
        assertTrue(SecretSettings.isSecret("repository.production.access-key"));
        assertTrue(SecretSettings.isSecret("repository.production.secret-key"));
        assertTrue(SecretSettings.isSecret("repository.production.account-key"));
    }

    @Test
    void aSettingNamedAsASecretIsSecretThoughNoModelDeclaresIt() {
        assertTrue(SecretSettings.isSecret("security.ad.bind-password"));
        assertTrue(SecretSettings.isSecret("secret.key"));
        assertTrue(SecretSettings.isSecret("security.saml.client-secret-value"));
    }

    @Test
    void anOrdinarySettingIsNoSecret() {
        assertFalse(SecretSettings.isSecret("user.mode"));
        assertFalse(SecretSettings.isSecret("repository.design.uri"));
        assertFalse(SecretSettings.isSecret("repository.design.login"));
        assertFalse(SecretSettings.isSecret("mail.username"));
        assertFalse(SecretSettings.isSecret("project.history.count"));
    }

    @ParameterizedTest
    @MethodSource("valuesWithCredentials")
    void hidesTheCredentialsAValueHolds(String value, String masked) {
        assertEquals(masked, SecretSettings.masked(value));
    }

    @Test
    void readsALongValueInTimeLinearToItsLength() {
        // A parameter name repeating a secret word with no value took seconds when the name was searched backwards.
        var value = "?" + "pass".repeat(20_000);

        assertEquals(value, assertTimeoutPreemptively(Duration.ofSeconds(1), () -> SecretSettings.masked(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"smtp://mail.example.com:587", "https://github.com/org/rules.git",
            "git@github.com:org/rules.git", "jdbc:postgresql://db:5432/studio?user=studio", "dd.MM.yyyy", "multi"})
    void keepsAValueThatHoldsNoCredentials(String value) {
        assertEquals(value, SecretSettings.masked(value));
    }

    /** Values with credentials in them, each with the credentials hidden. */
    private static Stream<Arguments> valuesWithCredentials() {
        return Stream.of(
                arguments("https://bot:ghp_token@github.com/org/rules.git", "https://***@github.com/org/rules.git"),
                arguments("https://ghp_token@github.com/org/rules.git", "https://***@github.com/org/rules.git"),
                arguments("jdbc:postgresql://db:5432/studio?user=studio&password=s3cret&ssl=true",
                        "jdbc:postgresql://db:5432/studio?user=studio&password=***&ssl=true"),
                arguments("jdbc:sqlserver://db;databaseName=studio;password=s3cret",
                        "jdbc:sqlserver://db;databaseName=studio;password=***"),
                arguments("jdbc:oracle:thin:studio/s3cret@db:1521/studio", "jdbc:oracle:thin:***@db:1521/studio"),
                arguments("https://rules.blob.core.windows.net/rules?sv=2024-01-01&sig=abc%2Bdef",
                        "https://rules.blob.core.windows.net/rules?sv=2024-01-01&sig=***"),
                arguments("DefaultEndpointsProtocol=https;AccountName=rules;AccountKey=a2V5;EndpointSuffix=net",
                        "DefaultEndpointsProtocol=https;AccountName=rules;AccountKey=***;EndpointSuffix=net"),
                arguments("https://api.example.com/hook?access_token=t0ken#top",
                        "https://api.example.com/hook?access_token=***#top"),
                arguments("https://rules.example.com/?a=1?Secret-Token=t&x=1",
                        "https://rules.example.com/?a=1?Secret-Token=***&x=1"),
                arguments("?sig=abc&signature=def", "?sig=***&signature=def"));
    }
}
