package org.openl.rules.spring.openapi;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.openl.util.StreamUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.swagger.v3.oas.annotations.Hidden;

public class SpringMvcHandlerMethodsHelper {

    private final ApplicationContext context;
    private volatile Map<RequestMappingInfo, HandlerMethod> handlerMethods;

    public SpringMvcHandlerMethodsHelper(ApplicationContext context) {
        this.context = context;
    }

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
                        .collect(StreamUtils.toLinkedMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }
        }
        return this.handlerMethods;
    }

    public Map<String, Object> getControllerAdvices() {
        Map<String, Object> controllerAdviceMap = context.getBeansWithAnnotation(ControllerAdvice.class);
        return Stream.of(controllerAdviceMap)
            .flatMap(mapEl -> mapEl.entrySet().stream())
            .filter(
                controller -> (AnnotationUtils.findAnnotation(controller.getValue().getClass(), Hidden.class) == null))
            .collect(StreamUtils.toLinkedMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
