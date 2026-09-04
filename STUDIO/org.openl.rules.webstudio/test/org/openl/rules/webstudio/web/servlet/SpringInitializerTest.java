package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

class SpringInitializerTest {

    @Test
    void dispatcherKeepsSmallMultipartFieldsInMemory() {
        var applicationContext = mock(XmlWebApplicationContext.class);
        var servletContext = mock(ServletContext.class);
        var registration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.addServlet(eq("springDispatcher"), any(DispatcherServlet.class)))
                .thenReturn(registration);
        var initializer = new SpringInitializer();
        ReflectionTestUtils.setField(initializer, "applicationContext", applicationContext);

        ReflectionTestUtils.invokeMethod(initializer, "registerDispatcherServlet", servletContext);

        var configCaptor = ArgumentCaptor.forClass(MultipartConfigElement.class);
        verify(registration).setMultipartConfig(configCaptor.capture());
        var config = configCaptor.getValue();
        assertEquals(-1L, config.getMaxFileSize());
        assertEquals(-1L, config.getMaxRequestSize());
        assertEquals(8 * 1024, config.getFileSizeThreshold());
    }
}
