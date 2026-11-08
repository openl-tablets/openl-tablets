package org.openl.studio.projects.model.localization;

import java.util.Collection;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.model.PageResponse;

/**
 * A page of the keys of the messages, with the languages of the project.
 */
@Getter
@Schema(description = "A page of the keys of the messages with their values, and the languages of the project.")
public final class LocalizationMessagesPage extends PageResponse<LocalizationMessageView> {

    @Parameter(description = "Languages of the project, the default one first, then the others by their tags.")
    private final List<LocalizationLanguageView> languages;

    public LocalizationMessagesPage(List<LocalizationLanguageView> languages,
                                    Collection<LocalizationMessageView> content,
                                    Pageable page,
                                    long total) {
        super(content, page, total);
        this.languages = languages;
    }
}
