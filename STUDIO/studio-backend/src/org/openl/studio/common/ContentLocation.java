package org.openl.studio.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Names the address of what an answer serves, when the request named it by an id it had before.
 *
 * <p>The {@code Content-Location} header of the answer carries the address of the request with the old id replaced
 * by the current one, in the path and in the named query parameters. When the request named several old ids — a
 * project and a table, say — each replacement starts from the address the ones before it gave.
 *
 * <p>An address that names the old id nowhere gets no header: a request that named it in its body is answered without
 * one.
 *
 * @author Yury Molchan
 */
public final class ContentLocation {

    private ContentLocation() {
    }

    /**
     * Names the address of the answer with an old id replaced by the current one.
     *
     * @param stale      the id the request named
     * @param current    the id what the request named has now
     * @param parameters the query parameters that may carry the id
     */
    public static void servedAs(String stale, String current, String... parameters) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || attributes.getResponse() == null) {
            return;
        }
        var response = attributes.getResponse();
        var address = response.getHeader(HttpHeaders.CONTENT_LOCATION);
        if (address == null) {
            var request = attributes.getRequest();
            var query = request.getQueryString();
            address = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
        }
        var replaced = replace(address, stale, current, Set.of(parameters));
        if (!replaced.equals(address)) {
            response.setHeader(HttpHeaders.CONTENT_LOCATION, replaced);
        }
    }

    private static String replace(String address, String stale, String current, Set<String> parameters) {
        var start = address.indexOf('?');
        var path = start < 0 ? address : address.substring(0, start);
        var replaced = Arrays.stream(path.split("/", -1))
                .map(segment -> segment.equals(stale) ? current : segment)
                .collect(Collectors.joining("/"));
        if (start < 0) {
            return replaced;
        }
        return replaced + Arrays.stream(address.substring(start + 1).split("&", -1))
                .map(parameter -> {
                    var name = parameter.substring(0, Math.max(parameter.indexOf('='), 0));
                    return parameters.contains(name) && parameter.equals(name + "=" + stale)
                            ? name + "=" + current
                            : parameter;
                })
                .collect(Collectors.joining("&", "?", ""));
    }
}
