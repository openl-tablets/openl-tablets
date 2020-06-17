package org.openl.rules.webstudio.web.jsf;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.web.context.request.RequestContextHolder;

public class ViewScope implements Scope, HttpSessionBindingListener {
    private static final String VIEW_SCOPE_SESSION_BINDING = "viewScopeSessionBinding";
    private final WeakHashMap<HttpSession, Set<ViewScopeCallback>> sessionToCallbacks = new WeakHashMap<>();

    @Override
    public Object get(String name, ObjectFactory objectFactory) {
        final Map<String, Object> viewMap = FacesContext.getCurrentInstance().getViewRoot().getViewMap();
        Object object = viewMap.get(name);
        if (object != null) {
            return object;
        } else {
            synchronized (RequestContextHolder.currentRequestAttributes().getSessionMutex()) {
                object = viewMap.get(name);
                if (object == null) {
                    object = objectFactory.getObject();
                    viewMap.put(name, object);
                }
                return object;
            }
        }
    }

    @Override
    public String getConversationId() {
        return null;
    }

    @Override
    public Object remove(String name) {
        FacesContext context = FacesContext.getCurrentInstance();

        final Map<String, Object> viewMap = context.getViewRoot().getViewMap();
        if (viewMap.containsKey(name)) {
            Object removed;
            synchronized (RequestContextHolder.currentRequestAttributes().getSessionMutex()) {
                if (viewMap.containsKey(name)) {
                    removed = viewMap.remove(name);
                } else {
                    return null;
                }
            }

            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
            Set<ViewScopeCallback> callbacks = sessionToCallbacks.get(session);
            if (callbacks != null) {
                synchronized (RequestContextHolder.currentRequestAttributes().getSessionMutex()) {
                    callbacks.removeIf(c -> c.getName().equals(name));
                }
            }

            return removed;
        }
        return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        HttpSession session = (HttpSession) externalContext.getSession(true);
        Set<ViewScopeCallback> callbacks = sessionToCallbacks.get(session);
        if (callbacks == null) {
            synchronized (sessionToCallbacks) {
                callbacks = sessionToCallbacks.computeIfAbsent(session, k -> new HashSet<>());
            }
        }

        synchronized (RequestContextHolder.currentRequestAttributes().getSessionMutex()) {
            callbacks.add(new ViewScopeCallback(name, callback));
        }

        Map<String, Object> sessionMap = externalContext.getSessionMap();
        if (!sessionMap.containsKey(VIEW_SCOPE_SESSION_BINDING)) {
            sessionMap.put(VIEW_SCOPE_SESSION_BINDING, this);
        }
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        Set<ViewScopeCallback> callbacks;
        synchronized (sessionToCallbacks) {
            callbacks = sessionToCallbacks.remove(event.getSession());
        }
        if (callbacks != null) {
            for (ViewScopeCallback callback : callbacks) {
                callback.run();
            }
        }
    }

}
