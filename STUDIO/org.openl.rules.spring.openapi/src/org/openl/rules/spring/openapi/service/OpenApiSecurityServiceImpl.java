package org.openl.rules.spring.openapi.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openl.rules.spring.openapi.model.SecuritySchemePair;
import org.openl.util.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI Security schema service
 *
 * @author Vladyslav Pikus
 */
@Component
public class OpenApiSecurityServiceImpl implements OpenApiSecurityService {

    private final List<io.swagger.v3.oas.annotations.security.SecurityScheme> globalSecuritySchemes;

    public OpenApiSecurityServiceImpl(ApplicationContext context) {
        this.globalSecuritySchemes = context
            .getBeansWithAnnotation(io.swagger.v3.oas.annotations.security.SecurityScheme.class)
            .values()
            .stream()
            .map(Object::getClass)
            .map(this::getSecurityScheme)
            .collect(Collectors.toList());
    }

    /**
     * Resolves security schema if present
     *
     * @param apiContext curren OpenAPI context
     */
    @Override
    public void generateGlobalSecurity(OpenApiContext apiContext) {
        globalSecuritySchemes.stream().map(this::getSecurityScheme).flatMap(Optional::stream).forEach(pair -> {
            apiContext.getOpenAPI().addSecurityItem(new SecurityRequirement().addList(pair.key));
            apiContext.getComponents().addSecuritySchemes(pair.key, pair.securityScheme);
        });
    }

    private Optional<SecuritySchemePair> getSecurityScheme(
            io.swagger.v3.oas.annotations.security.SecurityScheme securityScheme) {
        if (securityScheme == null) {
            return Optional.empty();
        }
        String key = null;
        SecurityScheme securitySchemeObject = new SecurityScheme();

        if (StringUtils.isNotBlank(securityScheme.in().toString())) {
            securitySchemeObject.setIn(getIn(securityScheme.in().toString()));
        }
        if (StringUtils.isNotBlank(securityScheme.type().toString())) {
            securitySchemeObject.setType(getType(securityScheme.type().toString()));
        }

        if (StringUtils.isNotBlank(securityScheme.openIdConnectUrl())) {
            securitySchemeObject.setOpenIdConnectUrl(securityScheme.openIdConnectUrl());
        }
        if (StringUtils.isNotBlank(securityScheme.scheme())) {
            securitySchemeObject.setScheme(securityScheme.scheme());
        }

        if (StringUtils.isNotBlank(securityScheme.bearerFormat())) {
            securitySchemeObject.setBearerFormat(securityScheme.bearerFormat());
        }
        if (StringUtils.isNotBlank(securityScheme.description())) {
            securitySchemeObject.setDescription(securityScheme.description());
        }
        if (StringUtils.isNotBlank(securityScheme.paramName())) {
            securitySchemeObject.setName(securityScheme.paramName());
        }
        if (StringUtils.isNotBlank(securityScheme.ref())) {
            securitySchemeObject.set$ref(securityScheme.ref());
        }
        if (StringUtils.isNotBlank(securityScheme.name())) {
            key = securityScheme.name();
        }

        if (securityScheme.extensions().length > 0) {
            AnnotationsUtils.getExtensions(securityScheme.extensions()).forEach(securitySchemeObject::addExtension);
        }

        getOAuthFlows(securityScheme.flows()).ifPresent(securitySchemeObject::setFlows);
        return Optional.of(new SecuritySchemePair(key, securitySchemeObject));
    }

    private static Optional<OAuthFlows> getOAuthFlows(io.swagger.v3.oas.annotations.security.OAuthFlows oAuthFlows) {
        if (isEmpty(oAuthFlows)) {
            return Optional.empty();
        }
        OAuthFlows oAuthFlowsObject = new OAuthFlows();
        if (oAuthFlows.extensions().length > 0) {
            AnnotationsUtils.getExtensions(oAuthFlows.extensions()).forEach(oAuthFlowsObject::addExtension);
        }

        getOAuthFlow(oAuthFlows.authorizationCode()).ifPresent(oAuthFlowsObject::setAuthorizationCode);
        getOAuthFlow(oAuthFlows.clientCredentials()).ifPresent(oAuthFlowsObject::setClientCredentials);
        getOAuthFlow(oAuthFlows.implicit()).ifPresent(oAuthFlowsObject::setImplicit);
        getOAuthFlow(oAuthFlows.password()).ifPresent(oAuthFlowsObject::setPassword);
        return Optional.of(oAuthFlowsObject);
    }

    private static Optional<OAuthFlow> getOAuthFlow(io.swagger.v3.oas.annotations.security.OAuthFlow oAuthFlow) {
        if (isEmpty(oAuthFlow)) {
            return Optional.empty();
        }
        OAuthFlow oAuthFlowObject = new OAuthFlow();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(oAuthFlow.authorizationUrl())) {
            oAuthFlowObject.setAuthorizationUrl(oAuthFlow.authorizationUrl());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(oAuthFlow.refreshUrl())) {
            oAuthFlowObject.setRefreshUrl(oAuthFlow.refreshUrl());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(oAuthFlow.tokenUrl())) {
            oAuthFlowObject.setTokenUrl(oAuthFlow.tokenUrl());
        }
        if (oAuthFlow.extensions().length > 0) {
            AnnotationsUtils.getExtensions(oAuthFlow.extensions()).forEach(oAuthFlowObject::addExtension);
        }

        getScopes(oAuthFlow.scopes()).ifPresent(oAuthFlowObject::setScopes);
        return Optional.of(oAuthFlowObject);
    }

    private static Optional<Scopes> getScopes(OAuthScope[] scopes) {
        if (isEmpty(scopes)) {
            return Optional.empty();
        }
        Scopes scopesObject = new Scopes();

        for (OAuthScope scope : scopes) {
            scopesObject.addString(scope.name(), scope.description());
        }
        return Optional.of(scopesObject);
    }

    private static SecurityScheme.In getIn(String value) {
        return Arrays.stream(SecurityScheme.In.values())
            .filter(i -> i.toString().equals(value))
            .findFirst()
            .orElse(null);
    }

    private static SecurityScheme.Type getType(String value) {
        return Arrays.stream(SecurityScheme.Type.values())
            .filter(i -> i.toString().equals(value))
            .findFirst()
            .orElse(null);
    }

    private static boolean isEmpty(io.swagger.v3.oas.annotations.security.OAuthFlows oAuthFlows) {
        if (oAuthFlows == null) {
            return true;
        }
        if (!isEmpty(oAuthFlows.implicit())) {
            return false;
        }
        if (!isEmpty(oAuthFlows.authorizationCode())) {
            return false;
        }
        if (!isEmpty(oAuthFlows.clientCredentials())) {
            return false;
        }
        if (!isEmpty(oAuthFlows.password())) {
            return false;
        }
        return oAuthFlows.extensions().length == 0;
    }

    private static boolean isEmpty(io.swagger.v3.oas.annotations.security.OAuthFlow oAuthFlow) {
        if (oAuthFlow == null) {
            return true;
        }
        if (!StringUtils.isBlank(oAuthFlow.authorizationUrl())) {
            return false;
        }
        if (!StringUtils.isBlank(oAuthFlow.refreshUrl())) {
            return false;
        }
        if (!StringUtils.isBlank(oAuthFlow.tokenUrl())) {
            return false;
        }
        if (!isEmpty(oAuthFlow.scopes())) {
            return false;
        }
        return oAuthFlow.extensions().length == 0;
    }

    private static boolean isEmpty(OAuthScope[] scopes) {
        return scopes == null || scopes.length == 0;
    }

    private io.swagger.v3.oas.annotations.security.SecurityScheme getSecurityScheme(Class<?> cl) {
        return AnnotationUtils.findAnnotation(cl, io.swagger.v3.oas.annotations.security.SecurityScheme.class);
    }
}
