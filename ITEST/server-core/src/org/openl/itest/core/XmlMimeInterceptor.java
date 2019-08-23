package org.openl.itest.core;

import java.io.IOException;

import org.springframework.http.MediaType;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class XmlMimeInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().clear();
        HttpHeaders headers = request.getHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_XML_VALUE);
        headers.add("Accept", MediaType.APPLICATION_XML_VALUE);
        return execution.execute(request, body);
    }
}
