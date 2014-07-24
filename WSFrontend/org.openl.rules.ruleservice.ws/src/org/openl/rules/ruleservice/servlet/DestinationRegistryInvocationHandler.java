package org.openl.rules.ruleservice.servlet;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;

public class DestinationRegistryInvocationHandler implements InvocationHandler {
    private final DestinationRegistry destinationRegistry;
    private final String encoding;

    private final Map<String, AbstractHTTPDestination> decodedDestinations = new ConcurrentHashMap<String, AbstractHTTPDestination>();

    public DestinationRegistryInvocationHandler(DestinationRegistry destinationRegistry, String encoding) {
        this.destinationRegistry = destinationRegistry;
        this.encoding = encoding;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(destinationRegistry, args);

        String methodName = method.getName();
        if (methodName.equals("addDestination") && args.length == 1 && args[0] instanceof AbstractHTTPDestination) {
            addDestination((AbstractHTTPDestination) args[0]);
        } else if (methodName.equals("removeDestination") && args.length == 1 && args[0] instanceof String) {
            removeDestination((String) args[0]);
        } else if (methodName.equals("getDestinationForPath") && args.length == 2 && args[0] instanceof String && args[1] instanceof Boolean) {
            result = getDestinationForPath((AbstractHTTPDestination) result, (String) args[0], (Boolean) args[1]);
        }

        return result;
    }

    private void addDestination(AbstractHTTPDestination destination) {
        String path = getTrimmedPath(destination.getEndpointInfo().getAddress());
        try {
            String decodedPath = URLDecoder.decode(path, encoding);
            if (!path.equals(decodedPath)) {
                decodedDestinations.put(decodedPath, destination);
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported Encoding", e);
        }
    }

    private void removeDestination(String path) {
        try {
            String decodedPath = URLDecoder.decode(path, encoding);
            if (!path.equals(decodedPath)) {
                decodedDestinations.remove(decodedPath);
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported Encoding", e);
        }
    }

    private AbstractHTTPDestination getDestinationForPath(AbstractHTTPDestination withoutDecoding, String path, boolean tryDecoding) {
        if (withoutDecoding != null) {
            return withoutDecoding;
        }

        if (tryDecoding) {
            // to use the url context match
            String trimmedPath = getTrimmedPath(path);
            return decodedDestinations.get(trimmedPath);
        } else {
            return null;
        }
    }

    /**
     * Remove the transport protocol from the path and make
     * it starts with /
     * @return trimmed path
     */
    public String getTrimmedPath(String path) {
        if (path == null) {
            return "/";
        }
        final String lh = "http://localhost/";
        final String lhs = "https://localhost/";

        if (path.startsWith(lh)) {
            path = path.substring(lh.length());
        } else if (path.startsWith(lhs)) {
            path = path.substring(lhs.length());
        }
        if (!path.contains("://") && !path.startsWith("/")) {
            path = "/" + path;

        }
        return path;
    }
}
