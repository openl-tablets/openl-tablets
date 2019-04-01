package org.openl.rules.webstudio.web.servlet;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

public class SessionCache {
    private Map<String, HttpSession> cache = new ConcurrentHashMap<>();

    public void add(HttpSession session) {
        cache.put(session.getId(), session);
    }

    public void remove(HttpSession session) {
        cache.remove(session.getId());
    }

    public void invalidateAll() {
        for (HttpSession session : new ArrayList<>(cache.values())) {
            session.invalidate();
        }
    }
}
