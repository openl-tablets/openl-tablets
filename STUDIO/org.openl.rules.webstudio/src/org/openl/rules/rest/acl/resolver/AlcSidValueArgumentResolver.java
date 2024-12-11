package org.openl.rules.rest.acl.resolver;

import jakarta.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.core.MethodParameter;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * A custom {@link HandlerMethodArgumentResolver} to resolve {@link Sid} arguments
 * in controller methods. The resolver determines whether the SID is a {@link PrincipalSid}
 * or a {@link GrantedAuthoritySid} based on request parameters.
 */
@ParametersAreNonnullByDefault
public class AlcSidValueArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PARAM_SID = "sid";
    private static final String PARAM_PRINCIPAL = "principal";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        var type = parameter.getParameterType();
        return type.isAssignableFrom(PrincipalSid.class) && type.isAssignableFrom(GrantedAuthoritySid.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) {
        var sid = getSid(webRequest);
        if (sid == null) {
            return null;
        }
        return isPrincipal(webRequest)
                ? new PrincipalSid(sid)
                : new GrantedAuthoritySid(sid);
    }

    private String getSid(NativeWebRequest webRequest) {
        return webRequest.getParameter(PARAM_SID);
    }

    private boolean isPrincipal(NativeWebRequest webRequest) {
        return Boolean.parseBoolean(webRequest.getParameter(PARAM_PRINCIPAL));
    }
}
