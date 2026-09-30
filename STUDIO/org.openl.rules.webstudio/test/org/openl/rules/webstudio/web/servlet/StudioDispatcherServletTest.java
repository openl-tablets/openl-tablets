package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import jakarta.servlet.http.MappingMatch;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletMapping;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

/**
 * Pins which handler answers at which address: the dispatcher keeps a controller under {@code /rest/*} and the
 * handshake at {@code /ws}, although both mappings reach the same application context.
 *
 * @author Yury Molchan
 */
class StudioDispatcherServletTest {

    private final HttpRequestHandler controller = mock(HttpRequestHandler.class);
    private final WebSocketHttpRequestHandler handshake = mock(WebSocketHttpRequestHandler.class);

    @Test
    void aControllerAnswersUnderTheRestPrefix() throws Exception {
        var servlet = dispatcher(Map.of("/projects", controller), Map.of());

        var response = call(servlet, "/rest", "/projects");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(controller).handleRequest(any(), any());
    }

    @Test
    void theHandshakeAnswersAtTheWebSocketAddress() throws Exception {
        var servlet = dispatcher(Map.of(), Map.of("/ws", handshake));

        var response = call(servlet, "/ws", null);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(handshake).handleRequest(any(), any());
    }

    @Test
    void theHandshakeDoesNotAnswerUnderTheRestPrefix() throws Exception {
        var servlet = dispatcher(Map.of("/projects", controller), Map.of("/ws", handshake));

        var response = call(servlet, "/rest", "/ws");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        verify(handshake, never()).handleRequest(any(), any());
    }

    @Test
    void aControllerDoesNotAnswerAtTheWebSocketAddress() throws Exception {
        // A controller mapped at /ws shows that the address alone does not let a handler through.
        var servlet = dispatcher(Map.of("/ws", controller), Map.of());

        var response = call(servlet, "/ws", null);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        verify(controller, never()).handleRequest(any(), any());
    }

    @Test
    void aControllerMatchingTheWebSocketAddressDoesNotHideTheHandshake() throws Exception {
        // The mapping of controllers comes first, and matches the address too.
        var servlet = dispatcher(Map.of("/ws", controller), Map.of("/ws", handshake));

        var response = call(servlet, "/ws", null);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(handshake).handleRequest(any(), any());
        verify(controller, never()).handleRequest(any(), any());
    }

    @Test
    void anUnknownAddressIsNotFound() throws Exception {
        var servlet = dispatcher(Map.of("/projects", controller), Map.of("/ws", handshake));

        var response = call(servlet, "/rest", "/unknown");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    /**
     * Builds the dispatcher over an application context that has a mapping of controllers and, as the STOMP
     * endpoint registers, a WebSocket mapping that comes after it. A mapping without handlers is left out.
     */
    private static StudioDispatcherServlet dispatcher(Map<String, Object> controllers,
                                                      Map<String, Object> handshakes) throws Exception {
        var servletContext = new MockServletContext();
        var context = new StaticWebApplicationContext();
        context.setServletContext(servletContext);
        if (!controllers.isEmpty()) {
            context.registerBean("controllers", SimpleUrlHandlerMapping.class,
                    () -> new SimpleUrlHandlerMapping(controllers, 0));
        }
        if (!handshakes.isEmpty()) {
            context.registerBean("handshakes", WebSocketHandlerMapping.class, () -> {
                var mapping = new WebSocketHandlerMapping();
                mapping.setUrlMap(handshakes);
                mapping.setOrder(1);
                return mapping;
            });
        }
        context.registerBean("adapter", HttpRequestHandlerAdapter.class);
        context.refresh();

        var servlet = new StudioDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.init(new MockServletConfig(servletContext, "springDispatcher"));
        return servlet;
    }

    private static MockHttpServletResponse call(StudioDispatcherServlet servlet, String servletPath, String pathInfo)
            throws Exception {
        var request = new MockHttpServletRequest("GET", servletPath + (pathInfo == null ? "" : pathInfo));
        request.setServletPath(servletPath);
        request.setPathInfo(pathInfo);
        request.setHttpServletMapping(pathInfo == null
                ? new MockHttpServletMapping(servletPath.substring(1), servletPath, "springDispatcher",
                        MappingMatch.EXACT)
                : new MockHttpServletMapping(pathInfo.substring(1), servletPath + "/*", "springDispatcher",
                        MappingMatch.PATH));
        var response = new MockHttpServletResponse();
        servlet.service(request, response);
        return response;
    }
}
