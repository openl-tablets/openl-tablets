package org.openl.studio.common.projection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.openl.studio.common.projection.test.ProjectTestView;

/**
 * Direct invocation of {@link FieldProjectionResponseBodyAdvice#beforeBodyWriteInternal} verifying that
 * the advice <em>merges</em> its filter into an upstream {@link SimpleFilterProvider} rather than
 * replacing it. Same-package access lets the test call the protected method.
 */
class FieldProjectionFilterMergeTest {

    @Test
    void preservesExistingFilterProviderAndAddsProjectionFilter() {
        var support = new FieldProjectionSupport();
        var advice = new FieldProjectionResponseBodyAdvice(support);

        var servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("fields", "id,name");
        var request = new ServletServerHttpRequest(servletRequest);
        var response = new ServletServerHttpResponse(new MockHttpServletResponse());

        var body = new ProjectTestView("1", "name-1", "OPENED", "secret", "writeOnly", null, null);
        var container = new MappingJacksonValue(body);

        // Pre-existing provider as if set by another advice running before us.
        var preExisting = new SimpleFilterProvider()
                .setFailOnUnknownId(false)
                .addFilter("sentinel", SimpleBeanPropertyFilter.serializeAll());
        container.setFilters(preExisting);

        advice.beforeBodyWriteInternal(container, MediaType.APPLICATION_JSON, mock(MethodParameter.class), request, response);

        assertSame(preExisting, container.getFilters(), "advice must not replace the existing FilterProvider");
        var provider = (SimpleFilterProvider) container.getFilters();
        assertNotNull(provider.findPropertyFilter("sentinel", null), "pre-existing filter must remain");
        assertNotNull(provider.findPropertyFilter(FieldProjectionSupport.FILTER_ID, null),
                "projection filter must be added under the shared id");
    }
}
