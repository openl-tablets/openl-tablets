package org.openl.studio.common;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

/**
 * This is like {@linkplain org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice}
 * but it works on class level, not method level.
 *
 * @author Yury Molchan
 */
@ControllerAdvice
@Order(1)
public class JsonViewControllerAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return super.supports(returnType, converterType) && returnType.getContainingClass().isAnnotationPresent(JsonView.class);
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
                                           MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

        var ann = returnType.getContainingClass().getAnnotation(JsonView.class);
        Assert.state(ann != null, "No JsonView annotation");

        Class<?>[] classes = ann.value();
        if (classes.length != 1) {
            throw new IllegalArgumentException(
                    "@JsonView only supported for response body advice with exactly 1 class argument: " + returnType);
        }

        bodyContainer.setSerializationView(classes[0]);

    }
}
