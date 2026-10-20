package org.openl.studio.settings.converter;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Tells the system settings whose values are secret, such as passwords and access keys.
 *
 * <p>A setting is secret when a settings model declares it so: a field or a getter annotated with
 * {@link SettingPropertyName#secret()}, naming the property in full or by the suffix a repository adds to its own
 * prefix (see {@link SettingDeclarations}).
 *
 * <p>A property is taken for secret by its name as well, whether a model declares it or not: when the last part of
 * the name holds {@code password} or {@code secret}, or ends with {@code key}. A secret value is never shown, so
 * taking one setting too many for secret costs nothing.
 *
 * <p>The value of an ordinary setting may still hold credentials, such as an address with a user and a token in it:
 * {@link #masked(String)} hides them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecretSettings {

    /** The user and the password of an address, as in {@code https://user:token@host}. */
    private static final Pattern ADDRESS_CREDENTIALS = Pattern.compile("(?<=://)[^/?#@\\s]+@");
    /** The user and the password of a connection string, as in {@code jdbc:oracle:thin:user/password@host}. */
    private static final Pattern CONNECTION_CREDENTIALS = Pattern.compile("(?<=:)[^:/@\\s]+/[^@\\s]+@");
    /**
     * A parameter of an address or a connection string whose name tells a secret, as {@code ?password=} does. The
     * name is looked through once, so a long value without the parameter costs no more than reading it.
     */
    private static final Pattern SECRET_PARAMETERS = Pattern.compile(
            "(?i)([?&;](?=[^=&;?#]*(?:pass|pwd|secret|token|key|credential))[^=&;?#]*+=)[^&;#]*");
    /** The signature of a shared access address, as {@code ?sig=} of Azure Blob Storage. */
    private static final Pattern SIGNATURE_PARAMETER = Pattern.compile("(?i)([?&;]sig=)[^&;#]*");
    private static final String HIDDEN = "***";

    private static final Set<String> NAMES = SettingDeclarations.names(SettingPropertyName::secret);
    private static final Set<String> SUFFIXES = SettingDeclarations.suffixes(SettingPropertyName::secret);

    /**
     * Whether the value of the setting of the given property is secret.
     */
    public static boolean isSecret(String property) {
        return NAMES.contains(property) || SUFFIXES.stream().anyMatch(property::endsWith) || isSecretByName(property);
    }

    /**
     * The value of a setting with the credentials it holds hidden: the user and the password of an address or a
     * connection string, and every parameter whose name tells a secret, such as a password, a token or a key. Any
     * other part of the value is kept.
     */
    public static String masked(String value) {
        var hidden = ADDRESS_CREDENTIALS.matcher(value).replaceAll(HIDDEN + "@");
        hidden = CONNECTION_CREDENTIALS.matcher(hidden).replaceAll(HIDDEN + "@");
        hidden = SIGNATURE_PARAMETER.matcher(hidden).replaceAll("$1" + HIDDEN);
        return SECRET_PARAMETERS.matcher(hidden).replaceAll("$1" + HIDDEN);
    }

    private static boolean isSecretByName(String property) {
        var name = property.substring(property.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return name.contains("password") || name.contains("secret") || name.endsWith("key");
    }
}
