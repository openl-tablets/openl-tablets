package org.openl.studio.settings.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.projection.FieldProjectionSupport;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.config.ObjectMapperConfig;

/**
 * A merge patch of the system settings with a value the settings cannot hold is refused as an unreadable request
 * body, which answers {@code 400} naming the field, and nothing is stored (EPBDS-15704).
 *
 * @author Yury Molchan
 */
class SystemSettingsControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper(new FieldProjectionSupport());
    private SettingsService settingsService;
    private SystemSettingsController controller;

    @BeforeEach
    void init() {
        settingsService = mock(SettingsService.class);
        controller = new SystemSettingsController(objectMapper, new BeanValidationProvider(List.of()),
                settingsService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"aaa\"", "\"#%\"", "1.1"})
    void threadCountThatIsNotAWholeNumberIsAnUnreadableBody(String value) throws Exception {
        var patch = objectMapper.readTree("{\"testRunThreadCount\": " + value + "}");
        var request = new MockHttpServletRequest();

        var ex = assertThrows(HttpMessageNotReadableException.class,
                () -> controller.mergePatchSettings(patch, request));

        var cause = assertInstanceOf(JsonMappingException.class, ex.getCause());
        assertEquals("testRunThreadCount", cause.getPath().getFirst().getFieldName());
        verify(settingsService, never()).store(any());
        verify(settingsService, never()).commit();
    }

    @Test
    void wholeThreadCountIsStored() throws Exception {
        var patch = objectMapper.readTree("{\"testRunThreadCount\": 2}");

        controller.mergePatchSettings(patch, new MockHttpServletRequest());

        var stored = ArgumentCaptor.forClass(SettingsHolder.class);
        verify(settingsService).store(stored.capture());
        verify(settingsService).commit();
        assertEquals(2, ((AdministrationSettings) stored.getValue()).getTestRunThreadCount());
    }
}
