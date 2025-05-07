package org.openl.rules.spring.openapi;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import org.openl.util.StreamUtils;

/**
 * Spring MVC Helper
 *
 * @author Vladyslav Pikus
 */
public class SpringMvcHandlerMethodsHelper {

    private final ApplicationContext context;
    private volatile Map<RequestMappingInfo, HandlerMethod> handlerMethods;
    private volatile Map<String, Object> controllerAdvices;

    public SpringMvcHandlerMethodsHelper(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Find all Spring Methods Handlers
     *
     * @return found methods handlers
     */
    public Map<RequestMappingInfo, HandlerMethod> getHandlerMethods() {
        if (this.handlerMethods == null) {
            synchronized (this) {
                if (this.handlerMethods == null) {
                    var requestMappingHandlers = context.getBeansOfType(RequestMappingHandlerMapping.class);
                    this.handlerMethods = requestMappingHandlers.values()
                            .stream()
                            .map(AbstractHandlerMethodMapping::getHandlerMethods)
                            .map(Map::entrySet)
                            .flatMap(Collection::stream)
                            .filter(e -> {
                                var handler = e.getValue();
                                return !OpenApiUtils.isHiddenApiMethod(handler.getMethod(), handler.getBeanType());
                            })
                            .collect(StreamUtils.toLinkedMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }
        }
        return this.handlerMethods;
    }

    /**
     * Find all public Spring Controller Advices
     *
     * @return found controller advice beans
     */
    public Map<String, Object> getControllerAdvices() {
        if (this.controllerAdvices == null) {
            synchronized (this) {
                if (this.controllerAdvices == null) {
                    var controllerAdviceMap = context.getBeansWithAnnotation(ControllerAdvice.class);
                    this.controllerAdvices = Stream.of(controllerAdviceMap)
                            .flatMap(mapEl -> mapEl.entrySet().stream())
                            .filter(controller -> !OpenApiUtils.isHidden(controller.getValue().getClass()))
                            .collect(StreamUtils.toLinkedMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }
        }
        return this.controllerAdvices;
    }

}
