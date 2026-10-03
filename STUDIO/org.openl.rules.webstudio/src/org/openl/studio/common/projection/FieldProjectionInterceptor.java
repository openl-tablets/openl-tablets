package org.openl.studio.common.projection;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Reads the {@code ?fields=} selection before an endpoint that answers with a projectable type runs.
 *
 * <p>A malformed or oversized selection answers {@code 400} before the endpoint changes anything. Read only while
 * the response is written, it would let a request create or edit data and still answer {@code 400} -- a created
 * personal access token, for example, whose secret the client would never see.
 *
 * <p>An endpoint declaring a type too generic to resolve, such as {@code ResponseEntity<?>}, has its selection read
 * once its body is known, by {@link FieldProjectionResponseBodyAdvice}.
 */
@Component
@RequiredArgsConstructor
public class FieldProjectionInterceptor implements HandlerInterceptor {

    private final FieldProjectionSupport support;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method
                && support.isProjectable(support.resolveTargetType(method.getReturnType().getGenericParameterType()))) {
            support.selectionOf(request);
        }
        return true;
    }
}
