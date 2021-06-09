package org.openl.rules.webstudio.web.jsf;

class ViewScopeCallback {
    private final String name;

    private final Runnable callback;

    private boolean called = false;

    ViewScopeCallback(String name, Runnable callback) {
        this.name = name;
        this.callback = callback;
    }

    public String getName() {
        return name;
    }

    void run() {
        if (!called) {
            try {
                callback.run();
            } finally {
                called = true;
            }
        }
    }
}