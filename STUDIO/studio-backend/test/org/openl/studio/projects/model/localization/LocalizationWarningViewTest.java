package org.openl.studio.projects.model.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * A warning names its language on the wire, the default one included, which a screen matches to its column.
 */
class LocalizationWarningViewTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void namesTheDefaultLanguageAsAnEmptyName() throws Exception {
        assertEquals("{\"language\":\"\",\"summary\":\"Written twice.\"}",
                mapper.writeValueAsString(new LocalizationWarningView("", "Written twice.", null)));
    }
}
