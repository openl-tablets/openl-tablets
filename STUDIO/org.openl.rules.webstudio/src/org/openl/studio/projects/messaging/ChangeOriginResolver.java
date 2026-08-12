package org.openl.studio.projects.messaging;

import java.util.regex.Pattern;
import jakarta.annotation.Nullable;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Names the client whose request is making the change being published right now.
 *
 * <p>A browser tab puts an id of its own on every request that changes something, in the
 * {@value #ORIGIN_HEADER} header. The id rides the change ping back out, so the tab recognises the
 * echo of its own action and skips re-reading what it has just read.
 *
 * <p>A change made outside a request — the workspace files watcher, a repository poll — has no
 * origin, and neither has a request that named no client. The ping then names no origin at all.
 */
@Component
public class ChangeOriginResolver {

    /** The header a client puts its own id in. */
    public static final String ORIGIN_HEADER = "X-OpenL-Client-Id";

    /**
     * An id is an opaque token the client picks for itself; anything longer or built of other
     * characters is ignored, so nothing unbounded reaches the subscribers of a ping.
     */
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    /**
     * The id of the client whose request runs on this thread.
     *
     * @return the id, or {@code null} when no request is making the change or it named no client
     */
    @Nullable
    public String current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        var origin = attributes.getRequest().getHeader(ORIGIN_HEADER);
        return origin != null && ID.matcher(origin).matches() ? origin : null;
    }
}
