package org.openl.studio.projects.model.localization;

import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;


/**
 * One change of the localization files of a project. {@code operation} selects the change.
 *
 * <p>A language is named as the file name names it: the language, country and variant joined by underscores, as
 * {@code de_LU}, and an empty name for the default file.
 */
@Schema(description = "One change of the localization files. `operation` selects the change.",
        discriminatorProperty = "operation")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "operation")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LocalizationAction.SetValue.class, name = "setValue"),
        @JsonSubTypes.Type(value = LocalizationAction.RemoveValue.class, name = "removeValue"),
        @JsonSubTypes.Type(value = LocalizationAction.AddKey.class, name = "addKey"),
        @JsonSubTypes.Type(value = LocalizationAction.RemoveKey.class, name = "removeKey"),
        @JsonSubTypes.Type(value = LocalizationAction.AddLanguage.class, name = "addLanguage")
})
public sealed interface LocalizationAction permits LocalizationAction.SetValue,
        LocalizationAction.RemoveValue,
        LocalizationAction.AddKey,
        LocalizationAction.RemoveKey,
        LocalizationAction.AddLanguage {

    /**
     * The name of a language a change writes: the language in lower case, then the country and the variant, each after
     * an underscore, as {@code de}, {@code de_LU} or {@code de__POSIX}. The default one is empty. The service also
     * requires the country in upper case, as the engine names the file of the locale: a file named in another case is
     * not read on a file system that tells the cases apart.
     */
    String LANGUAGE = "^(?:[a-z]{2,3}(?:_\\w+)?)?$";

    @Schema(name = "SetValue", description = """
            Writes the value of a key in a language: over the key where the file of the language holds it, or at the \
            end of the file.""")
    record SetValue(
            @Parameter(description = "Key of the messages.", required = true)
            @NotBlank String key,

            @Parameter(description = "Language of the value, empty for the default file.", required = true)
            @NotNull @Pattern(regexp = LANGUAGE) String language,

            @Parameter(description = "Value of the key.", required = true)
            @NotNull String value
    ) implements LocalizationAction {
    }

    @Schema(name = "RemoveValue", description = """
            Takes the value of a key out of the file of a language, so the language takes the value of the language \
            it falls back to.""")
    record RemoveValue(
            @Parameter(description = "Key of the messages.", required = true)
            @NotBlank String key,

            @Parameter(description = "Language of the value, empty for the default file.", required = true)
            @NotNull @Pattern(regexp = LANGUAGE) String language
    ) implements LocalizationAction {
    }

    @Schema(name = "AddKey", description = "Adds a key no language has, at the end of the file of each language given.")
    record AddKey(
            @Parameter(description = "Key of the messages.", required = true)
            @NotBlank String key,

            @Parameter(description = "Values of the key by the language, the default language as an empty name.",
                    required = true)
            @NotEmpty Map<String, @NotNull String> values
    ) implements LocalizationAction {
    }

    @Schema(name = "RemoveKey", description = "Takes a key out of the file of every language.")
    record RemoveKey(
            @Parameter(description = "Key of the messages.", required = true)
            @NotBlank String key
    ) implements LocalizationAction {
    }

    @Schema(name = "AddLanguage",
            description = "Adds a language the project has no file of: the file is created with the values given, "
                    + "in their order.")
    record AddLanguage(
            @Parameter(description = "Language to add, empty for the default file.", required = true)
            @NotNull @Pattern(regexp = LANGUAGE) String language,

            @Parameter(description = "Values of the keys in the language. Absent for an empty file.")
            @Nullable Map<@NotBlank String, @NotNull String> values
    ) implements LocalizationAction {
    }
}
