package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;

class DispatchInfoTest {

    @Test
    void labelsCandidatesByTheirDimensionsAndFlagsTheChosen() {
        IOpenMethod v2020 = versioned(Map.of("effectiveDate", "01/01/2020"));
        IOpenMethod v2021 = versioned(Map.of("effectiveDate", "01/01/2021"));
        OpenMethodDispatcher dispatcher = mock(OpenMethodDispatcher.class);
        when(dispatcher.getCandidates()).thenReturn(List.of(v2020, v2021));

        DispatchInfo info = DispatchInfo.of(dispatcher, v2021);

        assertEquals(List.of("effectiveDate: 01/01/2020", "effectiveDate: 01/01/2021"),
                info.candidates().stream().map(DispatchInfo.Candidate::label).toList());
        assertFalse(info.candidates().get(0).chosen());
        assertTrue(info.candidates().get(1).chosen(), "the selected version is flagged");
    }

    @Test
    void fallsBackToTheMethodNameWhenThereAreNoDimensions() {
        IOpenMethod plain = mock(IOpenMethod.class);
        when(plain.getName()).thenReturn("Rate");
        OpenMethodDispatcher dispatcher = mock(OpenMethodDispatcher.class);
        when(dispatcher.getCandidates()).thenReturn(List.of(plain));

        DispatchInfo info = DispatchInfo.of(dispatcher, null);

        assertEquals("Rate", info.candidates().get(0).label());
        assertFalse(info.candidates().get(0).chosen());
    }

    /** A candidate that carries dimension properties, like a table overloaded by {@code effectiveDate}. */
    private static IOpenMethod versioned(Map<String, Object> dimensions) {
        ITableProperties properties = mock(ITableProperties.class);
        when(properties.getAllDimensionalProperties()).thenReturn(dimensions);
        dimensions.forEach((name, value) ->
                when(properties.getPropertyValueAsString(name)).thenReturn(String.valueOf(value)));
        IOpenMethod method = mock(IOpenMethod.class, withSettings().extraInterfaces(ITablePropertiesMethod.class));
        when(((ITablePropertiesMethod) method).getMethodProperties()).thenReturn(properties);
        return method;
    }
}
