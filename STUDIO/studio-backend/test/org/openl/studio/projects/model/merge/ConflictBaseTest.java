package org.openl.studio.projects.model.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.studio.common.web.JacksonEnumConverterFactory;

class ConflictBaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void versionIsNamedByItsLowercaseCode() throws JsonProcessingException {
        var converter = new JacksonEnumConverterFactory(objectMapper).getConverter(ConflictBase.class);

        assertEquals(ConflictBase.BASE, converter.convert("base"));
        assertEquals(ConflictBase.OURS, converter.convert("ours"));
        assertEquals(ConflictBase.THEIRS, converter.convert("theirs"));
        assertEquals("\"ours\"", objectMapper.writeValueAsString(ConflictBase.OURS));
    }

    @Test
    void versionIsStillTakenByItsUpperCaseName() {
        var converter = new JacksonEnumConverterFactory(objectMapper).getConverter(ConflictBase.class);

        // Clients written for earlier versions of OpenL Studio send the names in upper case.
        assertEquals(ConflictBase.BASE, converter.convert("BASE"));
        assertEquals(ConflictBase.OURS, converter.convert("OURS"));
        assertEquals(ConflictBase.THEIRS, converter.convert("THEIRS"));
    }
}
