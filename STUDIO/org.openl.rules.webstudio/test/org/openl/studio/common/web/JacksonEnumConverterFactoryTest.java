package org.openl.studio.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.web.trace.debug.DebugStatus;
import org.openl.studio.projects.model.trace.StepType;

class JacksonEnumConverterFactoryTest {

    private final JacksonEnumConverterFactory factory = new JacksonEnumConverterFactory(new ObjectMapper());

    @Test
    void bindsByJsonPropertyCode() {
        var converter = factory.getConverter(StepType.class);
        assertEquals(StepType.INTO, converter.convert("into"));
        assertEquals(StepType.OUT, converter.convert("out"));
        // A session status binds from its lowercase code, not the Java-style constant name.
        assertEquals(DebugStatus.SUSPENDED, factory.getConverter(DebugStatus.class).convert("suspended"));
    }

    @Test
    void bindsAnEnumWithoutCodeByName() {
        // An enum with no @JsonProperty has no code, so Jackson falls back to the constant name.
        assertEquals(Plain.FIRST, factory.getConverter(Plain.class).convert("FIRST"));
    }

    private enum Plain {
        FIRST,
        SECOND
    }

    @Test
    void rejectsAnUnknownValueAsBadRequest() {
        var converter = factory.getConverter(StepType.class);
        assertThrows(IllegalArgumentException.class, () -> converter.convert("sideways"));
    }

    @Test
    void treatsBlankAsAbsent() {
        assertNull(factory.getConverter(StepType.class).convert("  "));
    }
}
