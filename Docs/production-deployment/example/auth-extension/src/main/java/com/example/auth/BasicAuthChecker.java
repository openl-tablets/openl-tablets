package com.example.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

import org.openl.rules.ruleservice.api.AuthorizationChecker;

@Order(1)
public class BasicAuthChecker implements AuthorizationChecker {

    public static final String BASIC_AUTH_PREFIX = "Basic ";
    private final byte[] expected;
    
    public BasicAuthChecker(@Value("${example.auth.username}") String username, @Value("${example.auth.password}") String password) {
        expected = (username + ":" + password).getBytes(StandardCharsets.ISO_8859_1);
    }

    @Override
    public boolean authorize(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BASIC_AUTH_PREFIX)) {
            return false;
        }
        
        String base64Credentials = authHeader.substring(BASIC_AUTH_PREFIX.length());

        byte[] provided;
        try {
            provided = Base64.getDecoder().decode(base64Credentials);
        } catch (IllegalArgumentException e) {
            return false; 
        }

        return MessageDigest.isEqual(provided, expected); 
    }
}
