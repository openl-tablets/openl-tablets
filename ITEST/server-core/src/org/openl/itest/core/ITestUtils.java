package org.openl.itest.core;

import java.util.Collections;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public final class ITestUtils {

    private ITestUtils() {
        // Hidden constructor
    }

    public static String getWadlBody(final String url) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new XmlMimeInterceptor()));
        final ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (Objects.equals(HttpStatus.OK, response.getStatusCode())) {
            return response.getBody();
        }
        return null;
    }

    public static String getWsdlBody(final String url) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new XmlMimeInterceptor()));
        final ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (Objects.equals(HttpStatus.OK, response.getStatusCode())) {
            return response.getBody();
        }
        return null;
    }
}
