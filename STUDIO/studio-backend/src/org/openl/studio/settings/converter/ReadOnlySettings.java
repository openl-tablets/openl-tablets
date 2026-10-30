package org.openl.studio.settings.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.studio.common.exception.BadRequestException;

/**
 * Refuses a request that changes a read-only setting.
 *
 * <p>A setting is read-only when the settings are answered with it wrapped as read-only (see
 * {@link SettingValueWrapperSerializer}): an environment variable or a system property sets it, directly or through
 * a {@code $ref} key. The request is compared with the settings as they are answered, before a setting model reads
 * it, so a value the model would reformat, such as a path it ends with a slash, still counts as unchanged.
 *
 * <p>A setting the request leaves out is unchanged, and so is one it gives no value: a setting the environment sets keeps
 * its value when it is reset, as it does when all the settings are reset. Values are compared as text, so {@code 60} and
 * {@code "60"} are the same, and the value of a secret setting is compared too, though it is never answered.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReadOnlySettings {

    private static final String VALUE = "value";
    private static final String READ_ONLY = "readOnly";
    private static final String SECRET = "secret";
    /** Asks {@link SettingValueWrapperSerializer} to write the value of a secret setting, for a comparison. */
    static final String SECRETS_REVEALED = ReadOnlySettings.class.getName() + ".secretsRevealed";

    /**
     * The settings as they are answered, with the values of the secret settings, to compare a request with. The
     * answer never leaves the server.
     */
    public static JsonNode answerOf(ObjectMapper mapper, Object settings) throws IOException {
        var writer = mapper.writer().withAttribute(SECRETS_REVEALED, Boolean.TRUE);
        return mapper.readTree(writer.writeValueAsBytes(settings));
    }

    /**
     * Refuses the request with {@code 400 Bad Request} naming every read-only setting it changes, in alphabetical
     * order.
     *
     * @param answered  the settings as they are answered now
     * @param requested the settings the request holds, in the same shape
     */
    public static void check(JsonNode answered, JsonNode requested) {
        var changed = new ArrayList<String>();
        collectChanged(answered, requested, "", changed);
        if (!changed.isEmpty()) {
            changed.sort(null);
            throw new BadRequestException("settings.read-only.message", new Object[]{String.join(", ", changed)});
        }
    }

    private static void collectChanged(JsonNode answered, JsonNode requested, String path, List<String> changed) {
        for (var setting : answered.properties()) {
            var name = path + setting.getKey();
            var answer = setting.getValue();
            var request = requested.path(setting.getKey());
            if (isReadOnly(answer)) {
                if (isChanged(answer, request)) {
                    changed.add(name);
                }
            } else if (answer.isObject() && request.isObject()) {
                collectChanged(answer, request, name + ".", changed);
            }
        }
    }

    private static boolean isChanged(JsonNode answer, JsonNode request) {
        var value = isWrapped(request) ? request.path(VALUE) : request;
        return !value.isMissingNode() && !value.isNull() && !textOf(answer).equals(textOf(value));
    }

    private static boolean isReadOnly(JsonNode setting) {
        return isWrapped(setting) && setting.path(READ_ONLY).asBoolean();
    }

    private static boolean isWrapped(JsonNode setting) {
        return setting.isObject() && (setting.has(READ_ONLY) || setting.has(SECRET));
    }

    private static String textOf(JsonNode setting) {
        if (isWrapped(setting)) {
            return textOf(setting.path(VALUE));
        }
        if (setting.isMissingNode() || setting.isNull()) {
            return "";
        }
        return setting.isValueNode() ? setting.asText() : setting.toString();
    }
}
