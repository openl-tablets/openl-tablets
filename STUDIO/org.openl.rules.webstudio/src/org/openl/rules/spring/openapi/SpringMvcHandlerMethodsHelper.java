package org.openl.rules.spring.openapi;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SpringMvcHandlerMethodsHelper {

    private final ApplicationContext context;
    private final AtomicReference<Map<RequestMappingInfo, HandlerMethod>> handlerMethods = new AtomicReference<>();
    private final AtomicReference<Map<String, Object>> controllerAdvices = new AtomicReference<>();

    /**
     * Find all Spring Methods Handlers
     *
     * @return found methods handlers
     */
    public Map<RequestMappingInfo, HandlerMethod> getHandlerMethods() {
        var methods = handlerMethods.get();
        if (methods == null) {
            synchronized (this) {
                methods = handlerMethods.get();
                if (methods == null) {
                    var requestMappingHandlers = context.getBeansOfType(RequestMappingHandlerMapping.class);
                    methods = requestMappingHandlers.values()
                            .stream()
                            .map(AbstractHandlerMethodMapping::getHandlerMethods)
                            .map(Map::entrySet)
                            .flatMap(Collection::stream)
                            .filter(e -> {
                                var handler = e.getValue();
                                return !OpenApiUtils.isHiddenApiMethod(handler.getMethod(), handler.getBeanType());
                            })
                            .collect(StreamUtils.toLinkedMap(Map.Entry::getKey, Map.Entry::getValue));
                    handlerMethods.set(methods);
                }
            }
        }
        return methods;
    }

    /**
     * Find all public Spring Controller Advices
     *
     * @return found controller advice beans
     */
    public Map<String, Object> getControllerAdvices() {
        var advices = controllerAdvices.get();
        if (advices == null) {
            synchronized (this) {
                advices = controllerAdvices.get();
                if (advices == null) {
                    var controllerAdviceMap = context.getBeansWithAnnotation(ControllerAdvice.class);
                    advices = Stream.of(controllerAdviceMap)
                            .flatMap(mapEl -> mapEl.entrySet().stream())
                            .filter(controller -> !OpenApiUtils.isHidden(controller.getValue().getClass()))
                            .collect(StreamUtils.toLinkedMap(Map.Entry::getKey, Map.Entry::getValue));
                    controllerAdvices.set(advices);
                }
            }
        }
        return advices;
    }

}
